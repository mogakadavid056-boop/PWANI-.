-- CLASSREP V3 - EXAM SECURITY, FLEXIBLE TIMERS (SEC TO MONTHS), COUNTDOWNS, RANKED RESULTS & AUTO-DELETE MARKED PDFs
-- Run once after 25_written_exams_marking.sql and 27_student_profiles_roles_and_marking_order.sql.
-- Safe to run multiple times (idempotent).

-- ============================================================
-- 1. SCHEMA UPGRADES ON QUIZZES, QUESTIONS & ATTEMPTS
-- ============================================================

alter table public.quizzes drop constraint if exists quizzes_time_limit_minutes_check;

alter table public.quizzes add column if not exists time_limit_seconds bigint;
alter table public.quizzes add column if not exists per_question_seconds bigint;
alter table public.quizzes add column if not exists countdown_mode text not null default 'overall';
alter table public.quizzes add column if not exists countdown_style text not null default 'digital';
alter table public.quizzes add column if not exists one_way_navigation boolean not null default false;
alter table public.quizzes add column if not exists lock_on_answer boolean not null default false;
alter table public.quizzes add column if not exists no_copy_paste boolean not null default false;
alter table public.quizzes add column if not exists on_exit_action text not null default 'warn';
alter table public.quizzes add column if not exists max_exit_violations integer not null default 2;
alter table public.quizzes add column if not exists strict_deadline boolean not null default true;

update public.quizzes
set time_limit_seconds = time_limit_minutes::bigint * 60
where time_limit_seconds is null
  and time_limit_minutes is not null
  and time_limit_minutes > 0;

alter table public.quizzes drop constraint if exists quizzes_time_limit_seconds_check;
alter table public.quizzes add constraint quizzes_time_limit_seconds_check
  check (time_limit_seconds is null or time_limit_seconds >= 5);

alter table public.quizzes drop constraint if exists quizzes_per_question_seconds_check;
alter table public.quizzes add constraint quizzes_per_question_seconds_check
  check (per_question_seconds is null or per_question_seconds >= 5);

alter table public.quizzes drop constraint if exists quizzes_countdown_mode_check;
alter table public.quizzes add constraint quizzes_countdown_mode_check
  check (countdown_mode in ('overall', 'per_question', 'both', 'closing_window'));

alter table public.quizzes drop constraint if exists quizzes_countdown_style_check;
alter table public.quizzes add constraint quizzes_countdown_style_check
  check (countdown_style in ('digital', 'bar', 'minimal'));

alter table public.quizzes drop constraint if exists quizzes_on_exit_action_check;
alter table public.quizzes add constraint quizzes_on_exit_action_check
  check (on_exit_action in ('off', 'warn', 'auto_submit'));

alter table public.quizzes drop constraint if exists quizzes_max_exit_violations_check;
alter table public.quizzes add constraint quizzes_max_exit_violations_check
  check (max_exit_violations between 1 and 20);

-- Per-question flexible timing (in any unit converted to seconds)
alter table public.quiz_questions add column if not exists time_limit_seconds bigint;
alter table public.quiz_questions drop constraint if exists quiz_questions_time_limit_seconds_check;
alter table public.quiz_questions add constraint quiz_questions_time_limit_seconds_check
  check (time_limit_seconds is null or time_limit_seconds >= 5);

-- Security, progress locking & download auto-cleanup tracking on quiz_attempts
alter table public.quiz_attempts add column if not exists focus_loss_count integer not null default 0;
alter table public.quiz_attempts add column if not exists security_events jsonb not null default '[]'::jsonb;
alter table public.quiz_attempts add column if not exists auto_submitted_reason text;
alter table public.quiz_attempts add column if not exists locked_question_ords integer[] not null default '{}'::integer[];
alter table public.quiz_attempts add column if not exists current_question_ord integer;
alter table public.quiz_attempts add column if not exists current_question_started_at timestamptz;
alter table public.quiz_attempts add column if not exists pdf_downloaded_at timestamptz;

-- ============================================================
-- 2. STORAGE POLICY: STUDENTS CAN DELETE OWN RELEASED PDF AFTER VERIFIED DOWNLOAD
-- ============================================================
do $$
begin
  if to_regclass('storage.objects') is not null then
    drop policy if exists "students delete own downloaded marked paper" on storage.objects;
    create policy "students delete own downloaded marked paper" on storage.objects
    for delete to authenticated
    using (
      bucket_id = 'marked-papers'
      and split_part(name, '/', 3) = auth.uid()::text || '.pdf'
      and exists (
        select 1 from public.quiz_attempts qa
        where qa.student_id = auth.uid()
          and qa.marking_status = 'released'
          and qa.marked_pdf_path = storage.objects.name
      )
    );
  end if;
exception when insufficient_privilege then
  null;
end $$;

-- ============================================================
-- 3. QUESTION INSERTION WITH OPTIONAL PER-QUESTION SECONDS
-- ============================================================
create or replace function public.insert_quiz_questions(p_quiz uuid, p_questions jsonb, p_start integer)
returns integer
language plpgsql security definer set search_path = public
as $$
declare
  q jsonb;
  opts jsonb;
  n integer;
  i integer := p_start;
  qtype text;
  mk numeric;
  q_secs bigint;
begin
  for q in select * from jsonb_array_elements(p_questions) loop
    i := i + 1;
    qtype := lower(coalesce(nullif(trim(q ->> 'type'), ''), 'mcq'));

    if qtype not in ('mcq', 'written') then
      raise exception 'Question % has an unknown type', i;
    end if;

    if coalesce(trim(q ->> 'question'), '') = '' then
      raise exception 'Question % is empty', i;
    end if;

    if coalesce(q ->> 'marks', '') = '' then
      mk := case when qtype = 'mcq' then 1 else 5 end;
    elsif (q ->> 'marks') !~ '^[0-9]+(\.[0-9]+)?$' then
      raise exception 'Question % has an invalid mark', i;
    else
      mk := (q ->> 'marks')::numeric;
    end if;

    if mk <= 0 or mk > 100 then
      raise exception 'Question % must have between 0.5 and 100 marks', i;
    end if;

    if coalesce(trim(q ->> 'time_limit_seconds'), '') = '' then
      q_secs := null;
    elsif trim(q ->> 'time_limit_seconds') ~ '^[0-9]+$' then
      q_secs := (trim(q ->> 'time_limit_seconds'))::bigint;
      if q_secs < 5 then
        raise exception 'Question % timer must be at least 5 seconds', i;
      end if;
    else
      raise exception 'Question % has an invalid timer value', i;
    end if;

    if qtype = 'mcq' then
      opts := q -> 'options';

      if jsonb_typeof(opts) <> 'array' or jsonb_array_length(opts) < 2 or jsonb_array_length(opts) > 5 then
        raise exception 'Question % needs 2 to 5 options', i;
      end if;

      n := jsonb_array_length(opts);

      if (q ->> 'correct_index') is null
         or (q ->> 'correct_index') !~ '^[0-9]+$'
         or (q ->> 'correct_index')::integer > n - 1 then
        raise exception 'Question % needs a valid correct answer', i;
      end if;

      insert into public.quiz_questions
        (quiz_id, ord, question, question_type, options, correct_index, explanation, marks, model_answer, time_limit_seconds)
      values
        (p_quiz, i, trim(q ->> 'question'), 'mcq', opts, (q ->> 'correct_index')::integer,
         nullif(trim(q ->> 'explanation'), ''), mk, null, q_secs);
    else
      insert into public.quiz_questions
        (quiz_id, ord, question, question_type, options, correct_index, explanation, marks, model_answer, time_limit_seconds)
      values
        (p_quiz, i, trim(q ->> 'question'), 'written', null, null,
         nullif(trim(q ->> 'explanation'), ''), mk, nullif(trim(q ->> 'model_answer'), ''), q_secs);
    end if;
  end loop;

  return i;
end;
$$;

revoke execute on function public.insert_quiz_questions(uuid, jsonb, integer) from public, anon, authenticated;

-- ============================================================
-- 4. CREATE QUIZ / EXAM WITH FLEXIBLE TIMING & SECURITY CONFIG
-- ============================================================
drop function if exists public.create_quiz(uuid, text, text, timestamptz, timestamptz, integer, jsonb, uuid, boolean, boolean, text);
drop function if exists public.create_quiz(uuid, text, text, timestamptz, timestamptz, integer, jsonb, uuid, boolean, boolean, text, bigint, bigint, text, text, boolean, boolean, boolean, text, integer, boolean);

create or replace function public.create_quiz(
  target_class_id uuid,
  quiz_title text,
  quiz_description text,
  quiz_opens_at timestamptz,
  quiz_closes_at timestamptz,
  time_limit integer,
  questions jsonb,
  target_course_id uuid default null,
  as_exam boolean default false,
  shuffle boolean default false,
  answer_format text default 'choices',
  p_time_limit_seconds bigint default null,
  p_per_question_seconds bigint default null,
  p_countdown_mode text default 'overall',
  p_countdown_style text default 'digital',
  p_one_way_navigation boolean default false,
  p_lock_on_answer boolean default false,
  p_no_copy_paste boolean default false,
  p_on_exit_action text default 'warn',
  p_max_exit_violations integer default 2,
  p_strict_deadline boolean default true
)
returns uuid
language plpgsql security definer set search_path = public
as $$
declare
  new_id uuid;
  n_mcq integer;
  n_written integer;
  v_course_ok boolean := false;
  v_total_secs bigint;
  v_legacy_mins integer;
  v_mode text := lower(coalesce(nullif(trim(p_countdown_mode), ''), 'overall'));
  v_style text := lower(coalesce(nullif(trim(p_countdown_style), ''), 'digital'));
  v_exit_action text := lower(coalesce(nullif(trim(p_on_exit_action), ''), 'warn'));
  v_max_exits integer := greatest(1, least(coalesce(p_max_exit_violations, 2), 20));
begin
  if not public.is_class_staff(target_class_id) then
    raise exception 'Only lecturers, prefects and administrators can create challenges';
  end if;

  if as_exam and not (public.is_admin() or public.is_class_lecturer(target_class_id)) then
    raise exception 'Only lecturers and administrators can create exams';
  end if;

  if answer_format not in ('choices', 'written', 'both') then
    raise exception 'The format must be choices, written or both';
  end if;

  if answer_format <> 'choices' and not as_exam then
    raise exception 'Written questions are only allowed in exams';
  end if;

  if v_mode not in ('overall', 'per_question', 'both', 'closing_window') then
    raise exception 'Invalid countdown mode';
  end if;

  if v_style not in ('digital', 'bar', 'minimal') then
    raise exception 'Invalid countdown style';
  end if;

  if v_exit_action not in ('off', 'warn', 'auto_submit') then
    raise exception 'Invalid exit detection action';
  end if;

  if quiz_title is null or char_length(trim(quiz_title)) = 0 then
    raise exception 'Please give it a title';
  end if;

  if quiz_closes_at <= quiz_opens_at or quiz_closes_at <= now() then
    raise exception 'The closing time must be in the future and after the opening time';
  end if;

  v_total_secs := coalesce(
    case when p_time_limit_seconds is not null and p_time_limit_seconds > 0 then p_time_limit_seconds else null end,
    case when time_limit is not null and time_limit > 0 then time_limit::bigint * 60 else null end
  );

  if as_exam and v_total_secs is null and (p_per_question_seconds is null or p_per_question_seconds < 5) then
    raise exception 'An exam needs a time limit or per-question timer';
  end if;

  if v_total_secs is not null and v_total_secs < 5 then
    raise exception 'The exam duration must be at least 5 seconds';
  end if;

  if p_per_question_seconds is not null and p_per_question_seconds < 5 then
    raise exception 'Per-question timer must be at least 5 seconds';
  end if;

  v_legacy_mins := case
    when v_total_secs is null then null
    else greatest(1, ceil(v_total_secs / 60.0)::integer)
  end;

  if jsonb_typeof(questions) <> 'array' or jsonb_array_length(questions) < 1 or jsonb_array_length(questions) > 200 then
    raise exception 'It needs 1 to 200 questions';
  end if;

  if target_course_id is not null then
    select exists (
      select 1 from public.courses c
      where c.id = target_course_id
        and c.class_id = target_class_id
        and lower(coalesce(c.status, 'approved')) = 'approved'
    ) into v_course_ok;

    if not v_course_ok and to_regclass('public.class_courses') is not null then
      execute 'select exists (select 1 from public.class_courses cc where cc.class_id = $1 and cc.course_id = $2 and lower(coalesce(cc.status, ''approved'')) = ''approved'')'
        into v_course_ok using target_class_id, target_course_id;
    end if;

    if not coalesce(v_course_ok, false) then
      raise exception 'Choose an approved unit of this class';
    end if;
  end if;

  insert into public.quizzes (
    class_id, course_id, title, description, opens_at, closes_at,
    time_limit_minutes, time_limit_seconds, per_question_seconds,
    countdown_mode, countdown_style, one_way_navigation, lock_on_answer,
    no_copy_paste, on_exit_action, max_exit_violations, strict_deadline,
    is_exam, shuffle_questions, answer_format, active, created_by
  )
  values (
    target_class_id, target_course_id, trim(quiz_title), nullif(trim(coalesce(quiz_description, '')), ''),
    quiz_opens_at, quiz_closes_at,
    v_legacy_mins, v_total_secs, p_per_question_seconds,
    v_mode, v_style, coalesce(p_one_way_navigation, false), coalesce(p_lock_on_answer, false),
    coalesce(p_no_copy_paste, false), v_exit_action, v_max_exits, coalesce(p_strict_deadline, true),
    as_exam, shuffle, answer_format, true, auth.uid()
  )
  returning id into new_id;

  perform public.insert_quiz_questions(new_id, questions, 0);

  select count(*) filter (where k.question_type = 'mcq'),
         count(*) filter (where k.question_type = 'written')
  into n_mcq, n_written
  from public.quiz_questions k
  where k.quiz_id = new_id;

  if answer_format = 'choices' and n_written > 0 then
    raise exception 'This exam is set to choices only, but it has written questions';
  end if;

  if answer_format = 'written' and n_mcq > 0 then
    raise exception 'This exam is set to written only, but it has choice questions';
  end if;

  insert into public.audit_logs (user_id, action, table_name, record_id, metadata)
  values (
    auth.uid(),
    case when as_exam then 'create_exam' else 'create_challenge' end,
    'quizzes',
    new_id,
    jsonb_build_object(
      'questions', jsonb_array_length(questions),
      'format', answer_format,
      'time_limit_seconds', v_total_secs,
      'per_question_seconds', p_per_question_seconds,
      'countdown_mode', v_mode,
      'one_way', p_one_way_navigation,
      'no_copy_paste', p_no_copy_paste,
      'on_exit_action', v_exit_action
    )
  );

  return new_id;
end;
$$;

grant execute on function public.create_quiz(
  uuid, text, text, timestamptz, timestamptz, integer, jsonb, uuid, boolean, boolean, text,
  bigint, bigint, text, text, boolean, boolean, boolean, text, integer, boolean
) to authenticated;

-- ============================================================
-- 5. LIST QUIZZES WITH TIMER, COUNTDOWN & SECURITY METADATA
-- ============================================================
drop function if exists public.list_quizzes(uuid);

create or replace function public.list_quizzes(target_class_id uuid)
returns table (
  quiz_id uuid,
  title text,
  description text,
  unit_code text,
  opens_at timestamptz,
  closes_at timestamptz,
  time_limit_minutes integer,
  time_limit_seconds bigint,
  per_question_seconds bigint,
  countdown_mode text,
  countdown_style text,
  one_way_navigation boolean,
  lock_on_answer boolean,
  no_copy_paste boolean,
  on_exit_action text,
  max_exit_violations integer,
  strict_deadline boolean,
  question_count integer,
  status text,
  attempted boolean,
  my_score numeric,
  my_total numeric,
  attempts_count bigint,
  is_exam boolean,
  answer_format text,
  total_marks numeric,
  my_marking_status text,
  pdf_downloaded_at timestamptz
)
language plpgsql stable security definer set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  if not public.is_class_member(target_class_id) then
    raise exception 'You are not a member of this class';
  end if;

  return query
  select
    z.id,
    z.title,
    coalesce(z.description, ''),
    coalesce(u.code, ''),
    z.opens_at,
    z.closes_at,
    z.time_limit_minutes,
    coalesce(z.time_limit_seconds, case when z.time_limit_minutes is not null then z.time_limit_minutes::bigint * 60 else null end),
    z.per_question_seconds,
    coalesce(z.countdown_mode, 'overall'),
    coalesce(z.countdown_style, 'digital'),
    coalesce(z.one_way_navigation, false),
    coalesce(z.lock_on_answer, false),
    coalesce(z.no_copy_paste, false),
    coalesce(z.on_exit_action, 'warn'),
    coalesce(z.max_exit_violations, 2),
    coalesce(z.strict_deadline, true),
    (select count(*)::integer from public.quiz_questions k where k.quiz_id = z.id),
    case when now() < z.opens_at then 'upcoming' when now() > z.closes_at then 'closed' else 'open' end,
    (ma.submitted_at is not null),
    case
      when ma.submitted_at is null then null
      when not z.is_exam then ma.score
      when ma.marking_status = 'released' then ma.score
      when ma.marking_status = 'auto' and now() > z.closes_at then ma.score
      else null
    end,
    ma.total,
    case
      when public.is_class_staff(target_class_id) then (select count(*) from public.quiz_attempts a where a.quiz_id = z.id and a.submitted_at is not null)
      else 0
    end,
    z.is_exam,
    z.answer_format,
    coalesce((select sum(k.marks) from public.quiz_questions k where k.quiz_id = z.id), 0),
    ma.marking_status,
    ma.pdf_downloaded_at
  from public.quizzes z
  left join public.courses u on u.id = z.course_id
  left join public.quiz_attempts ma on ma.quiz_id = z.id and ma.student_id = auth.uid()
  where z.class_id = target_class_id and z.active = true
  order by case when now() between z.opens_at and z.closes_at then 0 else 1 end, z.opens_at desc;
end;
$$;

grant execute on function public.list_quizzes(uuid) to authenticated;

-- ============================================================
-- 6. START QUIZ: PER-QUESTION TIMERS, SERVER TIME & SECURITY CONFIG
-- ============================================================
drop function if exists public.start_quiz(uuid);

create or replace function public.start_quiz(target_quiz_id uuid)
returns table (
  question_id uuid,
  ord integer,
  question_type text,
  question text,
  options jsonb,
  marks numeric,
  time_limit_minutes integer,
  time_limit_seconds bigint,
  per_question_seconds bigint,
  question_time_limit_seconds bigint,
  countdown_mode text,
  countdown_style text,
  one_way_navigation boolean,
  lock_on_answer boolean,
  no_copy_paste boolean,
  on_exit_action text,
  max_exit_violations integer,
  strict_deadline boolean,
  started_at timestamptz,
  server_now timestamptz,
  locked_question_ords integer[],
  focus_loss_count integer
)
language plpgsql security definer set search_path = public
as $$
#variable_conflict use_column
declare
  qz record;
  att record;
  my_role text;
  v_limit_secs bigint;
  v_grace interval;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select z.id, z.class_id, z.active, z.opens_at, z.closes_at,
         z.time_limit_minutes, z.time_limit_seconds, z.per_question_seconds,
         z.countdown_mode, z.countdown_style, z.one_way_navigation, z.lock_on_answer,
         z.no_copy_paste, z.on_exit_action, z.max_exit_violations, z.strict_deadline,
         z.shuffle_questions
  into qz
  from public.quizzes z
  where z.id = target_quiz_id;

  if not found then
    raise exception 'Not found';
  end if;

  select nullif(trim(lower(r.role::text)), '') into my_role
  from public.user_roles r where r.user_id = auth.uid() limit 1;

  if coalesce(my_role, 'student') not in ('student', 'class_rep', 'class representative', 'class_prefect', 'prefect', 'rep') then
    raise exception 'Only students can take this';
  end if;

  if not public.is_class_member(qz.class_id) then
    raise exception 'You are not a member of this class';
  end if;

  if not qz.active or now() < qz.opens_at or now() >= qz.closes_at then
    raise exception 'This assessment is not open';
  end if;

  v_limit_secs := coalesce(qz.time_limit_seconds, case when qz.time_limit_minutes is not null then qz.time_limit_minutes::bigint * 60 else null end);
  v_grace := case when coalesce(qz.strict_deadline, true) then interval '5 seconds' else interval '30 seconds' end;

  select a.quiz_id, a.started_at, a.submitted_at, a.locked_question_ords, a.focus_loss_count
  into att
  from public.quiz_attempts a
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid();

  if found then
    if att.submitted_at is not null then
      raise exception 'You have already submitted this assessment';
    end if;

    if v_limit_secs is not null
       and now() > att.started_at + (v_limit_secs || ' seconds')::interval + v_grace then
      raise exception 'Your exam time has expired';
    end if;
  else
    insert into public.quiz_attempts (quiz_id, student_id)
    values (target_quiz_id, auth.uid());

    select a.quiz_id, a.started_at, a.submitted_at, a.locked_question_ords, a.focus_loss_count
    into att
    from public.quiz_attempts a
    where a.quiz_id = target_quiz_id and a.student_id = auth.uid();
  end if;

  return query
  select
    q.id,
    q.ord,
    q.question_type,
    q.question,
    q.options,
    q.marks,
    qz.time_limit_minutes,
    v_limit_secs,
    qz.per_question_seconds,
    coalesce(q.time_limit_seconds, qz.per_question_seconds),
    coalesce(qz.countdown_mode, 'overall'),
    coalesce(qz.countdown_style, 'digital'),
    coalesce(qz.one_way_navigation, false),
    coalesce(qz.lock_on_answer, false),
    coalesce(qz.no_copy_paste, false),
    coalesce(qz.on_exit_action, 'warn'),
    coalesce(qz.max_exit_violations, 2),
    coalesce(qz.strict_deadline, true),
    att.started_at,
    now(),
    coalesce(att.locked_question_ords, '{}'::integer[]),
    coalesce(att.focus_loss_count, 0)
  from public.quiz_questions q
  where q.quiz_id = target_quiz_id
  order by case when qz.shuffle_questions then md5(auth.uid()::text || q.id::text) else null end, q.ord;
end;
$$;

grant execute on function public.start_quiz(uuid) to authenticated;

-- ============================================================
-- 7. SECURITY & PROGRESS RPCS DURING ACTIVE EXAM
-- ============================================================
create or replace function public.record_exam_security_event(
  target_quiz_id uuid,
  event_type text,
  event_detail text default ''
)
returns table (
  focus_loss_count integer,
  max_exit_violations integer,
  should_auto_submit boolean
)
language plpgsql security definer set search_path = public
as $$
declare
  qz record;
  att record;
  v_type text := lower(coalesce(nullif(trim(event_type), ''), 'app_exit'));
  v_is_exit boolean := v_type in ('app_exit', 'focus_lost', 'back_exit', 'split_screen', 'backgrounded');
  v_new_count integer;
  v_event jsonb;
  v_auto boolean;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select z.on_exit_action, coalesce(z.max_exit_violations, 2) as max_exit
  into qz
  from public.quizzes z
  where z.id = target_quiz_id;

  if not found then
    raise exception 'Not found';
  end if;

  select a.focus_loss_count, a.submitted_at
  into att
  from public.quiz_attempts a
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid()
  for update;

  if not found or att.submitted_at is not null then
    return query select coalesce(att.focus_loss_count, 0), qz.max_exit, false;
    return;
  end if;

  v_new_count := coalesce(att.focus_loss_count, 0) + (case when v_is_exit then 1 else 0 end);
  v_event := jsonb_build_object(
    'type', left(v_type, 40),
    'at', now(),
    'detail', left(coalesce(event_detail, ''), 200)
  );

  update public.quiz_attempts
  set focus_loss_count = v_new_count,
      security_events = coalesce(security_events, '[]'::jsonb) || jsonb_build_array(v_event)
  where quiz_id = target_quiz_id and student_id = auth.uid();

  v_auto := (qz.on_exit_action = 'auto_submit')
    or (qz.on_exit_action = 'warn' and v_new_count >= qz.max_exit);

  return query select v_new_count, qz.max_exit, coalesce(v_auto, false);
end;
$$;

grant execute on function public.record_exam_security_event(uuid, text, text) to authenticated;

create or replace function public.sync_exam_progress(
  target_quiz_id uuid,
  chosen_answers integer[],
  written_answers jsonb default '{}'::jsonb,
  locked_ords integer[] default '{}'::integer[],
  current_ord integer default 1
)
returns void
language plpgsql security definer set search_path = public
as $$
declare
  clean jsonb;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select coalesce(jsonb_object_agg(e.key, to_jsonb(left(e.value, 8000))), '{}'::jsonb)
  into clean
  from jsonb_each_text(case when jsonb_typeof(written_answers) = 'object' then written_answers else '{}'::jsonb end) e
  where e.key ~ '^[0-9]+$';

  update public.quiz_attempts a
  set answers = coalesce(chosen_answers, a.answers),
      written_answers = clean,
      locked_question_ords = coalesce(locked_ords, a.locked_question_ords),
      current_question_ord = current_ord,
      current_question_started_at = coalesce(a.current_question_started_at, now())
  where a.quiz_id = target_quiz_id
    and a.student_id = auth.uid()
    and a.submitted_at is null;
end;
$$;

grant execute on function public.sync_exam_progress(uuid, integer[], jsonb, integer[], integer) to authenticated;

-- ============================================================
-- 8. STRICT SUBMIT QUIZ (ENFORCES TIME EXPIRY & RECORDS AUTO-SUBMIT REASON)
-- ============================================================
drop function if exists public.submit_quiz(uuid, integer[], jsonb);
drop function if exists public.submit_quiz(uuid, integer[], jsonb, text);

create or replace function public.submit_quiz(
  target_quiz_id uuid,
  chosen_answers integer[],
  written_answers jsonb default '{}'::jsonb,
  p_auto_submitted_reason text default null
)
returns table (
  score numeric,
  total numeric,
  points_awarded integer,
  needs_marking boolean
)
language plpgsql security definer set search_path = public
as $$
#variable_conflict use_column
declare
  qz record;
  att record;
  total_m numeric;
  got numeric := 0;
  pts integer;
  q record;
  late_flag boolean;
  has_written boolean;
  clean jsonb;
  v_limit_secs bigint;
  v_limit_grace interval;
  v_close_grace interval;
  v_reason text := nullif(left(trim(coalesce(p_auto_submitted_reason, '')), 80), '');
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select z.id, z.class_id, z.closes_at, z.time_limit_minutes, z.time_limit_seconds, z.is_exam, z.strict_deadline
  into qz
  from public.quizzes z
  where z.id = target_quiz_id;

  if not found then
    raise exception 'Not found';
  end if;

  select a.quiz_id, a.started_at, a.submitted_at, a.score, a.total, a.points_awarded, a.marking_status
  into att
  from public.quiz_attempts a
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid()
  for update;

  if not found then
    raise exception 'Start it first';
  end if;

  if att.submitted_at is not null then
    return query select coalesce(att.score, 0), coalesce(att.total, 0), coalesce(att.points_awarded, 0), (att.marking_status in ('pending', 'marking'));
    return;
  end if;

  select coalesce(sum(k.marks), 0), coalesce(bool_or(k.question_type = 'written'), false)
  into total_m, has_written
  from public.quiz_questions k
  where k.quiz_id = target_quiz_id;

  v_limit_secs := coalesce(qz.time_limit_seconds, case when qz.time_limit_minutes is not null then qz.time_limit_minutes::bigint * 60 else null end);
  v_limit_grace := case when coalesce(qz.strict_deadline, true) then interval '15 seconds' else interval '60 seconds' end;
  v_close_grace := case when coalesce(qz.strict_deadline, true) then interval '20 seconds' else interval '5 minutes' end;

  late_flag := (v_limit_secs is not null
                and now() > att.started_at + (v_limit_secs || ' seconds')::interval + v_limit_grace)
               or now() > qz.closes_at + v_close_grace;

  if late_flag and v_reason is null then
    v_reason := 'DEADLINE_EXCEEDED';
  end if;

  -- Strict deadline mode blocks post-expiry MCQ scoring (and prevents unverified late dumps)
  if not late_flag then
    for q in
      select k.ord as n, k.correct_index as correct, k.marks as mk
      from public.quiz_questions k
      where k.quiz_id = target_quiz_id and k.question_type = 'mcq'
      order by k.ord
    loop
      if chosen_answers is not null
         and q.n >= 1
         and q.n <= coalesce(array_length(chosen_answers, 1), 0)
         and chosen_answers[q.n] is not null
         and chosen_answers[q.n] = q.correct then
        got := got + q.mk;
      end if;
    end loop;
  end if;

  select coalesce(jsonb_object_agg(e.key, to_jsonb(left(e.value, 8000))), '{}'::jsonb)
  into clean
  from jsonb_each_text(case when jsonb_typeof(written_answers) = 'object' then written_answers else '{}'::jsonb end) e
  where e.key ~ '^[0-9]+$'
    and exists (select 1 from public.quiz_questions k
                 where k.quiz_id = target_quiz_id and k.ord = e.key::integer and k.question_type = 'written');

  pts := case when late_flag or qz.is_exam then 0 else floor(got)::integer + 5 end;

  update public.quiz_attempts a
  set submitted_at = now(),
      score = got,
      total = total_m,
      mcq_marks = got,
      answers = chosen_answers,
      written_answers = clean,
      late = late_flag,
      auto_submitted_reason = v_reason,
      points_awarded = pts,
      marking_status = case when has_written then 'pending' else 'auto' end
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid();

  perform public.award_points(auth.uid(), qz.class_id, pts, 'quiz', target_quiz_id);

  return query select got, total_m, pts, has_written;
end;
$$;

grant execute on function public.submit_quiz(uuid, integer[], jsonb, text) to authenticated;

-- ============================================================
-- 9. CONFIRM MARKED PDF DOWNLOADED (CLEARS DB PATH AFTER STORAGE DELETE)
-- ============================================================
create or replace function public.confirm_marked_paper_downloaded(target_quiz_id uuid)
returns boolean
language plpgsql security definer set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  update public.quiz_attempts qa
  set pdf_downloaded_at = coalesce(qa.pdf_downloaded_at, now()),
      marked_pdf_path = null
  where qa.quiz_id = target_quiz_id
    and qa.student_id = auth.uid()
    and qa.marking_status = 'released';

  if not found then
    return false;
  end if;

  insert into public.audit_logs (user_id, action, table_name, record_id, metadata)
  values (auth.uid(), 'confirm_marked_pdf_downloaded', 'quiz_attempts', target_quiz_id,
          jsonb_build_object('deleted_remote_pdf', true));

  return true;
end;
$$;

grant execute on function public.confirm_marked_paper_downloaded(uuid) to authenticated;

-- ============================================================
-- 10. STUDENT LOCAL RE-GENERATION FALLBACK IF REMOTE PDF ALREADY DELETED
-- ============================================================
create or replace function public.get_my_marked_paper_detail(target_quiz_id uuid)
returns jsonb
language plpgsql stable security definer set search_path = public, auth
as $$
declare
  qz record;
  att record;
  stu record;
  qs jsonb;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select z.id, z.title, z.class_id, z.closes_at, z.answer_format, c.name as class_name,
         coalesce(u.code, '') as unit_code, coalesce(u.name, '') as unit_name
  into qz
  from public.quizzes z
  join public.classes c on c.id = z.class_id
  left join public.courses u on u.id = z.course_id
  where z.id = target_quiz_id;

  if not found then
    raise exception 'Not found';
  end if;

  select qa.*
  into att
  from public.quiz_attempts qa
  where qa.quiz_id = target_quiz_id
    and qa.student_id = auth.uid()
    and qa.submitted_at is not null
    and qa.marking_status = 'released';

  if not found then
    raise exception 'Your marked paper has not been returned yet';
  end if;

  select
    coalesce(nullif(trim(p.full_name), ''), nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''), 'Student') as full_name,
    coalesce(nullif(trim(p.admission_number), ''), nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''), '-') as admission_number
  into stu
  from auth.users au
  left join public.profiles p on p.id = au.id
  where au.id = auth.uid();

  select coalesce(jsonb_agg(jsonb_build_object(
    'ord', k.ord,
    'type', k.question_type,
    'question', k.question,
    'options', k.options,
    'marks', k.marks,
    'correct_index', k.correct_index,
    'chosen_index', case
      when att.answers is not null and k.ord >= 1 and k.ord <= coalesce(array_length(att.answers, 1), 0)
        then (att.answers)[k.ord]
      else -1
    end,
    'written_answer', coalesce(att.written_answers ->> k.ord::text, ''),
    'model_answer', '',
    'awarded_marks', case
      when k.question_type = 'mcq' then
        case when att.answers is not null and k.ord >= 1 and k.ord <= coalesce(array_length(att.answers, 1), 0)
                  and (att.answers)[k.ord] = k.correct_index
             then k.marks else 0 end
      when (att.question_marks -> k.ord::text ->> 'marks') is not null
        then (att.question_marks -> k.ord::text ->> 'marks')::numeric
      else null
    end,
    'comment', coalesce(att.question_marks -> k.ord::text ->> 'comment', '')
  ) order by k.ord), '[]'::jsonb)
  into qs
  from public.quiz_questions k
  where k.quiz_id = target_quiz_id;

  return jsonb_build_object(
    'quiz', jsonb_build_object(
      'id', qz.id,
      'title', qz.title,
      'class_id', qz.class_id,
      'class_name', qz.class_name,
      'unit_code', qz.unit_code,
      'unit_name', qz.unit_name,
      'closes_at', qz.closes_at,
      'answer_format', qz.answer_format,
      'total_marks', coalesce(att.total, 0)
    ),
    'student', jsonb_build_object(
      'id', auth.uid(),
      'full_name', coalesce(stu.full_name, 'Student'),
      'admission_number', coalesce(stu.admission_number, '-')
    ),
    'attempt', jsonb_build_object(
      'status', att.marking_status,
      'submitted_at', att.submitted_at,
      'late', att.late,
      'mcq_marks', att.mcq_marks,
      'written_marks', att.written_marks,
      'draft_version', att.draft_version,
      'overall_comment', coalesce(att.overall_comment, ''),
      'marked_pdf_path', coalesce(att.marked_pdf_path, '')
    ),
    'questions', qs,
    'draft', coalesce(att.marking_draft, '{"questions":{}}'::jsonb)
  );
end;
$$;

grant execute on function public.get_my_marked_paper_detail(uuid) to authenticated;

drop function if exists public.get_my_marked_paper(uuid);

create or replace function public.get_my_marked_paper(target_quiz_id uuid)
returns table (
  marking_status text,
  score numeric,
  total numeric,
  marked_pdf_path text,
  released_at timestamptz,
  overall_comment text,
  pdf_downloaded_at timestamptz
)
language plpgsql stable security definer set search_path = public
as $$
#variable_conflict use_column
declare
  closes timestamptz;
begin
  select z.closes_at into closes from public.quizzes z where z.id = target_quiz_id;

  if closes is null then
    raise exception 'Not found';
  end if;

  return query
  select a.marking_status,
         case when a.marking_status = 'released'
                or (a.marking_status = 'auto' and now() > closes) then a.score else null end,
         a.total,
         case when a.marking_status = 'released' then a.marked_pdf_path else null end,
         a.released_at,
         case when a.marking_status = 'released' then a.overall_comment else null end,
         a.pdf_downloaded_at
  from public.quiz_attempts a
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid() and a.submitted_at is not null;
end;
$$;

grant execute on function public.get_my_marked_paper(uuid) to authenticated;

-- ============================================================
-- 11. RESULTS & SUBMISSIONS WITH INTEGRITY METRICS (FOR RANKED PDF & BATCH RETURN)
-- ============================================================
drop function if exists public.quiz_results(uuid);

create or replace function public.quiz_results(target_quiz_id uuid)
returns table (
  student_id uuid,
  full_name text,
  admission_number text,
  score numeric,
  total numeric,
  submitted_at timestamptz,
  marking_status text,
  late boolean,
  mcq_marks numeric,
  written_marks numeric,
  focus_loss_count integer,
  auto_submitted_reason text
)
language plpgsql stable security definer set search_path = public, auth
as $$
declare
  cid uuid;
begin
  select z.class_id into cid from public.quizzes z where z.id = target_quiz_id;

  if cid is null then
    raise exception 'Not found';
  end if;

  if not public.is_class_staff(cid) then
    raise exception 'Not authorized';
  end if;

  return query
  select
    a.student_id,
    coalesce(nullif(trim(p.full_name), ''), nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''), 'Student'),
    coalesce(nullif(trim(p.admission_number), ''), nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''), '-'),
    case when a.marking_status in ('auto', 'released') then a.score else null end,
    a.total,
    a.submitted_at,
    a.marking_status,
    a.late,
    a.mcq_marks,
    a.written_marks,
    coalesce(a.focus_loss_count, 0),
    coalesce(a.auto_submitted_reason, '')
  from public.quiz_attempts a
  left join public.profiles p on p.id = a.student_id
  left join auth.users au on au.id = a.student_id
  where a.quiz_id = target_quiz_id and a.submitted_at is not null
  order by a.score desc nulls last, a.submitted_at asc;
end;
$$;

grant execute on function public.quiz_results(uuid) to authenticated;

drop function if exists public.list_exam_submissions(uuid);

create or replace function public.list_exam_submissions(target_quiz_id uuid)
returns table (
  student_id uuid,
  full_name text,
  admission_number text,
  submitted_at timestamptz,
  late boolean,
  mcq_marks numeric,
  written_marks numeric,
  final_score numeric,
  max_total numeric,
  marking_status text,
  focus_loss_count integer,
  auto_submitted_reason text,
  written_ready_count integer,
  written_total_count integer
)
language plpgsql stable security definer set search_path = public, auth
as $$
#variable_conflict use_column
declare
  mx numeric;
  v_written_total integer;
begin
  if not public.can_mark_quiz(target_quiz_id) then
    raise exception 'Not authorized';
  end if;

  select coalesce(sum(k.marks), 0), count(*) filter (where k.question_type = 'written')::integer
  into mx, v_written_total
  from public.quiz_questions k
  where k.quiz_id = target_quiz_id;

  return query
  select
    a.student_id,
    coalesce(
      nullif(trim(p.full_name), ''),
      nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''),
      nullif(split_part(coalesce(au.email, ''), '@', 1), ''),
      'Student'
    ),
    coalesce(
      nullif(trim(p.admission_number), ''),
      nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''),
      '-'
    ),
    a.submitted_at,
    a.late,
    a.mcq_marks,
    a.written_marks,
    case when a.marking_status in ('auto', 'released') then a.score else null end,
    mx,
    a.marking_status,
    coalesce(a.focus_loss_count, 0),
    coalesce(a.auto_submitted_reason, ''),
    coalesce((
      select count(*)::integer
      from public.quiz_questions k
      where k.quiz_id = target_quiz_id
        and k.question_type = 'written'
        and (a.question_marks -> k.ord::text ->> 'marks') is not null
    ), 0),
    coalesce(v_written_total, 0)
  from public.quiz_attempts a
  left join public.profiles p on p.id = a.student_id
  left join auth.users au on au.id = a.student_id
  where a.quiz_id = target_quiz_id and a.submitted_at is not null
  order by
    case a.marking_status when 'pending' then 0 when 'marking' then 1 else 2 end,
    lower(trim(coalesce(nullif(trim(p.full_name), ''), nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''), ''))),
    lower(trim(coalesce(nullif(trim(p.admission_number), ''), nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''), '')));
end;
$$;

grant execute on function public.list_exam_submissions(uuid) to authenticated;

notify pgrst, 'reload schema';

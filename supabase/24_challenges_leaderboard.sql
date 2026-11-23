-- CLASSREP V2 - CHALLENGES (quizzes), POINTS, STREAKS AND LEADERBOARD
-- Run once, after 22_attendance_tracker.sql (needs 21_notifications.sql for the "new quiz" alert).
--
-- Staff (lecturers, prefects, admins) create short quizzes called Challenges.
-- Students play them. The correct answers never leave the database until the student has submitted,
-- and grading happens on the server, so nobody can cheat by editing the app.
-- Points: +10 attendance, +2 poll vote, quiz = 1 point per correct answer + 5 for finishing.
-- Streak = consecutive days with any points (Nairobi time).
-- Leaderboard: weekly, top 10 plus the student's own rank. Students can hide their name.

-- 1. Tables
alter table public.profiles add column if not exists show_on_leaderboard boolean not null default true;

create table if not exists public.points_ledger (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  class_id uuid references public.classes(id) on delete set null,
  points integer not null check (points > 0),
  reason text not null,
  ref_id uuid,
  created_at timestamptz not null default now()
);

create unique index if not exists idx_points_unique
  on public.points_ledger(user_id, reason, ref_id) where ref_id is not null;
create index if not exists idx_points_user_time
  on public.points_ledger(user_id, created_at);

create table if not exists public.quizzes (
  id uuid primary key default gen_random_uuid(),
  class_id uuid not null references public.classes(id) on delete cascade,
  course_id uuid references public.courses(id) on delete set null,
  title text not null check (char_length(trim(title)) > 0),
  description text,
  opens_at timestamptz not null,
  closes_at timestamptz not null,
  time_limit_minutes integer check (time_limit_minutes between 1 and 180),
  active boolean not null default true,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  check (closes_at > opens_at)
);

create index if not exists idx_quizzes_class on public.quizzes(class_id, closes_at desc);

create table if not exists public.quiz_questions (
  id uuid primary key default gen_random_uuid(),
  quiz_id uuid not null references public.quizzes(id) on delete cascade,
  ord integer not null,
  question text not null,
  options jsonb not null,
  correct_index integer not null,
  explanation text,
  unique (quiz_id, ord)
);

create table if not exists public.quiz_attempts (
  id uuid primary key default gen_random_uuid(),
  quiz_id uuid not null references public.quizzes(id) on delete cascade,
  student_id uuid not null references auth.users(id) on delete cascade,
  started_at timestamptz not null default now(),
  submitted_at timestamptz,
  answers integer[],
  score integer,
  total integer,
  points_awarded integer not null default 0,
  unique (quiz_id, student_id)
);

create index if not exists idx_quiz_attempts_student on public.quiz_attempts(student_id, submitted_at desc);

-- 2. Row Level Security
alter table public.points_ledger enable row level security;
alter table public.quizzes enable row level security;
alter table public.quiz_questions enable row level security;
alter table public.quiz_attempts enable row level security;

drop policy if exists "Users read own points" on public.points_ledger;
create policy "Users read own points"
  on public.points_ledger for select to authenticated
  using (user_id = auth.uid() or public.is_admin());

drop policy if exists "Class members view quizzes" on public.quizzes;
create policy "Class members view quizzes"
  on public.quizzes for select to authenticated
  using (
    public.is_admin()
    or public.is_class_staff(class_id)
    or exists (
      select 1 from public.class_members cm
      where cm.class_id = quizzes.class_id
        and cm.user_id = auth.uid()
        and coalesce(cm.status::text, 'active') = 'active'
    )
  );

drop policy if exists "Staff view quiz questions" on public.quiz_questions;
create policy "Staff view quiz questions"
  on public.quiz_questions for select to authenticated
  using (
    exists (
      select 1 from public.quizzes q
      where q.id = quiz_questions.quiz_id
        and (public.is_admin() or public.is_class_staff(q.class_id))
    )
  );

drop policy if exists "Students read own attempts and staff read class attempts" on public.quiz_attempts;
create policy "Students read own attempts and staff read class attempts"
  on public.quiz_attempts for select to authenticated
  using (
    student_id = auth.uid()
    or exists (
      select 1 from public.quizzes q
      where q.id = quiz_attempts.quiz_id
        and (public.is_admin() or public.is_class_staff(q.class_id))
    )
  );

-- 3. Automatic Points Triggers (Attendance +10, Poll Vote +2) and Quiz Notification Trigger
create or replace function public.trg_award_attendance_points()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid;
  v_class_id uuid;
  v_json jsonb := to_jsonb(new);
begin
  if coalesce((v_json->>'excused')::boolean, false) then
    return new;
  end if;
  v_uid := coalesce(
    nullif(v_json->>'student_id', '')::uuid,
    nullif(v_json->>'user_id', '')::uuid
  );
  if v_uid is null then
    return new;
  end if;
  select class_id into v_class_id
  from public.attendance_sessions
  where id = (v_json->>'session_id')::uuid;

  insert into public.points_ledger (user_id, class_id, points, reason, ref_id)
  values (v_uid, v_class_id, 10, 'attendance', new.session_id)
  on conflict (user_id, reason, ref_id) where ref_id is not null do nothing;

  return new;
end;
$$;

create or replace function public.trg_award_poll_points()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid;
  v_class_id uuid;
  v_json jsonb := to_jsonb(new);
begin
  v_uid := coalesce(
    nullif(v_json->>'user_id', '')::uuid,
    nullif(v_json->>'student_id', '')::uuid,
    auth.uid()
  );
  if v_uid is null then
    return new;
  end if;
  select class_id into v_class_id
  from public.polls
  where id = new.poll_id;

  insert into public.points_ledger (user_id, class_id, points, reason, ref_id)
  values (v_uid, v_class_id, 2, 'poll_vote', new.poll_id)
  on conflict (user_id, reason, ref_id) where ref_id is not null do nothing;

  return new;
end;
$$;

create or replace function public.trg_notify_quiz_created()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if to_regprocedure('public._notify_class_members(uuid,uuid,text,text,text,jsonb)') is not null then
    perform public._notify_class_members(
      new.class_id,
      new.created_by,
      'quiz',
      'New Challenge: ' || coalesce(new.title, 'Quiz'),
      coalesce(nullif(trim(new.description), ''), 'Tap to open Challenges and test your knowledge.'),
      jsonb_build_object('quiz_id', new.id, 'class_id', new.class_id)
    );
  end if;
  return new;
end;
$$;

do $$
begin
  if to_regclass('public.attendance_records') is not null then
    drop trigger if exists trg_classrep_points_attendance on public.attendance_records;
    create trigger trg_classrep_points_attendance
      after insert on public.attendance_records
      for each row execute function public.trg_award_attendance_points();
  end if;
  if to_regclass('public.poll_votes') is not null then
    drop trigger if exists trg_classrep_points_poll on public.poll_votes;
    create trigger trg_classrep_points_poll
      after insert on public.poll_votes
      for each row execute function public.trg_award_poll_points();
  end if;
  drop trigger if exists trg_classrep_notify_quiz on public.quizzes;
  create trigger trg_classrep_notify_quiz
    after insert on public.quizzes
    for each row execute function public.trg_notify_quiz_created();
end $$;

-- 4. RPCs for Challenges
create or replace function public.create_quiz(
  target_class_id uuid,
  quiz_title text,
  quiz_description text,
  quiz_opens_at timestamptz,
  quiz_closes_at timestamptz,
  time_limit integer,
  questions jsonb,
  target_course_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_quiz_id uuid;
  v_idx integer := 0;
  v_q jsonb;
  v_opts jsonb;
  v_correct integer;
  v_opt_count integer;
begin
  if not public.is_class_staff(target_class_id) then
    raise exception 'Only class staff can create challenges.';
  end if;
  if char_length(trim(coalesce(quiz_title, ''))) = 0 then
    raise exception 'Quiz title is required.';
  end if;
  if quiz_closes_at <= quiz_opens_at then
    raise exception 'Closing time must be after opening time.';
  end if;
  if time_limit is not null and (time_limit < 1 or time_limit > 180) then
    raise exception 'Time limit must be between 1 and 180 minutes.';
  end if;
  if jsonb_typeof(questions) <> 'array' or jsonb_array_length(questions) < 1 or jsonb_array_length(questions) > 30 then
    raise exception 'Challenges must contain between 1 and 30 questions.';
  end if;
  if target_course_id is not null and not exists (
    select 1 from public.courses c
    where c.id = target_course_id and c.class_id = target_class_id
      and lower(coalesce(c.status, 'approved')) = 'approved'
  ) then
    raise exception 'Selected unit is not approved for this class.';
  end if;

  insert into public.quizzes (class_id, course_id, title, description, opens_at, closes_at, time_limit_minutes, active, created_by)
  values (
    target_class_id,
    target_course_id,
    trim(quiz_title),
    nullif(trim(coalesce(quiz_description, '')), ''),
    quiz_opens_at,
    quiz_closes_at,
    time_limit,
    true,
    auth.uid()
  )
  returning id into v_quiz_id;

  for v_q in select value from jsonb_array_elements(questions)
  loop
    v_idx := v_idx + 1;
    v_opts := v_q->'options';
    if jsonb_typeof(v_opts) <> 'array' then
      raise exception 'Question % options must be a list.', v_idx;
    end if;
    v_opt_count := jsonb_array_length(v_opts);
    if v_opt_count < 2 or v_opt_count > 5 then
      raise exception 'Question % must have between 2 and 5 options.', v_idx;
    end if;
    v_correct := (v_q->>'correct_index')::integer;
    if v_correct < 0 or v_correct >= v_opt_count then
      raise exception 'Question % has invalid correct_index.', v_idx;
    end if;
    if char_length(trim(coalesce(v_q->>'question', ''))) = 0 then
      raise exception 'Question % text cannot be empty.', v_idx;
    end if;

    insert into public.quiz_questions (quiz_id, ord, question, options, correct_index, explanation)
    values (
      v_quiz_id,
      v_idx,
      trim(v_q->>'question'),
      v_opts,
      v_correct,
      nullif(trim(coalesce(v_q->>'explanation', '')), '')
    );
  end loop;

  return v_quiz_id;
end;
$$;

grant execute on function public.create_quiz(uuid, text, text, timestamptz, timestamptz, integer, jsonb, uuid) to authenticated;

create or replace function public.list_quizzes(target_class_id uuid)
returns table (
  quiz_id uuid,
  title text,
  description text,
  unit_code text,
  opens_at timestamptz,
  closes_at timestamptz,
  time_limit_minutes integer,
  question_count bigint,
  status text,
  attempted boolean,
  my_score integer,
  my_total integer,
  attempts_count bigint
)
language plpgsql
security definer
set search_path = public
as $$
begin
  if not (
    public.is_admin()
    or public.is_class_staff(target_class_id)
    or exists (
      select 1 from public.class_members cm
      where cm.class_id = target_class_id
        and cm.user_id = auth.uid()
        and coalesce(cm.status::text, 'active') = 'active'
    )
  ) then
    raise exception 'Not a member of this class.';
  end if;

  return query
  select
    q.id as quiz_id,
    q.title,
    coalesce(q.description, '') as description,
    coalesce(cu.code, '') as unit_code,
    q.opens_at,
    q.closes_at,
    q.time_limit_minutes,
    (select count(*) from public.quiz_questions qq where qq.quiz_id = q.id) as question_count,
    case
      when not q.active or now() >= q.closes_at then 'closed'
      when now() < q.opens_at then 'upcoming'
      else 'open'
    end::text as status,
    (mya.submitted_at is not null) as attempted,
    mya.score as my_score,
    mya.total as my_total,
    (select count(*) from public.quiz_attempts qa where qa.quiz_id = q.id and qa.submitted_at is not null) as attempts_count
  from public.quizzes q
  left join public.courses cu on cu.id = q.course_id
  left join public.quiz_attempts mya on mya.quiz_id = q.id and mya.student_id = auth.uid()
  where q.class_id = target_class_id
  order by
    case
      when q.active and now() >= q.opens_at and now() < q.closes_at then 1
      when q.active and now() < q.opens_at then 2
      else 3
    end,
    q.closes_at desc;
end;
$$;

grant execute on function public.list_quizzes(uuid) to authenticated;

create or replace function public.start_quiz(target_quiz_id uuid)
returns table (
  question_id uuid,
  ord integer,
  question text,
  options jsonb,
  time_limit_minutes integer,
  started_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_quiz record;
  v_attempt record;
  v_role text;
begin
  select nullif(trim(lower(ur.role::text)), '') into v_role
  from public.user_roles ur where ur.user_id = auth.uid() limit 1;
  if coalesce(v_role, 'student') not in ('student', 'class_rep', 'class representative', 'class_prefect', 'prefect', 'rep') then
    raise exception 'Only students can play challenges.';
  end if;

  select * into v_quiz from public.quizzes where id = target_quiz_id;
  if v_quiz.id is null then
    raise exception 'Challenge not found.';
  end if;
  if not exists (
    select 1 from public.class_members cm
    where cm.class_id = v_quiz.class_id
      and cm.user_id = auth.uid()
      and coalesce(cm.status::text, 'active') = 'active'
  ) then
    raise exception 'Join this class to play its challenges.';
  end if;
  if not v_quiz.active or now() < v_quiz.opens_at or now() >= v_quiz.closes_at then
    raise exception 'This challenge is not currently open.';
  end if;

  select * into v_attempt
  from public.quiz_attempts qa
  where qa.quiz_id = target_quiz_id and qa.student_id = auth.uid();

  if v_attempt.id is null then
    insert into public.quiz_attempts (quiz_id, student_id, started_at)
    values (target_quiz_id, auth.uid(), now())
    returning * into v_attempt;
  elsif v_attempt.submitted_at is not null then
    raise exception 'You have already submitted this challenge.';
  elsif v_quiz.time_limit_minutes is not null
    and now() > v_attempt.started_at + make_interval(mins => v_quiz.time_limit_minutes) + interval '30 seconds' then
    raise exception 'Your time limit for this challenge has expired.';
  end if;

  return query
  select
    qq.id as question_id,
    qq.ord,
    qq.question,
    qq.options,
    v_quiz.time_limit_minutes,
    v_attempt.started_at
  from public.quiz_questions qq
  where qq.quiz_id = target_quiz_id
  order by qq.ord asc;
end;
$$;

grant execute on function public.start_quiz(uuid) to authenticated;

create or replace function public.submit_quiz(
  target_quiz_id uuid,
  chosen_answers integer[]
)
returns table (
  score integer,
  total integer,
  points_awarded integer
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_quiz record;
  v_attempt record;
  v_score integer := 0;
  v_total integer := 0;
  v_pts integer := 0;
  v_on_time boolean := true;
  v_q record;
begin
  select * into v_quiz from public.quizzes where id = target_quiz_id;
  if v_quiz.id is null then
    raise exception 'Challenge not found.';
  end if;

  select * into v_attempt
  from public.quiz_attempts qa
  where qa.quiz_id = target_quiz_id and qa.student_id = auth.uid();

  if v_attempt.id is null then
    raise exception 'Start this challenge first.';
  end if;
  if v_attempt.submitted_at is not null then
    return query select v_attempt.score, v_attempt.total, v_attempt.points_awarded;
    return;
  end if;

  if now() > v_quiz.closes_at + interval '30 seconds' then
    v_on_time := false;
  end if;
  if v_quiz.time_limit_minutes is not null
    and now() > v_attempt.started_at + make_interval(mins => v_quiz.time_limit_minutes) + interval '30 seconds' then
    v_on_time := false;
  end if;

  for v_q in
    select qq.ord as n, qq.correct_index
    from public.quiz_questions qq
    where qq.quiz_id = target_quiz_id
    order by qq.ord asc
  loop
    v_total := v_total + 1;
    if chosen_answers is not null
      and v_q.n >= 1
      and v_q.n <= coalesce(array_length(chosen_answers, 1), 0)
      and chosen_answers[v_q.n] = v_q.correct_index then
      v_score := v_score + 1;
    end if;
  end loop;

  v_pts := case when v_on_time then (v_score + 5) else 0 end;

  update public.quiz_attempts
  set submitted_at = now(),
      answers = chosen_answers,
      score = v_score,
      total = v_total,
      points_awarded = v_pts
  where id = v_attempt.id;

  if v_pts > 0 then
    insert into public.points_ledger (user_id, class_id, points, reason, ref_id)
    values (auth.uid(), v_quiz.class_id, v_pts, 'quiz_finish', target_quiz_id)
    on conflict (user_id, reason, ref_id) where ref_id is not null do nothing;
  end if;

  return query select v_score, v_total, v_pts;
end;
$$;

grant execute on function public.submit_quiz(uuid, integer[]) to authenticated;

create or replace function public.get_quiz_review(target_quiz_id uuid)
returns table (
  ord integer,
  question text,
  options jsonb,
  correct_index integer,
  chosen_index integer,
  explanation text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_quiz record;
  v_mine integer[];
  v_submitted boolean := false;
begin
  select * into v_quiz from public.quizzes where id = target_quiz_id;
  if v_quiz.id is null then
    raise exception 'Challenge not found.';
  end if;

  select qa.answers, (qa.submitted_at is not null)
  into v_mine, v_submitted
  from public.quiz_attempts qa
  where qa.quiz_id = target_quiz_id and qa.student_id = auth.uid();

  if not (public.is_admin() or public.is_class_staff(v_quiz.class_id) or coalesce(v_submitted, false)) then
    raise exception 'Submit the challenge first to view answers and explanations.';
  end if;

  return query
  select
    k.ord,
    k.question,
    k.options,
    k.correct_index,
    case
      when v_mine is not null and k.ord >= 1 and k.ord <= coalesce(array_length(v_mine, 1), 0)
        then v_mine[k.ord]
      else -1
    end as chosen_index,
    coalesce(k.explanation, '') as explanation
  from public.quiz_questions k
  where k.quiz_id = target_quiz_id
  order by k.ord asc;
end;
$$;

grant execute on function public.get_quiz_review(uuid) to authenticated;

create or replace function public.quiz_results(target_quiz_id uuid)
returns table (
  student_id uuid,
  full_name text,
  admission_number text,
  score integer,
  total integer,
  submitted_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_class_id uuid;
begin
  select class_id into v_class_id from public.quizzes where id = target_quiz_id;
  if v_class_id is null then
    raise exception 'Challenge not found.';
  end if;
  if not (public.is_admin() or public.is_class_staff(v_class_id)) then
    raise exception 'Only class staff can view challenge results.';
  end if;

  return query
  select
    qa.student_id,
    coalesce(nullif(trim(p.full_name), ''), 'Student')::text as full_name,
    coalesce(nullif(trim(p.admission_number), ''), '-')::text as admission_number,
    coalesce(qa.score, 0) as score,
    coalesce(qa.total, 0) as total,
    qa.submitted_at
  from public.quiz_attempts qa
  join public.profiles p on p.id = qa.student_id
  where qa.quiz_id = target_quiz_id
    and qa.submitted_at is not null
  order by qa.score desc, qa.submitted_at asc;
end;
$$;

grant execute on function public.quiz_results(uuid) to authenticated;

create or replace function public.delete_quiz(target_quiz_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_quiz record;
begin
  select * into v_quiz from public.quizzes where id = target_quiz_id;
  if v_quiz.id is null then
    return;
  end if;
  if not (public.is_admin() or public.is_class_lecturer(v_quiz.class_id) or v_quiz.created_by = auth.uid()) then
    raise exception 'Only the challenge creator, class lecturer, or administrator can delete this challenge.';
  end if;
  delete from public.quizzes where id = target_quiz_id;
end;
$$;

grant execute on function public.delete_quiz(uuid) to authenticated;

-- 5. RPCs for Points, Streaks & Weekly Leaderboard
create or replace function public.my_activity_stats(target_class_id uuid)
returns table (
  total_points integer,
  week_points integer,
  streak_days integer,
  best_streak integer,
  week_rank integer,
  ranked_students integer
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_today date := (now() at time zone 'Africa/Nairobi')::date;
  v_week_start timestamptz := date_trunc('week', now() at time zone 'Africa/Nairobi') at time zone 'Africa/Nairobi';
  v_total integer := 0;
  v_week integer := 0;
  v_cur_streak integer := 0;
  v_best_streak integer := 0;
  v_rank integer := null;
  v_ranked integer := 0;
  v_role text;
begin
  select lower(coalesce(ur.role::text, 'student')) into v_role
  from public.user_roles ur where ur.user_id = auth.uid();

  select
    coalesce(sum(pl.points), 0)::integer,
    coalesce(sum(pl.points) filter (where pl.created_at >= v_week_start), 0)::integer
  into v_total, v_week
  from public.points_ledger pl
  where pl.user_id = auth.uid()
    and (target_class_id is null or pl.class_id = target_class_id or pl.class_id is null);

  with active_days as (
    select distinct (pl.created_at at time zone 'Africa/Nairobi')::date as d
    from public.points_ledger pl
    where pl.user_id = auth.uid()
      and (target_class_id is null or pl.class_id = target_class_id or pl.class_id is null)
  ),
  islands as (
    select d, d - (row_number() over (order by d))::integer as grp
    from active_days
  ),
  streaks as (
    select grp, min(d) as start_d, max(d) as end_d, count(*)::integer as len
    from islands
    group by grp
  )
  select
    coalesce((select max(len) from streaks), 0),
    coalesce((
      select len from streaks
      where end_d >= v_today - 1
      order by end_d desc
      limit 1
    ), 0)
  into v_best_streak, v_cur_streak;

  if target_class_id is not null then
    with scores as (
      select
        cm.user_id as uid,
        coalesce(sum(pl.points) filter (where pl.created_at >= v_week_start and (pl.class_id = target_class_id or pl.class_id is null)), 0)::integer as pts
      from public.class_members cm
      left join public.user_roles ur on ur.user_id = cm.user_id
      left join public.points_ledger pl on pl.user_id = cm.user_id
      where cm.class_id = target_class_id
        and coalesce(cm.status::text, 'active') = 'active'
        and lower(coalesce(ur.role::text, 'student')) = 'student'
      group by cm.user_id
    ),
    ranked as (
      select uid, pts, dense_rank() over (order by pts desc) as rk
      from scores
    )
    select
      count(*)::integer,
      (select rk::integer from ranked where uid = auth.uid())
    into v_ranked, v_rank
    from ranked;
  end if;

  if coalesce(v_role, 'student') <> 'student' then
    v_rank := null;
  end if;

  return query select v_total, v_week, v_cur_streak, v_best_streak, v_rank, v_ranked;
end;
$$;

grant execute on function public.my_activity_stats(uuid) to authenticated;

create or replace function public.class_leaderboard(
  target_class_id uuid,
  period text default 'week',
  max_rows integer default 10
)
returns table (
  rank_no integer,
  display_name text,
  points integer,
  is_me boolean
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_since timestamptz;
  v_period text := lower(coalesce(period, 'week'));
  v_limit integer := greatest(1, least(coalesce(max_rows, 10), 50));
begin
  if not (
    public.is_admin()
    or public.is_class_staff(target_class_id)
    or exists (
      select 1 from public.class_members cm
      where cm.class_id = target_class_id
        and cm.user_id = auth.uid()
        and coalesce(cm.status::text, 'active') = 'active'
    )
  ) then
    raise exception 'Not a member of this class.';
  end if;

  v_since := case v_period
    when 'month' then date_trunc('month', now() at time zone 'Africa/Nairobi') at time zone 'Africa/Nairobi'
    when 'all' then '1970-01-01 00:00:00+00'::timestamptz
    else date_trunc('week', now() at time zone 'Africa/Nairobi') at time zone 'Africa/Nairobi'
  end;

  return query
  with student_scores as (
    select
      cm.user_id as uid,
      case
        when cm.user_id = auth.uid() or coalesce(p.show_on_leaderboard, true)
          then coalesce(nullif(trim(p.full_name), ''), 'Student')
        else 'Anonymous'
      end::text as dname,
      coalesce(sum(pl.points) filter (where pl.created_at >= v_since and (pl.class_id = target_class_id or pl.class_id is null)), 0)::integer as pts
    from public.class_members cm
    join public.profiles p on p.id = cm.user_id
    left join public.user_roles ur on ur.user_id = cm.user_id
    left join public.points_ledger pl on pl.user_id = cm.user_id
    where cm.class_id = target_class_id
      and coalesce(cm.status::text, 'active') = 'active'
      and lower(coalesce(ur.role::text, 'student')) = 'student'
    group by cm.user_id, p.full_name, p.show_on_leaderboard
  ),
  ranked as (
    select
      row_number() over (order by ss.pts desc, ss.dname asc)::integer as rk,
      ss.dname,
      ss.pts,
      (ss.uid = auth.uid()) as me
    from student_scores ss
  )
  select r.rk as rank_no, r.dname as display_name, r.pts as points, r.me as is_me
  from ranked r
  where r.rk <= v_limit or r.me
  order by r.rk asc;
end;
$$;

grant execute on function public.class_leaderboard(uuid, text, integer) to authenticated;

create or replace function public.set_leaderboard_visibility(visible boolean)
returns void
language sql
security definer
set search_path = public
as $$
  update public.profiles
  set show_on_leaderboard = coalesce(visible, true)
  where id = auth.uid();
$$;

grant execute on function public.set_leaderboard_visibility(boolean) to authenticated;

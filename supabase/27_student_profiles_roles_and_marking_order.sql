-- CLASSREP - STUDENT PROFILES VISIBILITY, STUDENT EXAM ACCESS & ORDERED MARKING
-- Run once in Supabase Dashboard -> SQL Editor. Safe to re-run anytime (idempotent).
--
-- Fixes three issues:
-- 1. Newly registered users entering a class have missing/invisible names & data:
--    - Ensures public.profiles has the required columns + user-self INSERT/UPDATE/SELECT policies
--      + classmate/staff SELECT visibility.
--    - Installs/repairs the auth.users signup trigger (handle_new_user) so full_name,
--      admission_number, email, and default 'student' role are ALWAYS written on signup.
--    - Backfills existing users whose profiles or user_roles rows were missing or blank.
-- 2. "Only students can take this" blocking students & class prefects on Exams / Challenges:
--    - Normalizes role checks (treats NULL, '', whitespace as 'student' and allows
--      student + class_rep / class_prefect / prefect / rep).
-- 3. Ordered exam marking by Name and Admission Number:
--    - Upgrades list_exam_submissions() to backfill blank profile fields from auth.users
--      metadata and return deterministic case-insensitive Name + Admission Number ordering.

-- ============================================================================
-- 1. PROFILES COLUMNS & SELF-HEALING SIGNUP TRIGGER
-- ============================================================================
do $$
begin
  if to_regclass('public.profiles') is not null then
    alter table public.profiles add column if not exists email text;
    alter table public.profiles add column if not exists full_name text;
    alter table public.profiles add column if not exists admission_number text;
    alter table public.profiles add column if not exists phone text;
    alter table public.profiles add column if not exists university_id uuid;
    alter table public.profiles add column if not exists school_id uuid;
    alter table public.profiles add column if not exists programme_id uuid;
    alter table public.profiles add column if not exists year_of_study integer;
    alter table public.profiles add column if not exists accepted_terms_at timestamptz;
    alter table public.profiles add column if not exists terms_version text;
  end if;
end $$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_name text;
  v_adm text;
begin
  v_name := coalesce(
    nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''),
    nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
    nullif(split_part(coalesce(new.email, ''), '@', 1), ''),
    'Student'
  );
  v_adm := nullif(trim(coalesce(
    new.raw_user_meta_data ->> 'admission_number',
    new.raw_user_meta_data ->> 'admission_no',
    ''
  )), '');

  if to_regclass('public.profiles') is not null then
    insert into public.profiles (id, email, full_name, admission_number)
    values (new.id, new.email, v_name, v_adm)
    on conflict (id) do update
    set email = coalesce(nullif(trim(public.profiles.email), ''), excluded.email),
        full_name = coalesce(nullif(trim(public.profiles.full_name), ''), excluded.full_name),
        admission_number = coalesce(nullif(trim(public.profiles.admission_number), ''), excluded.admission_number);
  end if;

  if to_regclass('public.user_roles') is not null then
    insert into public.user_roles (user_id, role, verified)
    values (new.id, 'student', false)
    on conflict (user_id) do nothing;
  end if;

  return new;
exception when others then
  -- Never block auth signup if an optional profile column/constraint fails
  return new;
end;
$$;

do $$
begin
  drop trigger if exists on_auth_user_created on auth.users;
  create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();
exception when insufficient_privilege then
  null;
end $$;

-- ============================================================================
-- 2. BACKFILL EXISTING USERS' PROFILES & STUDENT ROLES
-- ============================================================================
do $$
begin
  if to_regclass('public.profiles') is not null then
    insert into public.profiles (id, email, full_name, admission_number)
    select
      au.id,
      au.email,
      coalesce(
        nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''),
        nullif(trim(au.raw_user_meta_data ->> 'name'), ''),
        nullif(split_part(coalesce(au.email, ''), '@', 1), ''),
        'Student'
      ),
      nullif(trim(coalesce(
        au.raw_user_meta_data ->> 'admission_number',
        au.raw_user_meta_data ->> 'admission_no',
        ''
      )), '')
    from auth.users au
    on conflict (id) do update
    set email = coalesce(nullif(trim(public.profiles.email), ''), excluded.email),
        full_name = coalesce(nullif(trim(public.profiles.full_name), ''), excluded.full_name),
        admission_number = coalesce(nullif(trim(public.profiles.admission_number), ''), excluded.admission_number);
  end if;

  if to_regclass('public.user_roles') is not null then
    -- Repair empty/whitespace roles back to 'student'
    begin
      update public.user_roles
      set role = 'student'
      where nullif(trim(role::text), '') is null;
    exception when others then
      null;
    end;

    -- Ensure every auth user has a user_roles row defaulting to 'student'
    insert into public.user_roles (user_id, role, verified)
    select au.id, 'student', false
    from auth.users au
    on conflict (user_id) do nothing;
  end if;
end $$;

-- ============================================================================
-- 3. RLS VISIBILITY FOR PROFILES & USER ROLES (NO RECURSION)
-- ============================================================================
create or replace function public.shares_class_with(target_user uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select auth.uid() is not null and (
    auth.uid() = target_user
    or public.is_admin()
    or exists (
      select 1
      from public.class_members m1
      join public.class_members m2 on m2.class_id = m1.class_id
      where m1.user_id = auth.uid()
        and m2.user_id = target_user
    )
  );
$$;

grant execute on function public.shares_class_with(uuid) to authenticated;

do $$
begin
  if to_regclass('public.profiles') is not null then
    alter table public.profiles enable row level security;

    drop policy if exists "Users insert own profile" on public.profiles;
    create policy "Users insert own profile"
      on public.profiles for insert
      to authenticated
      with check (id = auth.uid());

    drop policy if exists "Users update own profile" on public.profiles;
    create policy "Users update own profile"
      on public.profiles for update
      to authenticated
      using (id = auth.uid())
      with check (id = auth.uid());

    drop policy if exists "Users read class profiles" on public.profiles;
    create policy "Users read class profiles"
      on public.profiles for select
      to authenticated
      using (public.shares_class_with(id));
  end if;

  if to_regclass('public.user_roles') is not null then
    alter table public.user_roles enable row level security;

    drop policy if exists "Users read own role" on public.user_roles;
    create policy "Users read own role"
      on public.user_roles for select
      to authenticated
      using (user_id = auth.uid() or public.is_admin());
  end if;
exception when insufficient_privilege then
  null;
end $$;

-- ============================================================================
-- 4. FIX start_quiz(): ALLOW STUDENTS & CLASS PREFECTS, HANDLE BLANK ROLES
-- ============================================================================
create or replace function public.is_student_exam_eligible(p_user_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_role text;
begin
  select nullif(trim(lower(ur.role::text)), '')
  into v_role
  from public.user_roles ur
  where ur.user_id = p_user_id
  limit 1;

  return coalesce(v_role, 'student') in (
    'student',
    'class_rep',
    'class representative',
    'class_prefect',
    'prefect',
    'rep'
  );
end;
$$;

grant execute on function public.is_student_exam_eligible(uuid) to authenticated;

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
  started_at timestamptz
)
language plpgsql security definer set search_path = public
as $$
#variable_conflict use_column
declare
  qz record;
  att record;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select z.id, z.class_id, z.active, z.opens_at, z.closes_at, z.time_limit_minutes, z.shuffle_questions
  into qz
  from public.quizzes z
  where z.id = target_quiz_id;

  if not found then
    raise exception 'Not found';
  end if;

  if not public.is_student_exam_eligible(auth.uid()) then
    raise exception 'Only students can take this';
  end if;

  if not public.is_class_member(qz.class_id) then
    raise exception 'You are not a member of this class';
  end if;

  if not qz.active or now() < qz.opens_at or now() >= qz.closes_at then
    raise exception 'This is not open';
  end if;

  select a.quiz_id, a.started_at, a.submitted_at
  into att
  from public.quiz_attempts a
  where a.quiz_id = target_quiz_id and a.student_id = auth.uid();

  if found then
    if att.submitted_at is not null then
      raise exception 'You have already taken this';
    end if;

    if qz.time_limit_minutes is not null
       and now() > att.started_at + make_interval(mins => qz.time_limit_minutes) + interval '30 seconds' then
      raise exception 'Your time is up';
    end if;
  else
    insert into public.quiz_attempts (quiz_id, student_id)
    values (target_quiz_id, auth.uid());

    select a.quiz_id, a.started_at, a.submitted_at
    into att
    from public.quiz_attempts a
    where a.quiz_id = target_quiz_id and a.student_id = auth.uid();
  end if;

  return query
  select q.id, q.ord, q.question_type, q.question, q.options, q.marks, qz.time_limit_minutes, att.started_at
  from public.quiz_questions q
  where q.quiz_id = target_quiz_id
  order by case when qz.shuffle_questions then md5(auth.uid()::text || q.id::text) else null end, q.ord;
end;
$$;

grant execute on function public.start_quiz(uuid) to authenticated;

-- ============================================================================
-- 5. ORDERED EXAM SUBMISSIONS (CASE-INSENSITIVE NAME + ADMISSION NO + METADATA FALLBACK)
-- ============================================================================
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
  marking_status text
)
language plpgsql stable security definer set search_path = public, auth
as $$
#variable_conflict use_column
declare
  mx numeric;
begin
  if not public.can_mark_quiz(target_quiz_id) then
    raise exception 'Not authorized';
  end if;

  select coalesce(sum(k.marks), 0) into mx from public.quiz_questions k where k.quiz_id = target_quiz_id;

  return query
  select
    a.student_id,
    coalesce(
      nullif(trim(p.full_name), ''),
      nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''),
      nullif(split_part(coalesce(au.email, ''), '@', 1), ''),
      'Student'
    ) as resolved_name,
    coalesce(
      nullif(trim(p.admission_number), ''),
      nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''),
      '-'
    ) as resolved_admission,
    a.submitted_at,
    a.late,
    a.mcq_marks,
    a.written_marks,
    case when a.marking_status in ('auto', 'released') then a.score else null end,
    mx,
    a.marking_status
  from public.quiz_attempts a
  left join public.profiles p on p.id = a.student_id
  left join auth.users au on au.id = a.student_id
  where a.quiz_id = target_quiz_id and a.submitted_at is not null
  order by
    case a.marking_status when 'pending' then 0 when 'marking' then 1 else 2 end,
    lower(trim(coalesce(
      nullif(trim(p.full_name), ''),
      nullif(trim(au.raw_user_meta_data ->> 'full_name'), ''),
      ''
    ))),
    lower(trim(coalesce(
      nullif(trim(p.admission_number), ''),
      nullif(trim(au.raw_user_meta_data ->> 'admission_number'), ''),
      ''
    )));
end;
$$;

grant execute on function public.list_exam_submissions(uuid) to authenticated;

notify pgrst, 'reload schema';

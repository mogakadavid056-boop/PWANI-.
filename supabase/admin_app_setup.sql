-- ============================================================================
-- ClassRep Admin App - Supabase Database Setup SQL
-- Run this once in your Supabase Dashboard -> SQL Editor -> New Query -> Run.
-- Safe to run multiple times (idempotent).
-- ============================================================================

-- 1. Helper function: returns TRUE only when the signed-in user is an Admin
create or replace function public.is_admin()
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from public.user_roles ur
    where ur.user_id = auth.uid()
      and lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin')
  );
$$;

grant execute on function public.is_admin() to authenticated;

-- 2. RLS Policies so Admins can manage University Structure & read all Classes
alter table if exists public.schools enable row level security;
drop policy if exists "Admins manage schools" on public.schools;
create policy "Admins manage schools"
  on public.schools
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.programmes enable row level security;
drop policy if exists "Admins manage programmes" on public.programmes;
create policy "Admins manage programmes"
  on public.programmes
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.academic_years enable row level security;
drop policy if exists "Admins manage academic_years" on public.academic_years;
create policy "Admins manage academic_years"
  on public.academic_years
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.classes enable row level security;
drop policy if exists "Admins manage classes" on public.classes;
drop policy if exists "Admins read all classes" on public.classes;
create policy "Admins manage classes"
  on public.classes
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.profiles enable row level security;
drop policy if exists "Admins manage profiles" on public.profiles;
create policy "Admins manage profiles"
  on public.profiles
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.user_roles enable row level security;
drop policy if exists "Admins manage user_roles" on public.user_roles;
create policy "Admins manage user_roles"
  on public.user_roles
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.class_members enable row level security;
drop policy if exists "Admins manage class_members" on public.class_members;
create policy "Admins manage class_members"
  on public.class_members
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.student_representatives enable row level security;
drop policy if exists "Admins manage student_representatives" on public.student_representatives;
create policy "Admins manage student_representatives"
  on public.student_representatives
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.courses enable row level security;
drop policy if exists "Admins manage courses" on public.courses;
create policy "Admins manage courses"
  on public.courses
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.course_lecturers enable row level security;
drop policy if exists "Admins manage course_lecturers" on public.course_lecturers;
create policy "Admins manage course_lecturers"
  on public.course_lecturers
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.notices enable row level security;
drop policy if exists "Admins manage notices" on public.notices;
create policy "Admins manage notices"
  on public.notices
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.schedules enable row level security;
drop policy if exists "Admins manage schedules" on public.schedules;
create policy "Admins manage schedules"
  on public.schedules
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.materials enable row level security;
drop policy if exists "Admins manage materials" on public.materials;
create policy "Admins manage materials"
  on public.materials
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.attendance_sessions enable row level security;
drop policy if exists "Admins manage attendance_sessions" on public.attendance_sessions;
create policy "Admins manage attendance_sessions"
  on public.attendance_sessions
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.attendance_records enable row level security;
drop policy if exists "Admins manage attendance_records" on public.attendance_records;
create policy "Admins manage attendance_records"
  on public.attendance_records
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table if exists public.polls enable row level security;
drop policy if exists "Admins manage polls" on public.polls;
create policy "Admins manage polls"
  on public.polls
  for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- 3. Executive Dashboard KPIs RPC
create or replace function public.admin_dashboard_stats()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_total_users bigint := 0;
  v_students bigint := 0;
  v_reps bigint := 0;
  v_lecturers bigint := 0;
  v_unverified_lecturers bigint := 0;
  v_admins bigint := 0;
  v_schools bigint := 0;
  v_programmes bigint := 0;
  v_classes bigint := 0;
  v_registers bigint := 0;
  v_open_registers bigint := 0;
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  select count(*) into v_total_users from public.profiles;

  select
    count(*) filter (where lower(coalesce(ur.role::text, 'student')) = 'student'),
    count(*) filter (where lower(coalesce(ur.role::text, '')) in ('class_rep', 'prefect', 'rep')),
    count(*) filter (where lower(coalesce(ur.role::text, '')) = 'lecturer'),
    count(*) filter (where lower(coalesce(ur.role::text, '')) = 'lecturer' and coalesce(ur.verified, false) = false),
    count(*) filter (where lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin'))
  into v_students, v_reps, v_lecturers, v_unverified_lecturers, v_admins
  from public.profiles p
  left join public.user_roles ur on ur.user_id = p.id;

  select count(*) into v_schools from public.schools;
  select count(*) into v_programmes from public.programmes;
  select count(*) into v_classes from public.classes;

  if to_regclass('public.attendance_sessions') is not null then
    execute 'select count(*), count(*) filter (where lower(coalesce(status::text, '''')) = ''open'') from public.attendance_sessions'
      into v_registers, v_open_registers;
  end if;

  return jsonb_build_object(
    'total_users', v_total_users,
    'total_students', v_students,
    'total_class_reps', v_reps,
    'total_lecturers', v_lecturers,
    'unverified_lecturers', v_unverified_lecturers,
    'total_admins', v_admins,
    'total_schools', v_schools,
    'total_programmes', v_programmes,
    'total_classes', v_classes,
    'total_registers', v_registers,
    'open_registers', v_open_registers
  );
end;
$$;

grant execute on function public.admin_dashboard_stats() to authenticated;

-- 4. University-wide Class Browser RPC for Oversight tab
create or replace function public.admin_list_classes(
  search_text text default '',
  max_rows integer default 60
)
returns table (
  id uuid,
  name text,
  code text,
  join_code text,
  year_of_study integer,
  programme_name text,
  member_count bigint,
  register_count bigint,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_q text := trim(coalesce(search_text, ''));
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  return query
  select
    c.id,
    c.name::text,
    c.code::text,
    c.join_code::text,
    c.year_of_study::integer,
    pr.name::text as programme_name,
    coalesce((select count(*) from public.class_members cm where cm.class_id = c.id and coalesce(cm.status::text, 'active') = 'active'), 0)::bigint as member_count,
    coalesce((select count(*) from public.attendance_sessions s where s.class_id = c.id), 0)::bigint as register_count,
    c.created_at
  from public.classes c
  left join public.programmes pr on pr.id = c.programme_id
  where v_q = ''
     or c.name ilike '%' || v_q || '%'
     or coalesce(c.code, '') ilike '%' || v_q || '%'
     or coalesce(c.join_code, '') ilike '%' || v_q || '%'
     or coalesce(pr.name, '') ilike '%' || v_q || '%'
  order by c.created_at desc
  limit greatest(1, least(coalesce(max_rows, 60), 200));
end;
$$;

grant execute on function public.admin_list_classes(text, integer) to authenticated;

-- 5. Atomic Academic Year switcher
create or replace function public.admin_set_current_academic_year(target_year_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  update public.academic_years
  set is_current = (id = target_year_id);
end;
$$;

grant execute on function public.admin_set_current_academic_year(uuid) to authenticated;

-- 6. Users & Lecturer Approvals RPCs
drop function if exists public.list_users(text, integer);
create or replace function public.list_users(
  search_text text default '',
  max_rows integer default 100
)
returns table (
  id uuid,
  full_name text,
  email text,
  admission_number text,
  role text,
  verified boolean
)
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_q text := trim(coalesce(search_text, ''));
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  return query
  select
    p.id,
    coalesce(p.full_name::text, ''),
    coalesce(au.email::text, ''),
    coalesce(p.admission_number::text, ''),
    lower(coalesce(ur.role::text, 'student')),
    coalesce(ur.verified, false)
  from public.profiles p
  left join auth.users au on au.id = p.id
  left join public.user_roles ur on ur.user_id = p.id
  where v_q = ''
     or coalesce(p.full_name, '') ilike '%' || v_q || '%'
     or coalesce(au.email, '') ilike '%' || v_q || '%'
     or coalesce(p.admission_number, '') ilike '%' || v_q || '%'
     or coalesce(ur.role::text, '') ilike '%' || v_q || '%'
  order by lower(coalesce(p.full_name, au.email, '')) asc
  limit greatest(1, least(coalesce(max_rows, 100), 500));
end;
$$;

grant execute on function public.list_users(text, integer) to authenticated;

create or replace function public.admin_assign_role(
  target_user_id uuid,
  new_role text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  insert into public.user_roles (user_id, role)
  values (target_user_id, lower(trim(new_role)))
  on conflict (user_id) do update
    set role = lower(trim(excluded.role::text));
end;
$$;

grant execute on function public.admin_assign_role(uuid, text) to authenticated;

create or replace function public.admin_set_lecturer_verified(
  target_user_id uuid,
  is_verified boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  insert into public.user_roles (user_id, role, verified)
  values (target_user_id, 'lecturer', is_verified)
  on conflict (user_id) do update
    set role = case when lower(coalesce(user_roles.role::text, '')) = '' then 'lecturer' else user_roles.role end,
        verified = is_verified;
end;
$$;

grant execute on function public.admin_set_lecturer_verified(uuid, boolean) to authenticated;

drop function if exists public.admin_list_pending_join_requests();
create or replace function public.admin_list_pending_join_requests()
returns table (
  class_id uuid,
  class_name text,
  user_id uuid,
  full_name text,
  email text,
  staff_number text
)
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required';
  end if;

  return query
  select
    cm.class_id,
    coalesce(c.name::text, 'Class'),
    cm.user_id,
    coalesce(p.full_name::text, 'Lecturer'),
    coalesce(au.email::text, ''),
    coalesce(p.admission_number::text, '')
  from public.class_members cm
  left join public.classes c on c.id = cm.class_id
  left join public.profiles p on p.id = cm.user_id
  left join auth.users au on au.id = cm.user_id
  where lower(coalesce(cm.status::text, '')) = 'pending'
  order by c.name asc, p.full_name asc;
end;
$$;

grant execute on function public.admin_list_pending_join_requests() to authenticated;

-- ============================================================================
-- HOW TO MAKE YOUR ACCOUNT AN ADMIN (Run once with your own email):
--
-- insert into public.user_roles (user_id, role, verified)
-- select id, 'admin', true
-- from auth.users
-- where lower(email) = lower('YOUR_ADMIN_EMAIL@university.edu')
-- on conflict (user_id) do update
--   set role = 'admin', verified = true;
-- ============================================================================

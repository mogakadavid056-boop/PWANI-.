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
drop policy if exists "Admins read all classes" on public.classes;
create policy "Admins read all classes"
  on public.classes
  for select
  to authenticated
  using (public.is_admin());

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
    coalesce((select count(*) from public.class_members cm where cm.class_id = c.id), 0)::bigint as member_count,
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

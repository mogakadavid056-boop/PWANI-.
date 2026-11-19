package com.example.admin.ui

object AdminSqlSnippet {
  val SETUP_SQL = """
    -- Run in Supabase Dashboard -> SQL Editor -> New Query -> Run
    create or replace function public.is_admin()
    returns boolean language sql security definer stable set search_path = public as $$
      select exists (
        select 1 from public.user_roles ur
        where ur.user_id = auth.uid()
          and lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin')
      );
    $$;

    alter table if exists public.schools enable row level security;
    drop policy if exists "Admins manage schools" on public.schools;
    create policy "Admins manage schools" on public.schools
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.programmes enable row level security;
    drop policy if exists "Admins manage programmes" on public.programmes;
    create policy "Admins manage programmes" on public.programmes
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.academic_years enable row level security;
    drop policy if exists "Admins manage academic_years" on public.academic_years;
    create policy "Admins manage academic_years" on public.academic_years
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.classes enable row level security;
    drop policy if exists "Admins manage classes" on public.classes;
    drop policy if exists "Admins read all classes" on public.classes;
    create policy "Admins manage classes" on public.classes
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.profiles enable row level security;
    drop policy if exists "Admins manage profiles" on public.profiles;
    create policy "Admins manage profiles" on public.profiles
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.user_roles enable row level security;
    drop policy if exists "Admins manage user_roles" on public.user_roles;
    create policy "Admins manage user_roles" on public.user_roles
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.class_members enable row level security;
    drop policy if exists "Admins manage class_members" on public.class_members;
    create policy "Admins manage class_members" on public.class_members
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.courses enable row level security;
    drop policy if exists "Admins manage courses" on public.courses;
    create policy "Admins manage courses" on public.courses
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    alter table if exists public.course_lecturers enable row level security;
    drop policy if exists "Admins manage course_lecturers" on public.course_lecturers;
    create policy "Admins manage course_lecturers" on public.course_lecturers
      for all to authenticated using (public.is_admin()) with check (public.is_admin());

    drop function if exists public.list_users(text, integer);
    create or replace function public.list_users(search_text text default '', max_rows integer default 100)
    returns table (id uuid, full_name text, email text, admission_number text, role text, verified boolean)
    language plpgsql security definer set search_path = public, auth as $$
    declare v_q text := trim(coalesce(search_text, ''));
    begin
      if not public.is_admin() then raise exception 'Forbidden'; end if;
      return query
      select p.id, coalesce(p.full_name::text, ''), coalesce(au.email::text, ''),
        coalesce(p.admission_number::text, ''), lower(coalesce(ur.role::text, 'student')), coalesce(ur.verified, false)
      from public.profiles p
      left join auth.users au on au.id = p.id
      left join public.user_roles ur on ur.user_id = p.id
      where v_q = '' or coalesce(p.full_name, '') ilike '%'||v_q||'%' or coalesce(au.email, '') ilike '%'||v_q||'%'
         or coalesce(p.admission_number, '') ilike '%'||v_q||'%'
      order by lower(coalesce(p.full_name, au.email, '')) asc
      limit greatest(1, least(coalesce(max_rows, 100), 500));
    end;
    $$;
    grant execute on function public.list_users(text, integer) to authenticated;

    create or replace function public.admin_assign_role(target_user_id uuid, new_role text)
    returns void language plpgsql security definer set search_path = public as $$
    begin
      if not public.is_admin() then raise exception 'Forbidden'; end if;
      insert into public.user_roles (user_id, role) values (target_user_id, lower(trim(new_role)))
      on conflict (user_id) do update set role = lower(trim(excluded.role::text));
    end;
    $$;
    grant execute on function public.admin_assign_role(uuid, text) to authenticated;

    create or replace function public.admin_set_lecturer_verified(target_user_id uuid, is_verified boolean)
    returns void language plpgsql security definer set search_path = public as $$
    begin
      if not public.is_admin() then raise exception 'Forbidden'; end if;
      insert into public.user_roles (user_id, role, verified) values (target_user_id, 'lecturer', is_verified)
      on conflict (user_id) do update set role = case when lower(coalesce(user_roles.role::text, '')) = '' then 'lecturer' else user_roles.role end, verified = is_verified;
    end;
    $$;
    grant execute on function public.admin_set_lecturer_verified(uuid, boolean) to authenticated;
  """.trimIndent()
}

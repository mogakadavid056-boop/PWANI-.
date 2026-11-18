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
    drop policy if exists "Admins read all classes" on public.classes;
    create policy "Admins read all classes" on public.classes
      for select to authenticated using (public.is_admin());
  """.trimIndent()
}

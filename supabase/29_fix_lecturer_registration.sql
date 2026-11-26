-- ============================================================================
-- CLASSREP - COLLECT & PERMANENTLY FIX LECTURER REGISTRATION
-- Run once in Supabase Dashboard -> SQL Editor -> Run. Safe to re-run (idempotent).
--
-- What this fixes:
--   1. Ensures normalize_passcode() & app_secrets('lecturer_passcode') = '87654321'.
--   2. Fixes trg_role_changed() so INSERT into user_roles never crashes on OLD.role.
--   3. Fixes become_lecturer(passcode) to UPSERT (INSERT ... ON CONFLICT DO UPDATE)
--      into user_roles instead of UPDATE (which affected 0 rows if row was missing).
--   4. Repairs handle_new_user() trigger & user_roles RLS ("Users read own role").
--   5. Collects & upgrades all existing users who already entered the valid
--      lecturer passcode (from passcode_attempts / audit_logs) yet remained student,
--      clears old failed attempt lockouts, and backfills missing user_roles rows.
-- ============================================================================

-- 1. Ensure normalize_passcode(text) exists without breaking existing signature
do $$
begin
  if to_regprocedure('public.normalize_passcode(text)') is null then
    create function public.normalize_passcode(passcode text)
    returns text
    language sql
    immutable
    as $fn$
      select btrim(coalesce(passcode, ''));
    $fn$;
  end if;
end $$;

-- 2. Ensure app_secrets exists & set lecturer passcode to 87654321
create table if not exists public.app_secrets (
  key text primary key,
  value_hash text not null,
  updated_at timestamptz not null default now()
);

alter table public.app_secrets enable row level security;

insert into public.app_secrets (key, value_hash)
values (
  'lecturer_passcode',
  encode(sha256(convert_to(public.normalize_passcode('87654321'), 'UTF8')), 'hex')
)
on conflict (key) do update
set value_hash = excluded.value_hash;

-- 3. Ensure user_roles has updated_at & readable own-row RLS policy
alter table if exists public.user_roles
  add column if not exists updated_at timestamptz not null default now();

do $$
begin
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

-- 4. Fix trg_role_changed() so INSERT never crashes referencing OLD.role
create or replace function public.trg_role_changed()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  pname text;
  v_old_role text := case when tg_op = 'UPDATE' then lower(coalesce(old.role::text, '')) else '' end;
  v_new_role text := lower(coalesce(new.role::text, ''));
  v_old_verified boolean := case when tg_op = 'UPDATE' then coalesce(old.verified, false) else false end;
  v_new_verified boolean := coalesce(new.verified, false);
begin
  begin
    if v_new_role = 'lecturer' and v_old_role <> 'lecturer' then
      select p.full_name into pname from public.profiles p where p.id = new.user_id;
      perform public.notify_users(
        public.admin_audience(),
        'lecturer_registered',
        'New lecturer registered',
        coalesce(pname, 'Someone') || ' registered as a lecturer with the passcode.',
        null,
        jsonb_build_object('user_id', new.user_id)
      );
    end if;

    if tg_op = 'UPDATE'
       and v_new_role = 'lecturer'
       and v_old_role = 'lecturer'
       and v_new_verified
       and not v_old_verified then
      perform public.notify_users(
        array[new.user_id],
        'lecturer_verified',
        'Your lecturer access is active again',
        'You can join and manage your classes with their class codes.'
      );
    end if;
  exception when others then
    null;
  end;

  return new;
end;
$$;

-- 5. Ensure auth signup trigger always creates profile + default student user_roles row
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
    values (new.id, 'student'::public.app_role, false)
    on conflict (user_id) do nothing;
  end if;

  return new;
exception when others then
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

-- 6. Fix become_lecturer(passcode): use UPSERT so missing user_roles rows never fail silently
create or replace function public.become_lecturer(passcode text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  my_role text;
  my_verified boolean := false;
  recent_fails integer := 0;
  stored text;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select lower(coalesce(r.role::text, '')), coalesce(r.verified, false)
  into my_role, my_verified
  from public.user_roles r
  where r.user_id = auth.uid()
  limit 1;

  if my_role in ('admin', 'super_admin', 'superadmin') then
    return true;
  end if;

  if my_role = 'lecturer' and my_verified then
    return true;
  end if;

  if exists (
    select 1 from public.class_members m
    where m.user_id = auth.uid() and lower(coalesce(m.status::text, 'active')) = 'active'
  ) then
    raise exception 'Students who are already in a class cannot become lecturers. Ask an administrator.';
  end if;

  if to_regclass('public.passcode_attempts') is not null then
    select count(*) into recent_fails
    from public.passcode_attempts a
    where a.user_id = auth.uid()
      and a.success = false
      and a.attempted_at > now() - interval '1 hour';

    if recent_fails >= 5 then
      raise exception 'Too many wrong attempts. Try again in an hour.';
    end if;
  end if;

  select s.value_hash into stored
  from public.app_secrets s
  where s.key = 'lecturer_passcode';

  if stored is not null
     and encode(sha256(convert_to(public.normalize_passcode(passcode), 'UTF8')), 'hex') = stored then

    insert into public.user_roles (user_id, role, verified, updated_at)
    values (auth.uid(), 'lecturer'::public.app_role, true, now())
    on conflict (user_id) do update
      set role = 'lecturer'::public.app_role,
          verified = true,
          updated_at = now();

    if to_regclass('public.passcode_attempts') is not null then
      insert into public.passcode_attempts (user_id, success) values (auth.uid(), true);
    end if;

    if to_regclass('public.audit_logs') is not null then
      insert into public.audit_logs (user_id, action, table_name, record_id)
      values (auth.uid(), 'become_lecturer', 'user_roles', auth.uid());
    end if;

    return true;
  end if;

  if to_regclass('public.passcode_attempts') is not null then
    insert into public.passcode_attempts (user_id, success) values (auth.uid(), false);
  end if;
  return false;
end;
$$;

grant execute on function public.become_lecturer(text) to authenticated;

-- 7. COLLECT & REPAIR AFFECTED ACCOUNTS
do $$
begin
  -- Clear failed passcode attempt lockouts caused before passcode/trigger fix
  if to_regclass('public.passcode_attempts') is not null then
    delete from public.passcode_attempts where success = false;
  end if;

  -- Backfill missing profiles for all auth users
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
    -- Collect & upgrade anyone whose passcode check already succeeded
    if to_regclass('public.passcode_attempts') is not null then
      insert into public.user_roles (user_id, role, verified, updated_at)
      select distinct pa.user_id, 'lecturer'::public.app_role, true, now()
      from public.passcode_attempts pa
      where pa.success = true
        and exists (select 1 from auth.users au where au.id = pa.user_id)
      on conflict (user_id) do update
        set role = excluded.role,
            verified = true,
            updated_at = now()
        where lower(coalesce(public.user_roles.role::text, '')) not in ('admin', 'super_admin', 'superadmin');
    end if;

    -- Collect & upgrade anyone recorded in audit_logs for become_lecturer
    if to_regclass('public.audit_logs') is not null then
      insert into public.user_roles (user_id, role, verified, updated_at)
      select distinct al.user_id, 'lecturer'::public.app_role, true, now()
      from public.audit_logs al
      where al.action = 'become_lecturer'
        and al.user_id is not null
        and exists (select 1 from auth.users au where au.id = al.user_id)
      on conflict (user_id) do update
        set role = excluded.role,
            verified = true,
            updated_at = now()
        where lower(coalesce(public.user_roles.role::text, '')) not in ('admin', 'super_admin', 'superadmin');
    end if;

    -- Ensure every remaining auth user has a user_roles row defaulting to 'student'
    insert into public.user_roles (user_id, role, verified)
    select au.id, 'student'::public.app_role, false
    from auth.users au
    on conflict (user_id) do nothing;
  end if;
end $$;

notify pgrst, 'reload schema';

-- 8. Show all verified lecturers collected/active right now
select
  au.id as user_id,
  coalesce(p.full_name, au.raw_user_meta_data ->> 'full_name', '') as full_name,
  au.email,
  ur.role::text as role,
  ur.verified
from auth.users au
join public.user_roles ur on ur.user_id = au.id
left join public.profiles p on p.id = au.id
where lower(coalesce(ur.role::text, '')) = 'lecturer'
order by ur.updated_at desc nulls last, au.created_at desc;

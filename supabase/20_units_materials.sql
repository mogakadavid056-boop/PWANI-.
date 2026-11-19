-- ============================================================================
-- ClassRep Feature Upgrade (1/3): Units, Materials by Unit & Shareable Join Code
-- Run FIRST in Supabase SQL Editor. Idempotent (safe to re-run).
-- ============================================================================

-- 1. Courses (Units) table columns & constraints
create table if not exists public.courses (
  id uuid primary key default gen_random_uuid(),
  class_id uuid not null references public.classes(id) on delete cascade,
  code text not null default '',
  name text not null default '',
  description text not null default '',
  status text not null default 'approved',
  rejection_reason text null,
  lecturer_id uuid null references public.profiles(id) on delete set null,
  requested_by uuid null references public.profiles(id) on delete set null,
  reviewed_by uuid null references public.profiles(id) on delete set null,
  reviewed_at timestamptz null,
  created_at timestamptz not null default now()
);

alter table public.courses add column if not exists description text not null default '';
alter table public.courses add column if not exists status text not null default 'approved';
alter table public.courses add column if not exists rejection_reason text null;
alter table public.courses add column if not exists lecturer_id uuid null references public.profiles(id) on delete set null;
alter table public.courses add column if not exists requested_by uuid null references public.profiles(id) on delete set null;
alter table public.courses add column if not exists reviewed_by uuid null references public.profiles(id) on delete set null;
alter table public.courses add column if not exists reviewed_at timestamptz null;

do $$
begin
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='courses' and column_name='title') then
    execute 'update public.courses set name = coalesce(nullif(name, ''''), title, '''') where coalesce(name, '''') = ''''';
  end if;
end $$;

update public.courses set status = 'approved' where status is null or trim(status) = '';

create table if not exists public.course_lecturers (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  lecturer_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (course_id, lecturer_id)
);

-- Seed course_lecturers from any existing courses.lecturer_id
insert into public.course_lecturers (course_id, lecturer_id)
select id, lecturer_id from public.courses
where lecturer_id is not null
on conflict (course_id, lecturer_id) do nothing;

-- 2. Materials table: attach required/optional course_id unit reference
alter table if exists public.materials
  add column if not exists course_id uuid null references public.courses(id) on delete set null;

create index if not exists idx_courses_class_status on public.courses (class_id, status);
create index if not exists idx_materials_class_course on public.materials (class_id, course_id, created_at desc);

-- 3. Role helpers (safe enum casts)
create or replace function public.is_class_lecturer(p_class_id uuid)
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
      and lower(coalesce(ur.role::text, '')) = 'lecturer'
      and (
        exists (select 1 from public.classes c where c.id = p_class_id and c.created_by = auth.uid())
        or exists (
          select 1 from public.class_members cm
          where cm.class_id = p_class_id
            and cm.user_id = auth.uid()
            and coalesce(cm.status::text, 'active') = 'active'
        )
      )
  );
$$;

create or replace function public.is_class_staff(p_class_id uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select public.is_admin()
    or public.is_class_lecturer(p_class_id)
    or exists (
      select 1 from public.student_representatives sr
      where sr.class_id = p_class_id and sr.user_id = auth.uid() and coalesce(sr.active, true) = true
    );
$$;

grant execute on function public.is_class_lecturer(uuid) to authenticated;
grant execute on function public.is_class_staff(uuid) to authenticated;

-- 4. RLS for courses, course_lecturers, and Admin oversight policies
alter table public.courses enable row level security;
drop policy if exists "Class members read approved or staff read all courses" on public.courses;
create policy "Class members read approved or staff read all courses"
  on public.courses for select to authenticated
  using (
    public.is_admin()
    or public.is_class_staff(class_id)
    or (
      lower(coalesce(status, 'approved')) = 'approved'
      and exists (
        select 1 from public.class_members cm
        where cm.class_id = courses.class_id and cm.user_id = auth.uid()
      )
    )
  );

drop policy if exists "Admins full manage courses" on public.courses;
create policy "Admins full manage courses"
  on public.courses for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

alter table public.course_lecturers enable row level security;
drop policy if exists "Authenticated read course_lecturers" on public.course_lecturers;
create policy "Authenticated read course_lecturers"
  on public.course_lecturers for select to authenticated
  using (true);

drop policy if exists "Admins manage course_lecturers" on public.course_lecturers;
create policy "Admins manage course_lecturers"
  on public.course_lecturers for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- Ensure Admins can manage notices, schedules, materials, class_members, attendance, polls, groups
do $$
declare
  t text;
begin
  foreach t in array array['notices', 'schedules', 'materials', 'class_members', 'attendance_sessions', 'attendance_records', 'polls', 'poll_options', 'poll_votes', 'groups', 'discussion_groups', 'group_members', 'group_messages', 'student_representatives'] loop
    if to_regclass('public.' || t) is not null then
      execute format('alter table public.%I enable row level security', t);
      execute format('drop policy if exists "Admins manage %s" on public.%I', t, t);
      execute format('create policy "Admins manage %s" on public.%I for all to authenticated using (public.is_admin()) with check (public.is_admin())', t, t);
    end if;
  end loop;
end $$;

-- Storage bucket policies for class-materials (Admins + class staff upload/delete, members read)
do $$
begin
  if to_regclass('storage.objects') is not null then
    drop policy if exists "Admins manage class-materials storage" on storage.objects;
    create policy "Admins manage class-materials storage"
      on storage.objects for all to authenticated
      using (bucket_id = 'class-materials' and public.is_admin())
      with check (bucket_id = 'class-materials' and public.is_admin());
  end if;
exception when insufficient_privilege then
  null;
end $$;

-- 5. RPC: regenerate_join_code (lecturers of the class or admins)
create or replace function public.regenerate_join_code(target_class_id uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_chars text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  v_code text := '';
  v_i integer;
  v_tries integer := 0;
begin
  if not (public.is_admin() or public.is_class_lecturer(target_class_id)) then
    raise exception 'Only a lecturer of this class or an administrator can change the join code.';
  end if;

  loop
    v_tries := v_tries + 1;
    v_code := '';
    for v_i in 1..6 loop
      v_code := v_code || substr(v_chars, 1 + floor(random() * length(v_chars))::int, 1);
    end loop;
    exit when not exists (select 1 from public.classes where join_code = v_code) or v_tries > 25;
  end loop;

  update public.classes
  set join_code = v_code
  where id = target_class_id;

  if not found then
    raise exception 'Class not found.';
  end if;

  return v_code;
end;
$$;

grant execute on function public.regenerate_join_code(uuid) to authenticated;

-- 6. RPC: list_class_units (returns units with lecturer names & material counts)
create or replace function public.list_class_units(target_class_id uuid)
returns table (
  course_id uuid,
  code text,
  name text,
  description text,
  status text,
  lecturers text,
  lecturer_ids uuid[],
  material_count bigint,
  rejection_reason text,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_is_staff boolean := public.is_class_staff(target_class_id);
begin
  if not (v_is_staff or exists (
    select 1 from public.class_members cm
    where cm.class_id = target_class_id and cm.user_id = auth.uid()
  )) then
    raise exception 'You do not have access to units in this class.';
  end if;

  return query
  select
    c.id as course_id,
    coalesce(c.code, '')::text as code,
    coalesce(c.name, '')::text as name,
    coalesce(c.description, '')::text as description,
    lower(coalesce(c.status, 'approved'))::text as status,
    coalesce(
      (
        select string_agg(distinct p.full_name, ', ' order by p.full_name)
        from public.course_lecturers cl
        join public.profiles p on p.id = cl.lecturer_id
        where cl.course_id = c.id
      ),
      ''
    )::text as lecturers,
    coalesce(
      (
        select array_agg(distinct cl.lecturer_id)
        from public.course_lecturers cl
        where cl.course_id = c.id
      ),
      array[]::uuid[]
    ) as lecturer_ids,
    coalesce(
      (select count(*) from public.materials m where m.course_id = c.id and m.class_id = target_class_id),
      0
    )::bigint as material_count,
    c.rejection_reason::text,
    c.created_at
  from public.courses c
  where c.class_id = target_class_id
    and (v_is_staff or lower(coalesce(c.status, 'approved')) = 'approved')
  order by upper(coalesce(c.code, '')), c.created_at desc;
end;
$$;

grant execute on function public.list_class_units(uuid) to authenticated;

-- 7. RPC: request_unit (lecturer -> pending; admin -> approved)
create or replace function public.request_unit(
  target_class_id uuid,
  unit_code text,
  unit_name text,
  unit_description text default '',
  target_lecturer_id uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_code text := upper(trim(coalesce(unit_code, '')));
  v_name text := trim(coalesce(unit_name, ''));
  v_desc text := trim(coalesce(unit_description, ''));
  v_is_admin boolean := public.is_admin();
  v_is_lecturer boolean := public.is_class_lecturer(target_class_id);
  v_status text;
  v_id uuid;
  v_class_name text;
begin
  if not (v_is_admin or v_is_lecturer) then
    raise exception 'Only lecturers of this class or administrators can register a unit.';
  end if;

  if length(v_code) < 2 or length(v_name) < 2 then
    raise exception 'Please enter both a valid unit code and unit name.';
  end if;

  if exists (
    select 1 from public.courses c
    where c.class_id = target_class_id
      and upper(trim(coalesce(c.code, ''))) = v_code
      and lower(coalesce(c.status, 'approved')) in ('approved', 'pending')
  ) then
    raise exception 'Unit % is already registered or pending approval in this class.', v_code;
  end if;

  v_status := case when v_is_admin then 'approved' else 'pending' end;

  insert into public.courses (class_id, code, name, description, status, lecturer_id, requested_by, reviewed_by, reviewed_at)
  values (
    target_class_id,
    v_code,
    v_name,
    v_desc,
    v_status,
    coalesce(target_lecturer_id, case when v_is_lecturer then auth.uid() else null end),
    auth.uid(),
    case when v_is_admin then auth.uid() else null end,
    case when v_is_admin then now() else null end
  )
  returning id into v_id;

  if target_lecturer_id is not null then
    insert into public.course_lecturers (course_id, lecturer_id)
    values (v_id, target_lecturer_id)
    on conflict (course_id, lecturer_id) do nothing;
  elsif v_is_lecturer then
    insert into public.course_lecturers (course_id, lecturer_id)
    values (v_id, auth.uid())
    on conflict (course_id, lecturer_id) do nothing;
  end if;

  select name into v_class_name from public.classes where id = target_class_id;

  if not v_is_admin and to_regclass('public.notifications') is not null then
    insert into public.notifications (user_id, class_id, kind, title, body, data)
    select
      ur.user_id,
      target_class_id,
      'unit_pending',
      'New unit awaiting approval',
      v_code || ' - ' || v_name || ' (' || coalesce(v_class_name, 'Class') || ')',
      jsonb_build_object('course_id', v_id, 'class_id', target_class_id)
    from public.user_roles ur
    where lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin');
  end if;

  return jsonb_build_object('course_id', v_id, 'status', v_status, 'code', v_code, 'name', v_name);
end;
$$;

grant execute on function public.request_unit(uuid, text, text, text, uuid) to authenticated;

-- 8. RPC: list_pending_units (admin)
create or replace function public.list_pending_units()
returns table (
  course_id uuid,
  class_id uuid,
  class_name text,
  code text,
  name text,
  description text,
  requested_by uuid,
  requester_name text,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  return query
  select
    c.id as course_id,
    c.class_id,
    coalesce(cl.name, 'Class')::text as class_name,
    coalesce(c.code, '')::text as code,
    coalesce(c.name, '')::text as name,
    coalesce(c.description, '')::text as description,
    c.requested_by,
    coalesce(p.full_name, 'Lecturer')::text as requester_name,
    c.created_at
  from public.courses c
  left join public.classes cl on cl.id = c.class_id
  left join public.profiles p on p.id = c.requested_by
  where lower(coalesce(c.status, '')) = 'pending'
  order by c.created_at asc;
end;
$$;

grant execute on function public.list_pending_units() to authenticated;

-- 9. RPC: review_unit (admin approve or reject)
create or replace function public.review_unit(
  target_course_id uuid,
  approve boolean,
  reason text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_course public.courses%rowtype;
  v_kind text;
  v_title text;
  v_body text;
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  select * into v_course from public.courses where id = target_course_id;
  if not found then
    raise exception 'Unit not found.';
  end if;

  update public.courses
  set
    status = case when approve then 'approved' else 'rejected' end,
    rejection_reason = case when approve then null else nullif(trim(coalesce(reason, '')), '') end,
    reviewed_by = auth.uid(),
    reviewed_at = now()
  where id = target_course_id;

  if to_regclass('public.notifications') is not null then
    v_kind := case when approve then 'unit_approved' else 'unit_rejected' end;
    v_title := case when approve then 'Unit approved: ' || v_course.code else 'Unit rejected: ' || v_course.code end;
    v_body := case
      when approve then v_course.code || ' - ' || v_course.name || ' is now active for materials.'
      else v_course.code || ' - ' || v_course.name || coalesce(': ' || nullif(trim(coalesce(reason, '')), ''), ' was not approved.')
    end;

    insert into public.notifications (user_id, class_id, kind, title, body, data)
    select distinct uid, v_course.class_id, v_kind, v_title, v_body,
      jsonb_build_object('course_id', v_course.id, 'class_id', v_course.class_id)
    from (
      select v_course.requested_by as uid where v_course.requested_by is not null
      union
      select cl.lecturer_id as uid from public.course_lecturers cl where cl.course_id = v_course.id
    ) targets;
  end if;
end;
$$;

grant execute on function public.review_unit(uuid, boolean, text) to authenticated;

-- 10. RPC: admin_set_unit_lecturer (admin assigns/removes lecturer on unit)
create or replace function public.admin_set_unit_lecturer(
  target_course_id uuid,
  target_lecturer_id uuid,
  assign boolean default true
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  if assign then
    insert into public.course_lecturers (course_id, lecturer_id)
    values (target_course_id, target_lecturer_id)
    on conflict (course_id, lecturer_id) do nothing;

    update public.courses
    set lecturer_id = coalesce(lecturer_id, target_lecturer_id)
    where id = target_course_id;
  else
    delete from public.course_lecturers
    where course_id = target_course_id and lecturer_id = target_lecturer_id;

    update public.courses c
    set lecturer_id = (
      select cl.lecturer_id from public.course_lecturers cl
      where cl.course_id = target_course_id
      limit 1
    )
    where c.id = target_course_id and c.lecturer_id = target_lecturer_id;
  end if;
end;
$$;

grant execute on function public.admin_set_unit_lecturer(uuid, uuid, boolean) to authenticated;

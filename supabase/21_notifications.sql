-- ============================================================================
-- ClassRep Feature Upgrade (2/3): Real-time & Push Notification Inbox
-- Run SECOND in Supabase SQL Editor (after 20_units_materials.sql). Idempotent.
-- ============================================================================

-- 1. Notifications table
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  class_id uuid null references public.classes(id) on delete cascade,
  kind text not null,
  title text not null,
  body text not null default '',
  data jsonb not null default '{}'::jsonb,
  read_at timestamptz null,
  created_at timestamptz not null default now()
);

create index if not exists idx_notifications_user_created
  on public.notifications (user_id, created_at desc);

create index if not exists idx_notifications_user_unread
  on public.notifications (user_id)
  where read_at is null;

alter table public.notifications enable row level security;

drop policy if exists "Users read own notifications" on public.notifications;
create policy "Users read own notifications"
  on public.notifications for select to authenticated
  using (user_id = auth.uid());

drop policy if exists "Users update own notifications" on public.notifications;
create policy "Users update own notifications"
  on public.notifications for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- 2. RPCs: unread_notification_count & mark_notifications_read
create or replace function public.unread_notification_count()
returns integer
language sql
security definer
stable
set search_path = public
as $$
  select count(*)::integer
  from public.notifications
  where user_id = auth.uid()
    and read_at is null;
$$;

grant execute on function public.unread_notification_count() to authenticated;

create or replace function public.mark_notifications_read(
  notification_ids uuid[] default null
)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  v_updated integer := 0;
begin
  if notification_ids is null or array_length(notification_ids, 1) is null then
    update public.notifications
    set read_at = now()
    where user_id = auth.uid()
      and read_at is null;
  else
    update public.notifications
    set read_at = now()
    where user_id = auth.uid()
      and read_at is null
      and id = any(notification_ids);
  end if;
  get diagnostics v_updated = row_count;
  return v_updated;
end;
$$;

grant execute on function public.mark_notifications_read(uuid[]) to authenticated;

-- 3. Internal helper to fan out class or admin notifications cleanly
create or replace function public._notify_class_members(
  p_class_id uuid,
  p_exclude_user_id uuid,
  p_kind text,
  p_title text,
  p_body text,
  p_data jsonb default '{}'::jsonb
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.notifications (user_id, class_id, kind, title, body, data)
  select distinct cm.user_id, p_class_id, p_kind, p_title, coalesce(p_body, ''), coalesce(p_data, '{}'::jsonb)
  from public.class_members cm
  where cm.class_id = p_class_id
    and coalesce(cm.status::text, 'active') = 'active'
    and (p_exclude_user_id is null or cm.user_id <> p_exclude_user_id);
end;
$$;

-- 4. Event triggers for notices, sessions, schedules, materials, polls, prefects, lecturers, units
create or replace function public.trg_notify_notice() returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_kind text := case when coalesce(new.is_urgent, false) then 'notice_urgent' else 'notice' end;
begin
  perform public._notify_class_members(
    new.class_id, new.created_by, v_kind,
    coalesce(new.title, 'New notice'),
    left(coalesce(new.content, ''), 160),
    jsonb_build_object('notice_id', new.id, 'class_id', new.class_id, 'urgent', coalesce(new.is_urgent, false))
  );
  return new;
end $$;

create or replace function public.trg_notify_attendance_session() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if coalesce(new.active, true) then
    perform public._notify_class_members(
      new.class_id, new.created_by, 'attendance_open',
      'Attendance register opened',
      coalesce(nullif(new.session_title, ''), 'Sign your attendance before the code expires.'),
      jsonb_build_object('session_id', new.id, 'class_id', new.class_id)
    );
  end if;
  return new;
end $$;

create or replace function public.trg_notify_schedule() returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public._notify_class_members(
    new.class_id, auth.uid(), 'timetable',
    case when tg_op = 'INSERT' then 'Timetable class added' else 'Timetable updated' end,
    coalesce(new.title, 'Class') || coalesce(' at ' || new.room, ''),
    jsonb_build_object('schedule_id', new.id, 'class_id', new.class_id)
  );
  return new;
end $$;

create or replace function public.trg_notify_material() returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public._notify_class_members(
    new.class_id, new.created_by, 'material',
    'New learning material',
    coalesce(new.title, 'New resource posted'),
    jsonb_build_object('material_id', new.id, 'class_id', new.class_id, 'course_id', new.course_id)
  );
  return new;
end $$;

create or replace function public.trg_notify_poll() returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public._notify_class_members(
    new.class_id, new.created_by, 'poll',
    'New class poll',
    coalesce(new.question, 'Vote now in the class poll.'),
    jsonb_build_object('poll_id', new.id, 'class_id', new.class_id)
  );
  return new;
end $$;

create or replace function public.trg_notify_prefect() returns trigger language plpgsql security definer set search_path = public as $$
declare v_class_name text;
begin
  if coalesce(new.active, true) then
    select name into v_class_name from public.classes where id = new.class_id;
    insert into public.notifications (user_id, class_id, kind, title, body, data)
    values (
      new.user_id, new.class_id, 'prefect_appointed',
      'Appointed Class Prefect',
      'You have been appointed as class prefect for ' || coalesce(v_class_name, 'your class') || '.',
      jsonb_build_object('class_id', new.class_id)
    );
  end if;
  return new;
end $$;

create or replace function public.trg_notify_class_member() returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_role text;
  v_name text;
  v_class text;
begin
  select lower(coalesce(role::text, 'student')) into v_role from public.user_roles where user_id = new.user_id;
  select full_name into v_name from public.profiles where id = new.user_id;
  select name into v_class from public.classes where id = new.class_id;

  if tg_op = 'INSERT' and v_role = 'lecturer' and lower(coalesce(new.status::text, 'active')) = 'pending' then
    -- Notify admins & existing class staff
    insert into public.notifications (user_id, class_id, kind, title, body, data)
    select distinct uid, new.class_id, 'lecturer_join_request',
      'Lecturer requested to join class',
      coalesce(v_name, 'A lecturer') || ' requested to join ' || coalesce(v_class, 'class') || '.',
      jsonb_build_object('class_id', new.class_id, 'user_id', new.user_id)
    from (
      select ur.user_id as uid from public.user_roles ur where lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin')
      union
      select sr.user_id as uid from public.student_representatives sr where sr.class_id = new.class_id and coalesce(sr.active, true) = true
      union
      select c.created_by as uid from public.classes c where c.id = new.class_id and c.created_by is not null
    ) recipients where uid is not null;
  elsif tg_op = 'UPDATE' and lower(coalesce(old.status::text, '')) = 'pending' and lower(coalesce(new.status::text, '')) = 'active' then
    insert into public.notifications (user_id, class_id, kind, title, body, data)
    values (
      new.user_id, new.class_id, 'join_approved',
      'Class join request approved',
      'You can now access ' || coalesce(v_class, 'the class') || '.',
      jsonb_build_object('class_id', new.class_id)
    );
  end if;
  return new;
end $$;

create or replace function public.trg_notify_user_roles() returns trigger language plpgsql security definer set search_path = public as $$
declare v_name text;
begin
  select full_name into v_name from public.profiles where id = new.user_id;
  if tg_op = 'INSERT' and lower(coalesce(new.role::text, '')) = 'lecturer' and not coalesce(new.verified, false) then
    insert into public.notifications (user_id, kind, title, body, data)
    select ur.user_id, 'lecturer_registered',
      'New lecturer awaiting verification',
      coalesce(v_name, 'A lecturer') || ' registered and needs verification.',
      jsonb_build_object('user_id', new.user_id)
    from public.user_roles ur
    where lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin');
  elsif tg_op = 'UPDATE' and lower(coalesce(new.role::text, '')) = 'lecturer'
    and not coalesce(old.verified, false) and coalesce(new.verified, false) then
    insert into public.notifications (user_id, kind, title, body, data)
    values (
      new.user_id, 'lecturer_verified',
      'Lecturer account verified',
      'Your lecturer account has been verified by administration.',
      jsonb_build_object('user_id', new.user_id)
    );
  end if;
  return new;
end $$;

do $$
begin
  if to_regclass('public.notices') is not null then
    drop trigger if exists trg_classrep_notify_notice on public.notices;
    create trigger trg_classrep_notify_notice after insert on public.notices for each row execute function public.trg_notify_notice();
  end if;
  if to_regclass('public.attendance_sessions') is not null then
    drop trigger if exists trg_classrep_notify_session on public.attendance_sessions;
    create trigger trg_classrep_notify_session after insert on public.attendance_sessions for each row execute function public.trg_notify_attendance_session();
  end if;
  if to_regclass('public.schedules') is not null then
    drop trigger if exists trg_classrep_notify_schedule on public.schedules;
    create trigger trg_classrep_notify_schedule after insert or update on public.schedules for each row execute function public.trg_notify_schedule();
  end if;
  if to_regclass('public.materials') is not null then
    drop trigger if exists trg_classrep_notify_material on public.materials;
    create trigger trg_classrep_notify_material after insert on public.materials for each row execute function public.trg_notify_material();
  end if;
  if to_regclass('public.polls') is not null then
    drop trigger if exists trg_classrep_notify_poll on public.polls;
    create trigger trg_classrep_notify_poll after insert on public.polls for each row execute function public.trg_notify_poll();
  end if;
  if to_regclass('public.student_representatives') is not null then
    drop trigger if exists trg_classrep_notify_prefect on public.student_representatives;
    create trigger trg_classrep_notify_prefect after insert or update on public.student_representatives for each row execute function public.trg_notify_prefect();
  end if;
  if to_regclass('public.class_members') is not null then
    drop trigger if exists trg_classrep_notify_member on public.class_members;
    create trigger trg_classrep_notify_member after insert or update on public.class_members for each row execute function public.trg_notify_class_member();
  end if;
  if to_regclass('public.user_roles') is not null then
    drop trigger if exists trg_classrep_notify_role on public.user_roles;
    create trigger trg_classrep_notify_role after insert or update on public.user_roles for each row execute function public.trg_notify_user_roles();
  end if;
end $$;

-- ============================================================================
-- ClassRep Feature Upgrade (3/3): Attendance Tracker, Warnings & Admin Oversight
-- Run THIRD in Supabase SQL Editor (after 20 & 21). Idempotent.
-- ============================================================================

-- 1. Ensure excuse tracking columns on attendance_records
alter table if exists public.attendance_records add column if not exists excused boolean not null default false;
alter table if exists public.attendance_records add column if not exists excuse_reason text null;
alter table if exists public.attendance_records add column if not exists excused_by uuid null references public.profiles(id) on delete set null;
alter table if exists public.attendance_records add column if not exists excused_at timestamptz null;

-- Helper: shared risk classification
create or replace function public._classify_attendance_risk(
  p_percentage numeric,
  p_miss_streak integer,
  p_total_sessions bigint
)
returns text
language sql
immutable
as $$
  select case
    when coalesce(p_total_sessions, 0) = 0 then 'ok'
    when coalesce(p_miss_streak, 0) >= 4 or coalesce(p_percentage, 100) < 60 then 'critical'
    when coalesce(p_miss_streak, 0) = 3 or coalesce(p_percentage, 100) < 75 then 'high'
    when coalesce(p_miss_streak, 0) = 2 or coalesce(p_percentage, 100) < 85 then 'watch'
    else 'ok'
  end;
$$;

-- Helper: detect student id column on attendance_records ('student_id' or 'user_id')
create or replace function public._attendance_record_student_col()
returns text
language sql
stable
set search_path = public
as $$
  select case
    when exists (
      select 1 from information_schema.columns
      where table_schema = 'public' and table_name = 'attendance_records' and column_name = 'student_id'
    ) then 'student_id'
    else 'user_id'
  end;
$$;

-- 2. RPC: class_attendance_risk (Lecturers & Admins ONLY - NOT prefects, NOT students)
create or replace function public.class_attendance_risk(target_class_id uuid)
returns table (
  student_id uuid,
  full_name text,
  admission_number text,
  attended bigint,
  excused bigint,
  total_sessions bigint,
  percentage numeric,
  miss_streak integer,
  last_attended_at timestamptz,
  risk_level text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_col text := public._attendance_record_student_col();
begin
  if not (public.is_admin() or public.is_class_lecturer(target_class_id)) then
    raise exception 'Only lecturers of this class or administrators can view the Attendance Tracker.';
  end if;

  return query execute format($q$
    with valid_sessions as (
      select s.id, coalesce(s.starts_at, s.created_at) as dt
      from public.attendance_sessions s
      where s.class_id = $1
        and lower(coalesce(s.status::text, 'closed')) <> 'voided'
    ),
    class_students as (
      select distinct
        p.id as sid,
        coalesce(nullif(trim(p.full_name), ''), 'Student') as full_name,
        coalesce(nullif(trim(p.admission_number), ''), '-') as admission_number
      from public.class_members cm
      join public.profiles p on p.id = cm.user_id
      left join public.user_roles ur on ur.user_id = p.id
      where cm.class_id = $1
        and coalesce(cm.status::text, 'active') = 'active'
        and lower(coalesce(ur.role::text, 'student')) <> 'lecturer'
    ),
    student_session_matrix as (
      select
        st.sid,
        vs.id as session_id,
        vs.dt,
        (ar.id is not null and not coalesce(ar.excused, false)) as is_present,
        (ar.id is not null and coalesce(ar.excused, false)) as is_excused,
        row_number() over (partition by st.sid order by vs.dt desc) as rn
      from class_students st
      cross join valid_sessions vs
      left join public.attendance_records ar
        on ar.session_id = vs.id and ar.%I = st.sid
    ),
    agg as (
      select
        m.sid,
        count(*) filter (where m.is_present) as attended_cnt,
        count(*) filter (where m.is_excused) as excused_cnt,
        count(m.session_id) as total_cnt,
        max(m.dt) filter (where m.is_present) as last_dt,
        coalesce(
          (
            select min(m2.rn) - 1
            from student_session_matrix m2
            where m2.sid = m.sid and (m2.is_present or m2.is_excused)
          ),
          count(m.session_id)
        )::integer as streak
      from student_session_matrix m
      group by m.sid
    )
    select
      cs.sid as student_id,
      cs.full_name::text,
      cs.admission_number::text,
      coalesce(a.attended_cnt, 0)::bigint as attended,
      coalesce(a.excused_cnt, 0)::bigint as excused,
      coalesce(a.total_cnt, 0)::bigint as total_sessions,
      case
        when coalesce(a.total_cnt, 0) = 0 then 100.0
        else round(((coalesce(a.attended_cnt, 0) + coalesce(a.excused_cnt, 0))::numeric * 100.0) / a.total_cnt::numeric, 1)
      end as percentage,
      coalesce(a.streak, 0)::integer as miss_streak,
      a.last_dt as last_attended_at,
      public._classify_attendance_risk(
        case when coalesce(a.total_cnt, 0) = 0 then 100.0
          else round(((coalesce(a.attended_cnt, 0) + coalesce(a.excused_cnt, 0))::numeric * 100.0) / a.total_cnt::numeric, 1) end,
        coalesce(a.streak, 0)::integer,
        coalesce(a.total_cnt, 0)::bigint
      ) as risk_level
    from class_students cs
    left join agg a on a.sid = cs.sid
    order by
      case public._classify_attendance_risk(
        case when coalesce(a.total_cnt, 0) = 0 then 100.0
          else round(((coalesce(a.attended_cnt, 0) + coalesce(a.excused_cnt, 0))::numeric * 100.0) / a.total_cnt::numeric, 1) end,
        coalesce(a.streak, 0)::integer,
        coalesce(a.total_cnt, 0)::bigint
      )
        when 'critical' then 1
        when 'high' then 2
        when 'watch' then 3
        else 4
      end,
      percentage asc,
      cs.full_name asc;
  $q$, v_col) using target_class_id;
end;
$$;

grant execute on function public.class_attendance_risk(uuid) to authenticated;

-- 3. RPC: student_attendance_detail (Lecturers, Admins, or the Student themselves)
create or replace function public.student_attendance_detail(
  target_class_id uuid,
  target_student_id uuid
)
returns table (
  session_id uuid,
  title text,
  session_type text,
  starts_at timestamptz,
  status text,
  excuse_reason text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_col text := public._attendance_record_student_col();
begin
  if not (public.is_admin() or public.is_class_lecturer(target_class_id) or auth.uid() = target_student_id) then
    raise exception 'Forbidden: only lecturers, admins, or the student can view this timeline.';
  end if;

  return query execute format($q$
    select
      s.id as session_id,
      coalesce(nullif(s.session_title, ''), nullif(s.course_title, ''), 'Class Session')::text as title,
      coalesce(nullif(s.course_code, ''), 'Session')::text as session_type,
      coalesce(s.starts_at, s.created_at) as starts_at,
      case
        when ar.id is null then 'absent'
        when coalesce(ar.excused, false) then 'excused'
        else 'present'
      end::text as status,
      ar.excuse_reason::text
    from public.attendance_sessions s
    left join public.attendance_records ar
      on ar.session_id = s.id and ar.%I = $2
    where s.class_id = $1
      and lower(coalesce(s.status::text, 'closed')) <> 'voided'
    order by coalesce(s.starts_at, s.created_at) desc;
  $q$, v_col) using target_class_id, target_student_id;
end;
$$;

grant execute on function public.student_attendance_detail(uuid, uuid) to authenticated;

-- 4. RPC: set_absence_excused (Lecturers & Admins only)
create or replace function public.set_absence_excused(
  target_session_id uuid,
  target_student_id uuid,
  excused boolean,
  reason text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_class_id uuid;
  v_col text := public._attendance_record_student_col();
  v_rec_id uuid;
begin
  select class_id into v_class_id from public.attendance_sessions where id = target_session_id;
  if v_class_id is null then
    raise exception 'Session not found.';
  end if;

  if not (public.is_admin() or public.is_class_lecturer(v_class_id)) then
    raise exception 'Only lecturers of this class or administrators can excuse absences.';
  end if;

  execute format('select id from public.attendance_records where session_id = $1 and %I = $2 limit 1', v_col)
    into v_rec_id using target_session_id, target_student_id;

  if excused then
    if v_rec_id is not null then
      update public.attendance_records
      set excused = true,
          excuse_reason = nullif(trim(coalesce(reason, '')), ''),
          excused_by = auth.uid(),
          excused_at = now()
      where id = v_rec_id;
    else
      execute format(
        'insert into public.attendance_records (session_id, %I, excused, excuse_reason, excused_by, excused_at, manual) values ($1, $2, true, $3, $4, now(), true)',
        v_col
      ) using target_session_id, target_student_id, nullif(trim(coalesce(reason, '')), ''), auth.uid();
    end if;
  else
    if v_rec_id is not null then
      delete from public.attendance_records
      where id = v_rec_id and coalesce(excused, false) = true;
    end if;
  end if;
end;
$$;

grant execute on function public.set_absence_excused(uuid, uuid, boolean, text) to authenticated;

-- 5. RPC: my_attendance_status (Any student member; strictly returns ONLY auth.uid()'s metrics)
create or replace function public.my_attendance_status(target_class_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_col text := public._attendance_record_student_col();
  v_att bigint := 0;
  v_exc bigint := 0;
  v_tot bigint := 0;
  v_streak integer := 0;
  v_last timestamptz := null;
  v_pct numeric := 100.0;
  v_lvl text := 'ok';
begin
  execute format($q$
    with valid_sessions as (
      select s.id, coalesce(s.starts_at, s.created_at) as dt
      from public.attendance_sessions s
      where s.class_id = $1
        and lower(coalesce(s.status::text, 'closed')) <> 'voided'
    ),
    mine as (
      select
        vs.id,
        vs.dt,
        (ar.id is not null and not coalesce(ar.excused, false)) as is_present,
        (ar.id is not null and coalesce(ar.excused, false)) as is_excused,
        row_number() over (order by vs.dt desc) as rn
      from valid_sessions vs
      left join public.attendance_records ar
        on ar.session_id = vs.id and ar.%I = $2
    )
    select
      count(*) filter (where is_present),
      count(*) filter (where is_excused),
      count(*),
      max(dt) filter (where is_present),
      coalesce(
        (select min(rn) - 1 from mine m2 where m2.is_present or m2.is_excused),
        count(*)
      )::integer
    from mine
  $q$, v_col)
  into v_att, v_exc, v_tot, v_last, v_streak
  using target_class_id, auth.uid();

  if coalesce(v_tot, 0) > 0 then
    v_pct := round(((coalesce(v_att, 0) + coalesce(v_exc, 0))::numeric * 100.0) / v_tot::numeric, 1);
  else
    v_pct := 100.0;
  end if;
  v_lvl := public._classify_attendance_risk(v_pct, v_streak, v_tot);

  return jsonb_build_object(
    'attended', coalesce(v_att, 0),
    'excused', coalesce(v_exc, 0),
    'total_sessions', coalesce(v_tot, 0),
    'percentage', v_pct,
    'miss_streak', coalesce(v_streak, 0),
    'last_attended_at', v_last,
    'risk_level', v_lvl
  );
end;
$$;

grant execute on function public.my_attendance_status(uuid) to authenticated;

-- 6. RPC: admin_class_overview (Admin Class Monitor header stats)
create or replace function public.admin_class_overview(target_class_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_res jsonb;
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  select jsonb_build_object(
    'class_id', c.id,
    'class_name', coalesce(c.name, 'Class'),
    'class_code', coalesce(c.code, ''),
    'join_code', coalesce(c.join_code, ''),
    'year_of_study', c.year_of_study,
    'school_name', coalesce(sc.name, ''),
    'programme_name', coalesce(pr.name, ''),
    'student_count', (select count(*) from public.class_members cm left join public.user_roles ur on ur.user_id = cm.user_id where cm.class_id = c.id and coalesce(cm.status::text, 'active') = 'active' and lower(coalesce(ur.role::text, 'student')) <> 'lecturer'),
    'lecturer_count', (select count(*) from public.class_members cm join public.user_roles ur on ur.user_id = cm.user_id where cm.class_id = c.id and coalesce(cm.status::text, 'active') = 'active' and lower(coalesce(ur.role::text, '')) = 'lecturer'),
    'prefect_count', (select count(*) from public.student_representatives sr where sr.class_id = c.id and coalesce(sr.active, true) = true),
    'unit_count', (select count(*) from public.courses cu where cu.class_id = c.id),
    'material_count', (select count(*) from public.materials m where m.class_id = c.id),
    'notice_count', (select count(*) from public.notices n where n.class_id = c.id),
    'session_count', (select count(*) from public.attendance_sessions s where s.class_id = c.id),
    'poll_count', (select count(*) from public.polls pl where pl.class_id = c.id)
  )
  into v_res
  from public.classes c
  left join public.programmes pr on pr.id = c.programme_id
  left join public.schools sc on sc.id = coalesce(c.school_id, pr.school_id)
  where c.id = target_class_id;

  return coalesce(v_res, '{}'::jsonb);
end;
$$;

grant execute on function public.admin_class_overview(uuid) to authenticated;

-- 7. RPC: admin_attendance_risk_overview (Paginated classes needing attention)
create or replace function public.admin_attendance_risk_overview(
  max_rows integer default 30,
  offset_rows integer default 0
)
returns table (
  class_id uuid,
  class_name text,
  school_name text,
  student_count bigint,
  total_sessions bigint,
  critical_count bigint,
  high_count bigint,
  watch_count bigint,
  ok_count bigint
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
  with class_stats as (
    select
      c.id as cid,
      coalesce(c.name, 'Class')::text as cname,
      coalesce(sc.name, pr.name, '')::text as sname,
      r.risk_level,
      count(distinct r.student_id) as cnt,
      max(r.total_sessions) as ts
    from public.classes c
    left join public.programmes pr on pr.id = c.programme_id
    left join public.schools sc on sc.id = coalesce(c.school_id, pr.school_id)
    left join lateral public.class_attendance_risk(c.id) r on true
    group by c.id, c.name, sc.name, pr.name, r.risk_level
  )
  select
    cs.cid as class_id,
    cs.cname as class_name,
    cs.sname as school_name,
    coalesce(sum(cs.cnt), 0)::bigint as student_count,
    coalesce(max(cs.ts), 0)::bigint as total_sessions,
    coalesce(sum(cs.cnt) filter (where cs.risk_level = 'critical'), 0)::bigint as critical_count,
    coalesce(sum(cs.cnt) filter (where cs.risk_level = 'high'), 0)::bigint as high_count,
    coalesce(sum(cs.cnt) filter (where cs.risk_level = 'watch'), 0)::bigint as watch_count,
    coalesce(sum(cs.cnt) filter (where cs.risk_level = 'ok'), 0)::bigint as ok_count
  from class_stats cs
  group by cs.cid, cs.cname, cs.sname
  having coalesce(max(cs.ts), 0) > 0
  order by critical_count desc, high_count desc, watch_count desc, cs.cname asc
  limit greatest(1, least(coalesce(max_rows, 30), 100))
  offset greatest(0, coalesce(offset_rows, 0));
end;
$$;

grant execute on function public.admin_attendance_risk_overview(integer, integer) to authenticated;

-- 8. RPC: admin_at_risk_students (Filterable across university or per class)
create or replace function public.admin_at_risk_students(
  target_class_id uuid default null,
  min_level text default 'watch',
  search_text text default '',
  max_rows integer default 30,
  offset_rows integer default 0
)
returns table (
  class_id uuid,
  class_name text,
  student_id uuid,
  full_name text,
  admission_number text,
  attended bigint,
  excused bigint,
  total_sessions bigint,
  percentage numeric,
  miss_streak integer,
  last_attended_at timestamptz,
  risk_level text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_min text := lower(coalesce(min_level, 'watch'));
  v_q text := lower(trim(coalesce(search_text, '')));
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  return query
  select
    c.id as class_id,
    coalesce(c.name, 'Class')::text as class_name,
    r.student_id,
    r.full_name,
    r.admission_number,
    r.attended,
    r.excused,
    r.total_sessions,
    r.percentage,
    r.miss_streak,
    r.last_attended_at,
    r.risk_level
  from public.classes c
  join lateral public.class_attendance_risk(c.id) r on true
  where (target_class_id is null or c.id = target_class_id)
    and r.total_sessions > 0
    and (
      (v_min = 'critical' and r.risk_level = 'critical')
      or (v_min = 'high' and r.risk_level in ('critical', 'high'))
      or (v_min = 'watch' and r.risk_level in ('critical', 'high', 'watch'))
      or (v_min = 'all')
    )
    and (
      v_q = ''
      or lower(r.full_name) like '%' || v_q || '%'
      or lower(r.admission_number) like '%' || v_q || '%'
      or lower(coalesce(c.name, '')) like '%' || v_q || '%'
    )
  order by
    case r.risk_level when 'critical' then 1 when 'high' then 2 when 'watch' then 3 else 4 end,
    r.percentage asc,
    r.full_name asc
  limit greatest(1, least(coalesce(max_rows, 30), 100))
  offset greatest(0, coalesce(offset_rows, 0));
end;
$$;

grant execute on function public.admin_at_risk_students(uuid, text, text, integer, integer) to authenticated;

-- 9. RPC: admin_run_attendance_alerts (Batch notification dispatch for High & Critical risk)
create or replace function public.admin_run_attendance_alerts()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_classes integer := 0;
  v_warnings integer := 0;
  v_alerts integer := 0;
  v_cls record;
  v_stu record;
begin
  if not public.is_admin() then
    raise exception 'Forbidden: admin privileges required.';
  end if;

  for v_cls in
    select distinct c.id, c.name
    from public.classes c
    where exists (select 1 from public.attendance_sessions s where s.class_id = c.id)
  loop
    v_classes := v_classes + 1;

    for v_stu in
      select * from public.class_attendance_risk(v_cls.id) r
      where r.risk_level in ('critical', 'high')
    loop
      if to_regclass('public.notifications') is not null then
        insert into public.notifications (user_id, class_id, kind, title, body, data)
        values (
          v_stu.student_id,
          v_cls.id,
          'attendance_warning',
          case when v_stu.risk_level = 'critical' then 'Urgent attendance warning' else 'Attendance warning' end,
          'Your attendance in ' || coalesce(v_cls.name, 'class') || ' is ' || v_stu.percentage || '% (' || v_stu.attended || '/' || v_stu.total_sessions || ').',
          jsonb_build_object('class_id', v_cls.id, 'risk_level', v_stu.risk_level)
        );
        v_warnings := v_warnings + 1;
      end if;
    end loop;

    if to_regclass('public.notifications') is not null and exists (
      select 1 from public.class_attendance_risk(v_cls.id) r where r.risk_level in ('critical', 'high')
    ) then
      insert into public.notifications (user_id, class_id, kind, title, body, data)
      select distinct uid, v_cls.id, 'attendance_alert',
        'Attendance risk alert: ' || coalesce(v_cls.name, 'Class'),
        'Students need attention in ' || coalesce(v_cls.name, 'this class') || '. Tap to review.',
        jsonb_build_object('class_id', v_cls.id)
      from (
        select cm.user_id as uid from public.class_members cm
        join public.user_roles ur on ur.user_id = cm.user_id
        where cm.class_id = v_cls.id and lower(coalesce(ur.role::text, '')) = 'lecturer'
        union
        select ur.user_id as uid from public.user_roles ur
        where lower(coalesce(ur.role::text, '')) in ('admin', 'super_admin', 'superadmin')
      ) targets;
      v_alerts := v_alerts + 1;
    end if;
  end loop;

  return jsonb_build_object(
    'processed_classes', v_classes,
    'warnings_sent', v_warnings,
    'alerts_sent', v_alerts
  );
end;
$$;

grant execute on function public.admin_run_attendance_alerts() to authenticated;

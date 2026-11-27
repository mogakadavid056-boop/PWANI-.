-- CLASSREP - LIVE VIDEO APP BACKGROUNDS (Background001.mp4 & Background002.mp4)
-- Run once in Supabase SQL Editor. Safe to re-run anytime (idempotent).
--
-- Automatically locates "Background001.mp4" (Auth/Welcome/Login/Register) and
-- "Background002.mp4" (all other app screens) in ANY Supabase Storage bucket
-- (public or private, root or inside a folder) and returns their bucket, object path,
-- publicity, and updated_at timestamp so the app streams, caches offline, and
-- auto-updates whenever you replace either video in Supabase without rebuilding the APK.

-- 1. Configure dedicated public bucket for MP4/WebM video backgrounds (up to 50 MB per file)
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'app-backgrounds',
  'app-backgrounds',
  true,
  52428800,
  array['video/mp4', 'video/webm', 'application/octet-stream']
)
on conflict (id) do update
set public = true,
    file_size_limit = 52428800,
    allowed_mime_types = array['video/mp4', 'video/webm', 'application/octet-stream'];

-- 2. Allow the app (both anon before login and authenticated after login)
--    to read uploaded video objects named Background001.mp4* or Background002.mp4* from any bucket.
do $$
begin
  if to_regclass('storage.objects') is not null then
    drop policy if exists "Anyone reads app background images" on storage.objects;
    drop policy if exists "Anyone reads app background videos" on storage.objects;
    create policy "Anyone reads app background videos"
      on storage.objects for select
      to anon, authenticated
      using (
        (
          lower(name) like '%background001%'
          or lower(name) like '%background002%'
        )
        and (
          lower(name) like '%.mp4'
          or lower(name) like '%.webm'
          or lower(name) not like '%.%'
        )
      );
  end if;
exception when insufficient_privilege then
  null;
end $$;

-- 3. Make any bucket containing Background001.mp4 or Background002.mp4 public if possible
do $$
begin
  update storage.buckets b
  set public = true
  where exists (
    select 1 from storage.objects o
    where o.bucket_id = b.id
      and (lower(o.name) like '%background001%' or lower(o.name) like '%background002%')
      and (lower(o.name) like '%.mp4' or lower(o.name) like '%.webm')
  );
exception when insufficient_privilege then
  null;
end $$;

-- 4. Discovery RPC callable by both anon (Welcome/Login/Register) and authenticated users
drop function if exists public.get_app_backgrounds();

create or replace function public.get_app_backgrounds()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, storage
as $$
declare
  bg1 record;
  bg2 record;
begin
  select
    o.bucket_id,
    o.name,
    coalesce(b.public, false) as is_public,
    coalesce(o.updated_at, o.created_at, now()) as updated_at
  into bg1
  from storage.objects o
  join storage.buckets b on b.id = o.bucket_id
  where (
      o.name ~* '(^|/)background[_ -]?0*1(\.(mp4|webm))?$'
      or (lower(o.name) like '%background001%' and (lower(o.name) like '%.mp4' or lower(o.name) like '%.webm'))
    )
  order by
    case when lower(o.name) like '%.mp4' then 0 else 1 end,
    coalesce(o.updated_at, o.created_at) desc nulls last
  limit 1;

  select
    o.bucket_id,
    o.name,
    coalesce(b.public, false) as is_public,
    coalesce(o.updated_at, o.created_at, now()) as updated_at
  into bg2
  from storage.objects o
  join storage.buckets b on b.id = o.bucket_id
  where (
      o.name ~* '(^|/)background[_ -]?0*2(\.(mp4|webm))?$'
      or (lower(o.name) like '%background002%' and (lower(o.name) like '%.mp4' or lower(o.name) like '%.webm'))
    )
  order by
    case when lower(o.name) like '%.mp4' then 0 else 1 end,
    coalesce(o.updated_at, o.created_at) desc nulls last
  limit 1;

  return jsonb_build_object(
    'bg_001', case when bg1.name is not null then jsonb_build_object(
      'bucket', bg1.bucket_id,
      'path', bg1.name,
      'public', bg1.is_public,
      'updated_at', bg1.updated_at
    ) else null end,
    'bg_002', case when bg2.name is not null then jsonb_build_object(
      'bucket', bg2.bucket_id,
      'path', bg2.name,
      'public', bg2.is_public,
      'updated_at', bg2.updated_at
    ) else null end
  );
end;
$$;

grant execute on function public.get_app_backgrounds() to anon, authenticated;

notify pgrst, 'reload schema';

-- ============================================================================
-- ClassRep Push Notifications (Phase 2): FCM Device Tokens
-- Run in Supabase SQL Editor after 21_notifications.sql. Idempotent.
-- ============================================================================

-- 1. Device tokens table (read by send-push Edge Function via service_role)
create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  token text not null unique,
  app_name text not null default 'main' check (app_name in ('main', 'admin')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_device_tokens_user_app
  on public.device_tokens (user_id, app_name);

alter table public.device_tokens enable row level security;

drop policy if exists "Users read own device tokens" on public.device_tokens;
create policy "Users read own device tokens"
  on public.device_tokens for select to authenticated
  using (user_id = auth.uid());

drop policy if exists "Users insert own device tokens" on public.device_tokens;
create policy "Users insert own device tokens"
  on public.device_tokens for insert to authenticated
  with check (user_id = auth.uid());

drop policy if exists "Users update own device tokens" on public.device_tokens;
create policy "Users update own device tokens"
  on public.device_tokens for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists "Users delete own device tokens" on public.device_tokens;
create policy "Users delete own device tokens"
  on public.device_tokens for delete to authenticated
  using (user_id = auth.uid());

-- 2. Register or reassign an FCM token to the signed-in user
create or replace function public.register_device_token(
  device_token text,
  app_name text default 'main'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_token text := nullif(trim(coalesce(device_token, '')), '');
  v_app text := case when lower(trim(coalesce(app_name, 'main'))) = 'admin' then 'admin' else 'main' end;
begin
  if v_uid is null then
    raise exception 'Not authenticated';
  end if;
  if v_token is null then
    raise exception 'Device token is required';
  end if;

  insert into public.device_tokens (user_id, token, app_name, updated_at)
  values (v_uid, v_token, v_app, now())
  on conflict (token) do update
  set user_id = excluded.user_id,
      app_name = excluded.app_name,
      updated_at = now();
end;
$$;

grant execute on function public.register_device_token(text, text) to authenticated;

-- 3. Unregister a device token on logout
create or replace function public.unregister_device_token(
  device_token text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_token text := nullif(trim(coalesce(device_token, '')), '');
begin
  if v_uid is null or v_token is null then
    return;
  end if;

  delete from public.device_tokens
  where token = v_token
    and user_id = v_uid;
end;
$$;

grant execute on function public.unregister_device_token(text) to authenticated;

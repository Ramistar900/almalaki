create table if not exists public.user_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  device_label text,
  platform text not null default 'android' check (platform = 'android'),
  first_seen_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  constraint user_devices_user_device_unique unique (user_id, device_id),
  constraint user_devices_label_length check (device_label is null or char_length(device_label) <= 80)
);

create index if not exists user_devices_user_last_seen_idx
  on public.user_devices (user_id, last_seen_at desc);

alter table public.user_devices enable row level security;

drop policy if exists "Users can read their own devices" on public.user_devices;
create policy "Users can read their own devices"
  on public.user_devices for select
  to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists "Users can register their own devices" on public.user_devices;
create policy "Users can register their own devices"
  on public.user_devices for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update their own devices" on public.user_devices;
create policy "Users can update their own devices"
  on public.user_devices for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can remove their own devices" on public.user_devices;
create policy "Users can remove their own devices"
  on public.user_devices for delete
  to authenticated
  using ((select auth.uid()) = user_id);

revoke all on table public.user_devices from anon;
grant select, insert, update, delete on table public.user_devices to authenticated;

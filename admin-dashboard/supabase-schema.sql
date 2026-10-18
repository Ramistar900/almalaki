
-- ROYAL ADMIN DASHBOARD
-- Public settings contain feature flags only, never secrets.

create table if not exists public.admin_users (
    user_id uuid primary key references auth.users(id) on delete cascade,
    created_at timestamptz not null default now()
);

create table if not exists public.app_remote_settings (
    app_key text primary key
        check (app_key in ('almalaki', 'royaltv')),
    settings jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now()
);

alter table public.admin_users enable row level security;
alter table public.app_remote_settings enable row level security;

-- Only authenticated users listed in admin_users can manage settings.
create or replace function public.is_dashboard_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.admin_users
        where user_id = (select auth.uid())
    );
$$;

revoke all on function public.is_dashboard_admin() from public;
grant execute on function public.is_dashboard_admin()
    to anon, authenticated;

-- The admin allowlist cannot be changed from the public client.
revoke all on public.admin_users from anon, authenticated;

-- App settings contain public feature flags, so apps can read them.
drop policy if exists "Public can read app settings"
    on public.app_remote_settings;

create policy "Public can read app settings"
on public.app_remote_settings
for select
to anon, authenticated
using (true);

drop policy if exists "Dashboard admins can update settings"
    on public.app_remote_settings;

create policy "Dashboard admins can update settings"
on public.app_remote_settings
for update
to authenticated
using (public.is_dashboard_admin())
with check (public.is_dashboard_admin());

drop policy if exists "Dashboard admins can insert settings"
    on public.app_remote_settings;

create policy "Dashboard admins can insert settings"
on public.app_remote_settings
for insert
to authenticated
with check (public.is_dashboard_admin());

insert into public.app_remote_settings (app_key, settings)
values
(
    'almalaki',
    '{"features": {}, "maintenanceMode": false}'::jsonb
),
(
    'royaltv',
    '{"features": {}, "maintenanceMode": false}'::jsonb
)
on conflict (app_key) do nothing;

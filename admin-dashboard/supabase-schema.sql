-- ROYAL ADMIN DASHBOARD
-- Only public feature flags and non-secret settings belong here.

create table if not exists public.admin_users (
    user_id uuid primary key
        references auth.users(id) on delete cascade,
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

-- Check whether the signed-in user is an approved administrator.
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
    to authenticated;

-- Never allow public clients to manage the administrator allowlist.
revoke all on public.admin_users from anon, authenticated;

-- Required table privileges; RLS still controls which rows are accessible.
grant select on public.app_remote_settings to anon, authenticated;
grant insert, update on public.app_remote_settings to authenticated;

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

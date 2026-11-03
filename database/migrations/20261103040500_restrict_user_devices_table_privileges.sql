revoke all on table public.user_devices from public, anon, authenticated, service_role;
grant select, insert, update, delete on table public.user_devices to authenticated;

-- admin_users is an internal registry. The application only calls
-- is_dashboard_admin(), a SECURITY DEFINER boolean check; direct table access
-- by API roles must remain denied.
drop policy if exists "Deny direct API access to admin registry" on public.admin_users;
create policy "Deny direct API access to admin registry"
on public.admin_users
for all
to anon, authenticated
using (false)
with check (false);

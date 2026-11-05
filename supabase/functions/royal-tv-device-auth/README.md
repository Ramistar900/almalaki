# ROYAL TV device authorization

This function is part of the draft device-isolation flow. It must be deployed
only after the matching database migration has been reviewed and applied to a
staging project.

## Required secrets

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY` (server-side only; never ship it in an app)
- `ROYAL_TV_RATE_LIMIT_SALT` (a long random secret used to hash IP addresses)

The function expects Supabase Auth Anonymous Sign-Ins to be enabled for TV
devices. Do not enable this in production until CAPTCHA / Auth abuse controls
and activation rate limits have been reviewed.

## Actions

- `issue_code`: signed-in restaurant owner requests a 10-minute one-time code.
- `redeem`: anonymous TV session redeems the code and becomes bound to one
  restaurant. The database stores the Auth user ID, not a client-supplied token.
- `device_check`: confirms the anonymous account is still paired and active.
- `list_devices`: owner lists registrations for a restaurant they own.
- `revoke_device`: owner revokes one registration in their restaurant.

All owner and device JWTs are validated with Auth `getUser`. All privileged
database operations use the service-role client inside this Edge Function only.
Never log activation codes, access tokens, refresh tokens, or raw IP addresses.

This function is not deployed by this branch commit.

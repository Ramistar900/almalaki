# ROYAL TV device authorization Edge Function

Function name: `royal-tv-device-auth`

This function is a draft backend boundary on branch `royal-tv-device-isolation-draft`. It is not deployed and does not yet make current Realtime command channels private or enforce authorization on every sync/media/advertising path.

## Actions

Send JSON with `action` to the function URL:

- `redeem`: `activation_code`, `device_id` (new UUID), `channel_device_id` (existing `ROYAL-TV-<12 hex>` ID), and `device_name`. Returns a one-time device token.
- `device_check`: `device_token`. Hashes the token server-side and rejects unknown/revoked devices or inactive restaurants. This is a check endpoint only; callers must not treat it as a permanent authorization cache.
- `issue_code`: owner bearer JWT in the Authorization header and `restaurant_id`.
- `list_devices`: owner bearer JWT and `restaurant_id`.
- `revoke_device`: owner bearer JWT and registered `device_id`.

Owner actions call Supabase as the owner, preserving RLS and the SQL ownership RPC. Device-token lookup uses the service-role client only inside the Edge Function. Never copy service-role/secret keys into Android or browser code.

## Deployment prerequisites — not yet completed

1. Review and apply the companion SQL only to a disposable/staging database first.
2. Configure server-side secrets `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` in a non-production Supabase project.
3. Add rate limiting/abuse monitoring for `redeem` and `device_check`; the draft currently does not provide distributed rate limiting.
4. Add automated tests for owner A/B, device A/B, revoked device, inactive restaurant, expired/reused activation code, duplicate ID, and concurrent redemption.
5. Integrate TV token persistence and revocation handling.
6. Route every privileged command/data path through an authorization boundary. The current direct public Realtime channel remains a bypass until a tested private-Realtime/JWT or server-gateway cutover is completed.
7. Validate function deployment and both Android builds in staging before considering production.

## Important limitations

- The endpoint is intentionally configured with gateway JWT verification disabled because device bearer tokens are custom application tokens, not Supabase JWTs. The function must therefore validate each action itself; do not weaken these checks.
- The current implementation is a draft and has not been deployed or runtime-tested.
- Do not change project-wide Realtime public-access settings yet. Existing topics such as `royal-orders` must be tested first.
- Preserve TCP streaming on port `39872` and its existing framing.

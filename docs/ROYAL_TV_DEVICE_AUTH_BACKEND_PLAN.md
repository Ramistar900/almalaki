# ROYAL TV device authorization backend — implementation plan

Status: design preparation only. This document does not claim that device isolation is implemented. No production database or project settings may be changed as part of this plan.

## Decision

Use a server-controlled authorization boundary. A custom device bearer token stored as a hash in `public.royal_tv_devices` is not itself a Supabase Realtime JWT. Do not pass it to Realtime as though it were one, and never put a Supabase service-role/secret key in either Android app.

Preferred first implementation: a Supabase Edge Function gateway for device registration checks and command delivery, with the function's service-role credential held only in server-side secrets. The function must authenticate the owner JWT for owner actions, hash and verify the device token for device actions, check `revoked_at IS NULL`, verify the restaurant remains active, and derive the restaurant/device scope from the database rather than trusting client-supplied IDs. Return only the minimum required result and never log bearer tokens or activation codes.

This gateway approach is the least disruptive initial path because current TV receivers subscribe directly to public Realtime topics. It does not, by itself, make existing direct Realtime topics private. A coordinated private-Realtime migration is a separate stage requiring a device-bound Supabase Auth identity/JWT, RLS policies, `private = true` on both clients, and project-wide public-channel compatibility testing.

## Required endpoint contracts

### Owner actions
- `POST /royal-tv-activation-code`: require a valid owner access token; accept a restaurant ID only as a requested resource and verify `public.is_restaurant_owner(restaurant_id)` plus active restaurant status server-side; return one high-entropy single-use code with a short expiry.
- `GET /royal-tv-devices?restaurant_id=...`: verify owner access to that restaurant; return safe device metadata only, never token hashes.
- `POST /royal-tv-devices/revoke`: verify owner access to the registered device's restaurant; revoke by server-resolved device ID; idempotently reject/handle already-revoked devices.

### TV registration and device actions
- `POST /royal-tv-redeem`: accept activation code, the existing legacy channel ID (`ROYAL-TV-...`), and device name; validate input, redeem exactly once in a transaction, and return a new device token once. Never log the code/token. Rate-limit attempts.
- `POST /royal-tv-device-check`: accept device token over TLS; hash it server-side, locate the device, and reject unknown/revoked devices or inactive restaurants. Return only the minimum authorization state.
- Any command/sync/media/ad endpoint must perform equivalent owner/device and tenant checks before processing or returning data. A single successful check endpoint is not a substitute for securing every path.

## Required hardening before implementation is considered production-ready

1. Add database constraints/transactions for single-use activation and duplicate device identities; serialize activation issuance per owner/restaurant.
2. Keep owner table reads scoped by RLS. Prefer a narrowly scoped revocation RPC rather than client table UPDATE grants, and ensure ownership/identity fields are immutable.
3. Add request size limits, rate limits, abuse monitoring, CORS only where browser clients require it, and generic error responses that do not leak whether a code or device exists.
4. Do not return the device token again after initial redemption. Store it using platform secure storage; never in logs or plain preferences.
5. Define token rotation and revocation semantics. Revocation must be checked on every privileged operation, not only at pairing time.
6. Add tests for owner A targeting owner B, revoked/unknown device, inactive restaurant, expired/reused activation code, concurrent redemption, duplicate legacy ID, invalid token, and rate-limit behavior.
7. Run SQL against disposable/staging Supabase, deploy the function to a non-production environment, and test with separate owner A/B and TV A/B identities before any production change.

## Compatibility rules

- Preserve current legacy channel IDs such as `ROYAL-TV-<12 hex>` as `channel_device_id`; do not cast them to UUID.
- Do not change the TCP video stream protocol: port `39872`, packet format (`Int packet size`, `Long presentationTimeUs`, `Int flags`, then bytes), config magic `0x52545643`, and config flag `Int.MIN_VALUE`.
- Do not globally disable public Realtime access until every topic (including `royal-orders`) is inventoried and tested.
- No production SQL migration, project Auth setting, or Realtime setting is authorized by this design document.

## Acceptance gate

Do not merge PR #20 or claim tenant isolation until the backend boundary is implemented, all privileged paths are covered, the SQL and Edge Function tests pass in staging, both Android modules build, and compatibility tests show that existing devices and the video stream still work.

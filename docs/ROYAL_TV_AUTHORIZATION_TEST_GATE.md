# ROYAL TV authorization endpoint — review and test gate

## Current implementation

Function: `supabase/functions/royal-tv-authorize/index.ts`

Actions:
- `create_challenge`: requires a valid Supabase user token and active owner membership; creates a 5-minute high-entropy pairing code and stores only its SHA-256 digest.
- `pair_device`: consumes a challenge using a conditional update, then issues a random per-device credential once and stores only its digest.
- `send_command`: requires active owner membership, an active restaurant, and an active device belonging to the same restaurant.
- `poll_commands`: requires the per-device credential and claims commands with conditional updates.
- `acknowledge`: accepts only an unexpired claimed command belonging to the authenticated device.
- `revoke_device`: owner-only revocation for a device in the same restaurant.

## Required negative tests before production

- Missing or invalid owner bearer token: reject with 401.
- Authenticated user without active owner membership: reject with 403.
- Suspended restaurant: reject owner actions.
- Invalid, expired, or previously consumed pairing code: reject.
- Two simultaneous attempts to consume one pairing code: exactly one may succeed.
- Duplicate device identifier: reject; do not expose raw device credential on error.
- Missing, malformed, unknown, or revoked device credential: reject polling and acknowledgement.
- Owner attempts to send to a device in another restaurant: reject.
- Device attempts to poll or acknowledge another device's command: return no command / reject.
- Expired command or duplicate acknowledgement: reject.
- Unsupported command type and oversized payload: reject.
- Client roles `anon` and `authenticated` cannot directly select or mutate the three server-only tables.

## Deployment and client integration gates

1. Do not apply the migration or deploy this function to production yet.
2. Configure `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` as server-side Edge Function secrets. Never embed the service-role key in Android.
3. Review the database migration against the actual production schema and confirm table grants.
4. Run the negative tests above against an isolated test project.
5. Update Android clients to store the device credential in Android Keystore and call this endpoint. Until that client migration is complete, the current Realtime command path is not secured by this endpoint.
6. Do not merge PR #12 or claim the vulnerability is fixed until old command delivery is removed/disabled and both clients use the authorized path.

## Known limitations of this first implementation

- This is a first server endpoint implementation, not a production security certification.
- A command becomes `claimed` before the TV confirms it. Recovery/lease semantics for a TV that disconnects after claiming must be designed and tested before production use.
- Operational rate limiting, audit logging, schema/migration execution, end-to-end tests, and Android Keystore integration remain required.
- The endpoint has not been deployed, and no production database has been changed.

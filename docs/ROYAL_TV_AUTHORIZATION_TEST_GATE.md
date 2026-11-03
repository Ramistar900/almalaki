# ROYAL TV authorization endpoint — review and test gate

## Current implementation

Function: `supabase/functions/royal-tv-authorize/index.ts`

Actions:
- `create_challenge`: requires a valid Supabase user token and active owner membership; creates a 5-minute high-entropy pairing code and stores only its SHA-256 digest.
- `pair_device`: consumes a challenge using a conditional update, then issues a random per-device credential once and stores only its digest.
- `send_command`: requires active owner membership, an active restaurant, and an active device belonging to the same restaurant.
- `poll_commands`: requires the per-device credential and claims commands with conditional updates.
- `acknowledge`: accepts only an unexpired claimed command belonging to the authenticated device.
- `list_paired_devices`: owner-only list of paired devices for one authorized restaurant; exposes device metadata only, never credential hashes.
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
- A command claim now has a 30-second lease. An expired claim is returned to the pending queue only for the same device and restaurant, while expired commands are marked expired. Android stores accepted command IDs before acknowledgement to suppress replay after a lost ACK. This remains at-least-once delivery: a process crash between a local side effect and persisting its ID can still produce a duplicate, so command handlers should remain idempotent.
- Operational rate limiting, audit logging, schema/migration execution, end-to-end tests, and Android Keystore integration remain required.
- The endpoint has not been deployed, and no production database has been changed.

## Android TV client progress (2026-10-31)

Implemented on the authorization branch:
- `RoyalTVCredentialStore.kt`: encrypts the per-device credential with a non-exportable Android Keystore AES-GCM key; only ciphertext and IV are stored in private preferences.
- `RoyalTVAuthorizationClient.kt`: calls the pairing, command-poll and acknowledge actions over HTTPS; never embeds a service-role key.
- `RoyalTVSecureCommandPoller.kt`: polls the server queue and acknowledges only after the local command handler accepts a command.
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVOwnerAuthorizationClient.kt`: owner API helper for challenge creation, listing owned restaurants, sending commands and revocation; uses the access token supplied by `AdminContent`.
- `RoyalTVMainActivity.kt`: presents a pairing-code dialog; once paired, it selects server-authenticated polling and does not fall back to legacy Realtime when the stored credential is present but unusable.
- `.github/workflows/royal-tv-auth-android-check.yml`: compile-only Android CI workflow.

Still incomplete:
- The owner pairing screen loads active restaurants for which the signed-in user has an active owner membership, then lets the owner select one before requesting a pairing code. Paired-device listing, revocation UI, and constrained owner controls for updating/showing/hiding the ticker and clearing the screen are implemented on the branch. Android compile CI succeeded for the latest command-control commit `c0a8981bb623a0621740d5ffa2406b5da5b2ca77` and documentation head `5ae59d97d0d49efb248c6fa24bd2cb60950fbe4f` (both workflow runs completed successfully). This is compile-only verification; staging endpoint integration tests and real-device tests remain outstanding.
- The migration and Edge Function are not deployed; the TV pairing request cannot work against production yet.
- The legacy Realtime path remains for unpaired installations only, pending owner-side migration and end-to-end verification.
- Android compile CI succeeded on 2026-11-01 for commits `112ffdedd0e0a6dac6d5229c54e4a8439c811673`, `1334034bd125a8e8dda5867c4d1ec1f02d3073e6`, `c0a8981bb623a0621740d5ffa2406b5da5b2ca77`, and `5ae59d97d0d49efb248c6fa24bd2cb60950fbe4f`. Both Android Kotlin compile tasks completed successfully on the latest head. This is compile-only verification. No device test or negative integration test has been confirmed.
- Server-side stale-claim recovery and Android-side processed-command deduplication are implemented on this branch. The lease helper has a Deno unit test; the Deno test workflow and isolated staging/end-to-end tests still need to pass before production use.

## Integrated build follow-up (2026-11-03)

- Pull request check run `41565546301` used synthetic merge commit `2d49db9328f65e028e0f9cccf9213b887c41b7e1`, whose base parent was `e06af9caa7a0bf728f463397986feae93d884e6a` and whose PR head was `55b3f46156ad7ca6c6081f7fa564d527ed3393fd`.
- In that run, `:royaltv:compileDebugKotlin` completed; `:app:compileDebugKotlin` failed because the merged base snapshot contained a misplaced customer session observer and an incompatible Facebook OAuth provider reference.
- The later current `main` tree (`3ea8605eaa054dea83898995b6f0ed5899b22152`) contains the corresponding corrections: the observer is imported and placed inside the customer account portal where its state is in scope, and Facebook sign-in uses the SDK's `Facebook` provider reference.
- That old failure must not be treated as the result for the later `main` tree. A fresh pull-request integration run is required before reporting a full successful build.
- The Android compile gate remains enabled. No production migration or Edge Function deployment was performed.


## Command delivery recovery (2026-11-03)

- The server claim lease is 30 seconds and is shared with the validation test helper.
- A poll by a paired device first marks its own expired commands as `expired`, then returns only its own stale, unexpired claims to `pending`. Tenant and paired-device filters are applied to both updates.
- Acknowledgement requires the command to remain `claimed`, unexpired, unacknowledged, and inside the current claim lease.
- The Android poller records an accepted command UUID in private app preferences before sending its acknowledgement. If the server must redeliver because the acknowledgement was lost, the client acknowledges the known command without repeating the local action.
- This is bounded at-least-once delivery, not a mathematical exactly-once guarantee. A crash between the local action and the local deduplication write may still repeat an action; display commands must remain safe to repeat.
- No schema migration was needed for this lease implementation, and no production migration or Edge Function deployment was performed.


## Second integrated build check (2026-11-03)

- Follow-up check `41584276366` used merge base `9f8d933771337befdfc4b5be7e86b87ead47ee94`. ROYAL TV Kotlin compilation completed, while Almalaki compilation stopped at `CustomerScreen.kt:529` because that base snapshot's `RoyalCustomerInterface` declaration did not yet expose the `brandingRevision` argument already passed by `CustomerScreen`.
- The current `main` commit `fe475e81d13aa1b3aa7f42f1a6731aa60fe35773` adds the matching `brandingRevision` parameter to `RoyalCustomerInterface`. The failure belongs to the earlier merge snapshot and needs a new integration run against the updated base.
- No change was made to customer UI code from the ROYAL TV branch; this branch continues to keep its changes limited to TV authorization and its review documentation.

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
- Server-side rate limiting and durable audit-event recording are implemented on this review branch, but neither is deployed. The Android Keystore code is present but still needs real-device verification. The Edge Function and migrations remain undeployed, and isolated negative/end-to-end tests are required before production.
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
- Server-side stale-claim recovery and Android-side processed-command deduplication are implemented on this branch. The lease helper Deno unit test passed in run 41584559028; isolated staging/end-to-end tests and real-device verification remain outstanding.

## Integrated build follow-up (2026-11-03)

- Pull request check run `41565546301` used synthetic merge commit `2d49db9328f65e028e0f9cccf9213b887c41b7e1`, whose base parent was `e06af9caa7a0bf728f463397986feae93d884e6a` and whose PR head was `55b3f46156ad7ca6c6081f7fa564d527ed3393fd`.
- In that run, `:royaltv:compileDebugKotlin` completed; `:app:compileDebugKotlin` failed because the merged base snapshot contained a misplaced customer session observer and an incompatible Facebook OAuth provider reference.
- The later current `main` tree (`3ea8605eaa054dea83898995b6f0ed5899b22152`) contains the corresponding corrections: the observer is imported and placed inside the customer account portal where its state is in scope, and Facebook sign-in uses the SDK's `Facebook` provider reference.
- That old failure was superseded by the integration run recorded below; it must not be treated as the result for the later `main` tree.
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
- The current `main` commit `fe475e81d13aa1b3aa7f42f1a6731aa60fe35773` added the matching `brandingRevision` parameter to `RoyalCustomerInterface`. The later integration run below used this updated base and passed.
- No change was made to customer UI code from the ROYAL TV branch; this branch continues to keep its changes limited to TV authorization and its review documentation.

## Latest verified integration and validation (2026-11-03)

- Android integration run 41584559020 completed successfully for PR head a522733ca45105fa0f75bab11904dd1fe299b1dd, using merge commit eb25bdb00ced8e8bc46b3b2b399d194367e67fcf with base parent fe475e81d13aa1b3aa7f42f1a6731aa60fe35773. Both :royaltv:compileDebugKotlin and :app:compileDebugKotlin completed successfully.
- Deno validation workflow run 41584559028 completed successfully: 5 tests passed, 0 failed, including the exact 30-second claim lease boundary.
- RoyalTVAuthorizationClient.kt now uses the same Supabase publishable API key as RoyalSupabaseAuth.kt. The prior TV-client key contained one extra character; it was corrected on this branch. This is a public client key, not a service-role secret.
- The claim lease and persistent client deduplication were compiled successfully in the integration build. This does not replace a live concurrency test against PostgreSQL.
- Rate limiting and durable audit-event storage remain open security work. Do not add or apply a new production migration until the current live migration list and exact next version are verified. No paid Supabase branch was created, no migration was applied, and the Edge Function was not deployed.
- PR #18 remains open and draft. No merge was performed.

## Final review additions (2026-11-03)

- The authorization Edge Function now uses shared validators for restaurant UUIDs, device IDs/names, allowed command types, and payload size in the challenge/pair/list/send/revoke/ack request paths. The Deno workflow runs both `deno check` on the Edge Function and the shared validation unit tests.
- Device polling and acknowledgement now also re-check that the paired device's restaurant is active. A suspended restaurant cannot receive or acknowledge queued secure commands.
- The TV exposes a secure pairing/re-pair action from its Back/exit dialog. It stops both command transports before clearing old local credentials, rotates the device identifier, persists secure-mode selection, and then shows the pairing-code dialog. The owner should revoke the old device from Almalaki before re-pairing. If secure-mode persistence fails, the TV keeps the existing credential and does not clear it.
- Device identifiers now retain the full UUID-derived random value rather than truncating it to 12 hexadecimal characters. Re-pairing gets a fresh identifier so it does not collide with the revoked row's unique device_id.
- Android integration run `41585854568` passed for branch head `55e71dd6abacec926926cf6c860f3bb318879761`; `:royaltv:compileDebugKotlin` and `:app:compileDebugKotlin` both completed successfully against the PR merge snapshot.
- Deno validation run `41585854566` passed on the same head; Edge Function type-check and all 5 validation tests passed. Push-triggered Android run `41585848502` also passed.
- PR #18 remains open and draft. No merge, Supabase database change, production migration, Edge Function deployment, or paid Supabase branch was performed.
- Remaining pre-production work: server-side rate limiting, durable audit-event recording, isolated staging tests for owner/tenant/device authorization and concurrent challenge consumption, real Android TV pairing/revocation/re-pair tests, and a reviewed retirement plan for the legacy Realtime command path. Do not describe the secure endpoint as production deployed until those gates pass.
## SQL privilege review and final test gate (2026-11-03)

- The schema migration explicitly grants the three server-only ROYAL TV tables to `service_role` after revoking `public`, `anon`, and `authenticated`. The atomic pairing RPC remains executable only by `service_role`.
- Static migration tests were added for RLS enablement, client-role revocation, service-role table grants, `SECURITY DEFINER` with an empty search path, one-time unexpired challenge consumption, and service-role-only RPC execution.
- Deno run `41586251163` passed `deno check` and all 7 tests (0 failures). The Deno test runner was granted read permission only to `database/migrations` for these static checks.
- Android integration run `41586251191` completed successfully for branch head `ed58d54a678518c23595dabfe8dcc42a50e9d856`, with latest main commit `fe475e81d13aa1b3aa7f42f1a6731aa60fe35773` as the merge base. The `:royaltv:compileDebugKotlin` and `:app:compileDebugKotlin` tasks succeeded.
- All SQL work described here is source review and static test coverage only. Neither migration was applied to Supabase; no paid branch, deployment, or production data change was made.

## Server-side rate limiting (2026-11-05)

- Added migration `202611050001_royal_tv_rate_limits.sql` with a server-only fixed-window counter table and atomic `consume_royal_tv_rate_limit` RPC. Client roles have no table access or RPC execute permission; only `service_role` can consume counters.
- Added per-action limits for owner restaurant listing, pairing challenge creation, device pairing, paired-device listing, command sending, device polling, acknowledgements, and device revocation. The Edge Function hashes action/subject keys before storing them; raw credentials and user identifiers are not written to the rate-limit table.
- HTTP 429 is returned when a bucket reaches its configured limit. A rate-limit storage/RPC failure fails closed with HTTP 503 rather than silently disabling the limit.
- Added Deno static tests for migration privileges, fixed-window atomic enforcement, and presence of all action gates. The validation workflow now watches database migration changes and grants read access only to the migration and Edge Function source directories required by those tests.
- This is source implementation only. The migration has not been applied, the Edge Function has not been deployed, and live load/concurrency behavior has not been tested. Rate-limit row retention/cleanup still needs an operational policy before production.


## Durable audit events (2026-11-06)

- Added `202611060001_royal_tv_audit_events.sql` with a server-only `royal_tv_audit_events` table. Client roles have no privileges; the Edge Function service role receives INSERT only, with no table SELECT/UPDATE/DELETE grant.
- The table records a request UUID, action/outcome, optional actor/restaurant/paired-device/command UUIDs, and small allow-listed metadata. It deliberately has no columns for pairing codes, device credentials, credential hashes, access tokens, refresh tokens, or command payloads.
- Durable attempt events are written before the security-sensitive mutations `create_challenge`, `pair_device`, `send_command`, `acknowledge`, and `revoke_device`. If the attempt cannot be stored, the operation fails closed with HTTP 503 before the mutation starts.
- A separate success event is appended after a mutation succeeds. If that completion insert fails, the durable attempt remains and the already-completed operation is not misreported as failed; the server emits only a static non-sensitive diagnostic.
- Routine device polling and read-only listing are intentionally not written as individual audit rows to avoid high-volume noise. Rate-limit counters continue to cover request volume.
- Static tests verify table privileges, absence of raw credential fields, and attempt/success instrumentation. This is not a live database test: the migration is not applied, the Edge Function is not deployed, and audit retention/cleanup policy plus isolated staging tests remain required before production.

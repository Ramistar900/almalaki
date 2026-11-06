# ROYAL TV Pairing and Server-Side Command Authorization Design

**Status:** design only — not implemented; no production database changes.
**Related work:** PR #12 (layout sync foundation), currently not safe to merge until command authorization is enforced end-to-end.

## 1. Security finding

The current Android receiver in `royaltv/src/main/java/com/almalaki/royaltv/RoyalTVRealtime.kt` subscribes to a channel named `royal-tv-{deviceId}` using a publishable Supabase key. A device ID and channel name are identifiers, not credentials. The local `RoyalTVCommandAuthorization`, `RoyalTVDeviceRegistry`, and pairing state in the cafe app cannot independently authorize commands received by another Android device; local state is not a server trust boundary.

The publishable key is expected to be public and must not be treated as a secret. Never embed a Supabase `service_role` or secret key in either Android app.

## 2. Required security properties

- Every command is authorized on the server at the moment it is accepted.
- A controller must have a valid authenticated session and an active `owner` membership for the target restaurant.
- A target TV must be paired to that same restaurant, active, and not revoked.
- A device identifier, restaurant ID, join code, or Realtime topic alone never grants access.
- Pairing codes are single-use, short-lived, high-entropy, rate-limited, and stored as hashes.
- Device credentials are unique per device, revocable, stored using Android Keystore-backed protection, and never logged.
- Commands and pending content are tenant-scoped; changing a supplied restaurant/device ID must not cross tenant boundaries.
- Revocation prevents new commands and expires/rejects queued commands according to an explicit policy.
- Local/offline playback remains available. Network control may be unavailable while offline; existing local media/data must not be deleted on revocation or subscription expiry.

## 3. Recommended architecture

Prefer a server-authorized command queue over allowing Android clients to publish commands directly to a predictable Realtime topic.

### Pairing

1. The owner app requests a pairing challenge through an authenticated server endpoint and supplies a restaurant ID.
2. The server verifies the caller's active owner membership using the existing membership/owner checks, then creates a short-lived, single-use challenge. Only a hash is stored; attempts are rate-limited.
3. The TV enters the challenge and submits it to the server. The server consumes it atomically, checks restaurant state, and registers a device record bound to exactly one restaurant.
4. The server returns a unique device credential only once. Store it using Android Keystore-backed encryption; store only a verifier/hash server-side where the chosen token format permits.
5. The owner can list and revoke paired devices. A revoked device cannot obtain commands or pair itself again without a new owner-approved challenge.

The existing `public.restaurant_join_codes` table is for restaurant joining and must not automatically be reused as a TV pairing secret. Reuse its restaurant lookup only if the existing semantics are explicitly appropriate; otherwise create a separate TV-specific challenge flow.

### Command delivery

1. The owner app sends a command to a server endpoint with its normal authenticated user token, target device ID, command type, and validated payload.
2. The server derives the caller from the verified token (never from a caller-supplied user ID), verifies active owner membership, and checks that the target device is active and belongs to the same restaurant.
3. The server validates command type, payload size/schema, and expiry; then writes a tenant-scoped command record with a unique ID and server timestamp.
4. The TV retrieves pending commands through a device-authenticated endpoint. The server verifies the device credential, active pairing, restaurant binding, command expiry, and delivery/acknowledgement state before returning commands.
5. The TV validates the command schema again, deduplicates by command ID, acknowledges completion, and rejects expired or malformed commands.
6. Realtime may later be used only as a wake-up signal. It must not carry trusted commands directly unless private-channel authorization has been independently designed and tested. Polling/queue retrieval remains the authorization boundary.

## 4. Server data model proposal

Names below are illustrative and must be reconciled with the current schema before a migration is written.

- `royal_tv_private.paired_devices`: device UUID, restaurant UUID, display name, role limited to TV/display, credential verifier, created/last-seen timestamps, revoked timestamp, and metadata needed for audit. Do not expose this table through the public Data API.
- `royal_tv_private.tv_pairing_challenges`: challenge hash, restaurant UUID, creator user UUID, expiry, consumed timestamp, failed-attempt count, and creation timestamp. Use atomic consume semantics and cleanup expired challenges.
- `royal_tv_private.tv_commands`: command UUID, restaurant UUID, target device UUID, command type, validated JSON payload, created/expiry timestamps, delivery/acknowledgement timestamps, and status. Enforce foreign keys and indexes for pending commands.

Enable RLS as defense in depth, deny direct client access to private tables, and expose narrowly scoped server endpoints. Do not grant broad table access merely to make the Android clients work. Any privileged function must use a fixed empty search path, fully qualify object names, verify caller identity/authorization, and have explicit EXECUTE grants.

## 5. Implementation sequence

1. Inspect existing migrations, pairing classes, device registry, connection manager, and current Supabase SDK versions.
2. Agree on the endpoint and credential lifecycle before writing SQL or Android changes.
3. Implement the server-side pairing/command endpoints and migration in a dedicated branch. Do not apply them to production during development.
4. Add negative tests: unauthenticated caller, non-owner, inactive membership, cross-restaurant target, invalid/expired/replayed challenge, revoked device, invalid device credential, expired command, oversized/unknown payload, and duplicate acknowledgement.
5. Update the owner app and TV receiver to use the new server flow; remove direct trusted command broadcast.
6. Run database security advisors and automated tests, build both Android variants, and inspect the PR diff.
7. Keep PR #12 unmerged until server authorization is implemented and tests demonstrate both allowed and denied cases. Do not deploy a migration or Edge Function without explicit approval.

## 6. Acceptance criteria

- A user from restaurant A cannot send to or read commands for a TV belonging to restaurant B, even by guessing IDs or editing requests.
- A revoked or unpaired TV receives no commands.
- Replaying a pairing challenge or command acknowledgement does not grant access or execute a command twice.
- No privileged server key is present in APK source, resources, build configuration, logs, or repository history.
- Local playback still works with no network connection.
- Build success alone is not considered proof of authorization correctness.

# ROYAL TV device isolation — review notes

## Status
Draft design only. This change has not been applied to the live Supabase database and does not claim complete tenant isolation.

## Scope
- Adds a server-side device registry linked to `restaurants.id`.
- Owner-only listing and revocation of devices via row-level security.
- Activation codes are random 256-bit values, expire after 10 minutes, are stored only as SHA-256 hashes, and are single-use.
- Device bearer tokens are random 256-bit values, returned once at registration, and stored only as hashes.
- Does not change the existing TCP video stream on port 39872 or its packet format.

## Required before merge/release
1. Integrate the owner UI to issue/display an activation code.
2. Integrate ROYAL TV pairing to redeem the code, persist the device token safely, and handle revoked devices.
3. Add server-side token validation to every TV sync/command/advertisement endpoint before returning or accepting tenant data. Do not trust a client-supplied `restaurant_id`.
4. Verify all realtime channels and storage paths are tenant-scoped.
5. Add tests for owner A vs owner B, expired/reused code, revoked device, duplicate device UUID, suspended restaurant, and unauthenticated owner-code issuance.
6. Review Supabase advisor findings separately: public SECURITY DEFINER RPCs are intentional only where their input/output is tightly bounded; `admin_users` has RLS enabled without policies; leaked-password protection is disabled.
7. Apply only after SQL review and staging validation. This migration has not been run against production.

## Review caveat
The code is an initial registration primitive, not sufficient by itself for end-to-end authorization. Token verification and rate limiting at the API boundary are still required.

## Free static audit findings — 2026-10-31

### Finding A: Realtime topic is not bound to a server-verified device identity

Reviewed:
- `royaltv/src/main/java/com/almalaki/royaltv/RoyalTVRealtime.kt`
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVRealtime.kt`

Both clients build a topic from the caller-provided `deviceId` (`royal-tv-{deviceId}`) and connect using only the public/publishable project key. The code shown does not attach the device token from `royal_tv_devices`, and does not authenticate topic access against the device registry. Topic names are identifiers, not authorization.

This is a high-priority authorization gap in the client implementation. Actual exploitability depends on the project's Realtime channel access configuration, which must be verified before making a definitive exposure claim. Do not assume a random-looking device ID provides security.

### Finding B: Realtime authorization policies were not found in the live database

A read-only query confirmed that `realtime.messages` exists with RLS enabled, but `pg_policies` returned no policies for that relation. No database changes were made. This alone does not establish the effective public/private channel configuration; check the Realtime settings and test public and private channel access in an isolated test environment.

### Finding C: Existing device IDs do not match the draft registration function's UUID contract

Reviewed:
- `royaltv/src/main/java/com/almalaki/royaltv/RoyalTVDeviceIdentity.kt`
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVDeviceIdentity.kt`
- `database/migrations/202610310001_royal_tv_device_isolation.sql`

Both current client identity implementations create a prefixed short string such as `ROYAL-TV-<12 hex characters>`. The draft SQL table and `redeem_royal_tv_activation_code(p_activation_code text, p_device_id uuid, p_device_name text)` require a UUID. Passing the current stored ID directly will fail UUID parsing; silently replacing IDs also risks splitting existing device identity/channel mappings.

**Blocker:** choose and document a compatible identity migration before integrating pairing. Safest design is to introduce a separate stable UUID registration ID (or deliberately migrate existing IDs with explicit compatibility handling), while preserving the existing display ID and current Realtime/TCP behavior until a coordinated cutover is tested. Do not merge or apply the current migration as-is.

### Required remediation before declaring device isolation complete

1. Set device command channels to private and require authenticated access; do not rely on topic secrecy.
2. Add narrow Realtime policies that validate the caller against a server-side device/owner authorization record, with correct topic-to-device binding. Do not accept a client-supplied restaurant ID as proof of ownership.
3. Add a server-side authorization path for sending commands so a sender must own/control the target device. A public publishable key is not a device credential.
4. Integrate one-time activation and secure token persistence/verification, including revocation checks. Never send the raw token in logs.
5. Add isolated automated checks for unauthorized subscribe, unauthorized broadcast, owner A targeting owner B, revoked devices, expired/reused activation codes, and current-ID-to-registration-ID compatibility.
6. Keep these changes as draft until code paths, Realtime settings/policies, and staging tests are reviewed. Do not apply experimental DDL to production.

### Test status

- Static source review: performed for the two Realtime clients and both device identity implementations.
- Read-only production catalog query: performed; RLS is enabled on `realtime.messages`, but no policies were returned.
- Read-only catalog scan: no existing public function names matching ROYAL TV/device/command/ad/sync were returned; only `app_remote_settings` matched the table-name scan. This does not replace a full application source audit.
- Runtime authorization tests: not run.
- Production schema/data changes: none.
- Merge to `main`: not performed.


## Follow-up implementation — preserve existing channel identity

The draft migration now separates two identifiers:
- `device_id uuid`: a server-side registration UUID used as the registry primary key.
- `channel_device_id text`: the existing client-generated `ROYAL-TV-...` identity used in current Realtime topic names.

The draft `redeem_royal_tv_activation_code` signature now accepts both values and stores the legacy channel identity alongside the registration UUID. This avoids trying to cast the existing prefixed ID to UUID and avoids silently changing current topic names as part of database registration.

### Compatibility status and remaining blockers
- This is a schema/API design adjustment on the draft branch only; neither Android client calls the revised RPC yet.
- No device-token persistence, token verification, revocation enforcement, or private-channel authorization has been integrated into the current clients.
- The revised migration has been reviewed by source-level string/shape checks only. It has not been executed by PostgreSQL, compiled, or tested against staging.
- The public/anonymous redemption RPC still requires abuse/rate-limit design before production use. A high-entropy one-time code is not a substitute for rate limiting.
- Do not merge or apply this migration until SQL syntax, function privileges, concurrent redemption, existing-ID compatibility, and end-to-end authorization tests pass.


## Follow-up security review — 2026-11-01

### Finding D: Private Realtime requires both server policy and private client configuration

Current Supabase Realtime documentation confirms that channel access is evaluated using RLS policies on `realtime.messages`, the caller's JWT/headers, and `realtime.topic()`. A private channel must be instantiated with `private: true`, and the project's `Allow public access` setting must be disabled to enforce private-only channels. The current Android clients use a publishable key and a topic derived from the legacy device ID; the reviewed client code does not configure a private channel or provide a device-authenticated JWT. Adding a policy alone would not complete the change.

Reference: https://supabase.com/docs/guides/realtime/authorization

### Finding E: A custom device token is not automatically a Realtime identity

The proposed `device_token_hash` is a custom bearer credential stored in the application database. Realtime authorization evaluates the JWT and request context; it does not automatically look up this custom token hash. Before private Realtime can be enabled without breaking existing clients, choose and test one server-controlled bridge:
- authenticate the device through a server endpoint that validates token hash and revocation, then issue a short-lived, narrowly scoped JWT accepted by Realtime; or
- route command publish/receive through a server-controlled gateway that validates the device token and does not expose cross-tenant messages.

Do not put a Supabase service-role/secret key in either Android app. Do not treat a client-supplied restaurant ID or topic string as proof of ownership.

### Finding F: Remaining migration hardening items

- Activation-code issuance has no database-enforced serialization per owner/restaurant; two simultaneous issuance requests could leave more than one unconsumed code. Consider a transaction-scoped advisory lock or a uniqueness invariant after validating the desired multi-device pairing behavior.
- The redemption RPC is callable by `anon`; the 256-bit code has high entropy, but production still needs abuse controls/monitoring and a deliberate policy for repeated invalid attempts.
- The current migration has not been executed by PostgreSQL. Function grants, extension schema availability (`extensions.gen_random_bytes` / `extensions.digest`), table ownership assumptions, RLS behavior, and concurrent redemption must be tested in an isolated staging database.
- The owner update policy allows updates only when `revoked_at` is non-null, but the intended revocation semantics should be tested for already-revoked rows and should prevent any accidental change to immutable ownership/identity fields.
- Device registration records are not yet consumed by any existing command/sync/ad delivery path. Until each path checks authorization and revocation server-side, this schema must not be described as device isolation.

### Verification status after this review

- Supabase Realtime authorization documentation reviewed.
- Source-level inspection reconfirmed the legacy `ROYAL-TV-...` identity in both clients.
- No migration executed; no production database changed; no Android build or runtime authorization test performed.
- PR #20 remains a draft and must not be merged yet.


## Full command-path audit — 2026-11-01

### Confirmed cloud command path

The application-side send path currently found in source is:

`RoyalTVTickerControlScreen`
→ `RoyalTVConnectionManager.sendCommand`
→ `RoyalTVRealtime.sendCommand`
→ channel `royal-tv-{targetDeviceId}`
→ Broadcast event `royal_tv_command`.

Files:
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVTickerControlScreen.kt`
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVConnectionManager.kt`
- `app/src/main/java/com/almalaki/cafe/royaltv/RoyalTVRealtime.kt`
- `royaltv/src/main/java/com/almalaki/royaltv/RoyalTVRealtime.kt`
- `royaltv/src/main/java/com/almalaki/royaltv/RoyalTVMainActivity.kt`

### Confirmed authorization gaps

1. The owner sender creates a fresh Supabase client with only the publishable key. It does not call `realtime.setAuth(ownerAccessToken)`; therefore the logged-in owner JWT is not attached to the channel join/publish.
2. The receiver also creates a client with only the publishable key. It does not use a per-device Auth session, does not bind an Auth user to a registered device, and does not instantiate a private channel.
3. The owner navigation already has an `accessToken` in `AdminScreen` and passes it into `AdminContent`, but `RoyalTVControlScreen`, `RoyalTVTickerControlScreen`, and `RoyalTVConnectionManager` currently do not receive that token.
4. `RoyalTVCommandAuthorization` relies on local `RoyalTVDeviceRegistry` and `RoyalTVPairingManager` state. Those local preferences are not a server-side tenant boundary and cannot authorize a Realtime write.
5. The only confirmed `RoyalTVConnectionManager` UI construction found is the ticker control screen. The sender UI also includes local/Wi-Fi Direct functionality; this audit does not establish that every content, ad, order, YouTube, storage, or media path is routed through a centrally authorized remote-command gateway.
6. The standalone ROYAL TV main activity subscribes on startup using the legacy display ID. It has no activation screen or server registration check yet.
7. The separate `royal-orders` Postgres Changes channel in `app/src/main/java/com/almalaki/cafe/SupabaseRealtime.kt` is currently created without `private = true`. A project-wide switch that disables public Realtime access must not be flipped until this and any other non-ROYAL-TV channels are assessed for compatibility.

### Database and SQL review updates

- Read-only production catalog inspection confirms `pgcrypto` is installed in schema `extensions`, and `public.is_restaurant_owner(uuid)` exists.
- The live `realtime.messages` policy query returned no rows at the time of inspection.
- The draft SQL still needs a safe device-auth binding model, device-only read policy, owner-only publish policy, an owner revocation RPC, and concurrency-safe activation issuance. Realtime policy subqueries must not depend on client-visible device-table columns; a narrowly scoped non-exposed SECURITY DEFINER helper is one candidate, subject to review.
- The project-level `Allow public access` setting is not a SQL migration setting. It must be changed only after all existing Realtime topics have been inventoried and staged.

### Important configuration prerequisite

A device-specific anonymous Auth session is a viable way to give each TV a JWT identity without embedding a service-role key. However, Supabase Anonymous Sign-Ins must be enabled for the project, and the TV client must persist/refresh that session safely. This project setting has not been verified or changed. Supabase recommends CAPTCHA/abuse controls for public anonymous sign-in; the production abuse-control design must be decided before enabling it.

### Stage gate — do not merge yet

The current PR remains design/migration-only and is not end-to-end isolation. Do not merge or apply until all of the following are committed and verified:
- Owner UI issues activation codes and lists/revokes registered devices, passing the actual signed-in owner JWT.
- TV activation creates/uses a unique device Auth identity, redeems a one-time code, stores the session securely, and refuses service if revoked.
- Both Realtime clients use `private = true`; owner publisher supplies a valid owner JWT; receiver uses only its bound device JWT.
- Policies deny owner A publishing to owner B's device, device A subscribing to device B's topic, and any device publishing commands.
- All remote command, advertisement, media, storage, and sync endpoints are inventoried and independently tenant-authorized. Do not infer coverage from the ticker path.
- The project-wide Realtime public-access setting is tested against `royal-orders` and other channel users before changing it.
- SQL is executed against disposable/staging Supabase, concurrency and authorization tests pass, and both Android modules build successfully.
- The existing TCP stream remains unchanged: port 39872 and its current packet/config framing.

### Execution status after full-path audit

- Source-path inventory: performed for the currently indexed repository source.
- Production catalog inspection: read-only only.
- Supabase Realtime and anonymous Auth documentation: reviewed.
- Branch SQL write attempt: blocked by the platform's safety checks; no migration contents were changed by that attempt.
- Android source integration: not changed in this audit pass.
- SQL/runtime tests: not run.
- Production project settings/data: not changed.
- `main`: not changed; PR remains draft.

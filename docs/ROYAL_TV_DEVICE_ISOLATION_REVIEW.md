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

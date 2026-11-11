# ROYAL TV tenant-bound registration and Realtime authorization

Status: implementation and automated regression coverage are on the draft branch `feature/royal-tv-device-registration-uuid`. Production migration, Edge Function deployment, Auth changes, and Realtime project settings remain untouched. Keep PR #40 as a draft until the latest commit passes CI and the staging/physical-device gates below.

## What this stage implements

- A durable registration UUID per TV, separate from the legacy channel/display ID. Existing `ROYAL-TV-001` numeric IDs and 12-hex IDs remain unchanged.
- A cryptographically random 256-bit activation code, hashed in the database, valid for 10 minutes and redeemable once for one active restaurant.
- One Supabase Auth identity per TV. The trusted Edge Function creates the identity, atomically redeems the restaurant code through a server-only RPC, and returns a session to the TV over TLS.
- Encrypted local persistence of the device access/refresh tokens using Android Keystore AES-GCM. The TV refreshes its own session and does not fall back to expired credentials.
- A private server registry mapping Auth identity, registration UUID, channel display ID, restaurant ID, owner, and revocation state.
- Owner-only code issuance, device listing, and device revocation. Each list/revoke request revalidates the caller's signed-in user against active owner membership server-side.
- Revocation marks the registry row and bans the device Auth user. When the TV's refresh is rejected, it clears its local session and returns to the activation screen.

## Realtime channel authorization

All existing app Realtime topics discovered in the repository are included in the draft migration and updated Android channel configuration:

| Topic | Allowed joiner | Access |
|---|---|---|
| `royal-tv-ROYAL-TV-...` | Registered TV identity for that exact device; active owner of its restaurant for sending | TV can receive only; restaurant owner can send only to its own active TV |
| `royal-orders` | Signed-in active restaurant owner | Read-only order notifications; underlying `orders` RLS continues to scope rows to the current owner/customer |
| `restaurant-interface-{restaurant_uuid}` | Anonymous or signed-in customer, for an active restaurant whose UUID matches the topic | Read-only public interface refresh for that restaurant |

The migration uses restrictive guards on reserved topic namespaces as well as permissive allow policies. This is intentional: an older generic `USING (true)` or `WITH CHECK (true)` policy must not override the tenant checks. The PostgreSQL regression fixture creates simulated broad legacy policies and verifies that cross-restaurant TV joins/sends are still denied, while the order and interface topics retain only their intended readers.

Every Android channel creation for these topics sets `private = true`. No client can publish arbitrary broadcasts to `royal-orders` or `restaurant-interface-...`; those topics are for Postgres Changes notifications only.

## Revocation semantics

Supabase caches channel authorization for an already-open WebSocket until the JWT expires or is refreshed. Therefore, a TV already connected at the moment of revocation may remain joined briefly. The Almalaki sender creates a fresh authenticated private channel for each command, so the server rechecks the device registration before every new send. The TV attempts session refresh every five minutes; once the revoked Auth user is banned, refresh fails, the receiver closes, the encrypted local session is cleared, and the activation screen returns.

## Automated verification

The latest fully green run before the final revoked-session UI callback change verified:

- Almalaki Debug APK build.
- Standalone ROYAL TV Debug APK build.
- Existing strict owner/restaurant-scope SQL suite.
- ROYAL TV registration, one-use/expiry, cross-tenant, read/write, revocation, anonymous-denial, owner-order-topic, and customer-interface-topic SQL regression fixture.
- Deno type-check of `supabase/functions/royal-tv-device-auth/index.ts`.

The follow-up change that returns a revoked TV to its activation screen must pass CI on the resulting PR head before this work is considered validated. A successful build or disposable PostgreSQL test is not a substitute for the staging and physical-device checks below.

## Remaining gates before production

1. Run CI on the latest PR head and inspect both Android APK builds, PostgreSQL suites, and Deno type-check.
2. Deploy the migration and Edge Function only to an isolated Supabase staging project first. Verify actual Auth session creation/refresh, invalid/expired/reused activation codes, owner A versus owner B, TV A versus TV B, device publish denial, device listing, and revoke.
3. Test actual Realtime joins against the staging Realtime service with two restaurants and two registered TVs. Confirm row-level owner scoping for `royal-orders`, and confirm a customer can only join the matching active restaurant interface topic.
4. Add/confirm invalid-code abuse controls, monitoring, and operational alerting before public rollout. The activation token has 256 bits of entropy and is one-use, but that does not replace monitoring.
5. On physical devices, test pairing, app restart, session refresh, revoke/re-pair, and preservation of the existing Wi-Fi Direct/H.264 transport (including port 39872). The SQL fixture simulates two restaurants and TVs; it does not prove two physical televisions work end to end.
6. Only after the migration and both updated clients are deployed and verified should the Supabase Realtime setting **Allow public access to channels** be disabled. Supabase disconnects existing clients when this setting changes. Do not toggle it from this PR or before the production rollout is coordinated.

## Deployment boundary

- Migration exists only as a repository file; it has not been applied to production.
- `royal-tv-device-auth` exists only in the repository; it has not been deployed.
- `verify_jwt = false` is required because first-time TV redemption occurs before the TV has a JWT. The function manually validates owner JWTs for owner actions; redemption is protected by the high-entropy one-use code and the database's atomic redeem RPC.
- The service-role key is used only by the Edge Function and is never embedded in either Android app.
- No production Auth settings, Realtime settings, or production data have been changed.
- No merge to `main` or release has been made.

## Compatibility notes

- Receiver and sender retain the exact legacy display ID in the channel topic; the registration UUID is never substituted into that topic.
- The receiver starts listening only when an encrypted device session exists. An invalid or revoked session never falls back to a public channel.
- The owner sender refuses to send without the owner's JWT and creates a fresh private channel per command.
- The existing customer-facing restaurant interface remains readable for active restaurants; its private topic authorization is tied to the restaurant UUID in the topic.

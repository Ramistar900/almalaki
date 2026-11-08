# ROYAL TV tenant-bound registration and Realtime authorization

Status: client integration is implemented on the draft branch `feature/royal-tv-device-registration-uuid`; CI verification and real-device/staging validation are pending. No production migration, Edge Function deployment, or Realtime project setting has been changed.

## What this stage adds

- A separate server-side registration UUID for each TV, while preserving the saved channel/display identity, including numeric IDs such as `ROYAL-TV-001` and existing 12-hex IDs.
- A one-use, 256-bit activation code bound to one active restaurant, expiring after 10 minutes.
- A separate Supabase Auth identity for each registered TV. The Edge Function creates the identity and returns its session once; the TV app encrypts the session at rest with Android Keystore AES-GCM and refreshes it through the Auth REST endpoint.
- A server registry linking `auth_user_id`, device UUID, legacy channel ID, and restaurant ID.
- Realtime RLS policies: a TV identity may receive broadcasts only on its own registered topic; a restaurant owner may send only to an active device belonging to a restaurant they own. Device identities cannot publish.
- Owner-only server actions to list devices and revoke a device.

## Channel compatibility

The topic keeps the exact saved display ID, including legacy numeric IDs such as `ROYAL-TV-001` and existing 12-hex IDs. Registration uses a separate UUID and does not rename the channel. Both Android clients now set `private = true` when creating the channel. The owner sender attaches its current owner JWT; the TV receiver sets the registered device JWT before joining.

## Revocation behavior

Revocation marks the registry row, bans the device Auth user to prevent future token refresh, and makes new Realtime channel joins fail. Supabase documents that channel authorization is cached for an existing WebSocket connection until its JWT expires or a new JWT is sent. Therefore, an already-open receiver socket can remain joined temporarily. The owner sender must create a fresh authenticated channel for each command (as the current sender already does) so the revoked device cannot receive subsequent commands after the server-side status check. The UI must display revocation as complete only after both the database update and Auth ban succeed.

## Safety gates still open

1. Run CI after the latest client changes: Almalaki Debug build, standalone ROYAL TV Debug build, tenant-scope SQL tests, ROYAL TV Realtime SQL test, and Deno Edge Function type-check.
2. Test the SQL migration and Edge Function in a disposable/staging Supabase project. Verify owner A cannot send to owner B, TV A cannot subscribe to TV B, TV identities cannot publish, and revoked/expired/reused codes fail.
3. Review and add abuse controls/monitoring for invalid pairing attempts before production.
4. Inventory every Realtime topic, especially `royal-orders`, before disabling project-wide public channel access. Do not toggle that project setting as part of this PR.
5. On real devices, test pairing, restart/session refresh, revocation behavior, two restaurants/two TVs, and confirm Wi-Fi Direct/H.264 transport (including port 39872) is unchanged.

## Deployment boundary

- The migration is a repository file only; it has not been applied.
- The Edge Function is a repository file only; it has not been deployed.
- `verify_jwt = false` is required because TV redemption starts before a device JWT exists. The function manually validates owner JWTs for all owner actions; the redeem action is protected by the high-entropy one-use code.
- No service-role key is embedded in Android.
- No changes to `main`, production Auth settings, Realtime settings, or production data.


## Client-side implementation notes

- `RoyalTVDeviceAuthManager` intentionally uses the Auth REST refresh endpoint rather than raising the standalone TV module's Android minimum SDK by adding another Supabase Auth dependency. Session material is encrypted at rest; a failed refresh never falls back to an expired access token.
- The TV receiver starts only when an encrypted device session exists. A missing or unusable session never falls back to a public channel; re-pairing may be required if the device session has been revoked or is no longer refreshable.
- The Almalaki sender's Realtime method refuses to send without a non-empty owner JWT and always marks the channel private.
- The Almalaki `royal-orders` topic remains outside this ROYAL TV change; it must be reviewed separately before any project-wide Realtime setting is changed.


## Legacy display-ID compatibility

- The receiver continues using `RoyalTVDisplayIdAllocator.getDisplayId()` for the channel topic, exactly as before this work.
- The registration UUID is a separate UUID and is never used as the channel topic.
- The migration and Edge Function accept the currently generated numeric display IDs and the existing 12-hex `RoyalTVDeviceIdentity` IDs. This avoids breaking existing sender configurations and TVs already paired under numeric display IDs.

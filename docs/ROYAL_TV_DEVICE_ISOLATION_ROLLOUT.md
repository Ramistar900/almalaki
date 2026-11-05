# ROYAL TV device isolation — rollout and verification

## Current branch status

This implementation is staged on `royal-tv-device-auth-isolation-20261105`.
It has not been merged into `main`. The database migration and Edge Function
have not been applied or deployed to the production Supabase project.

The local display ID (for example `ROYAL-TV-001`) remains unchanged for
display and compatibility. It is not an authorization identity. The server
assigns a UUID to every registered TV and returns a private topic in this form:

```text
royal-tv:<server-device-uuid>
```

The TCP video transport is intentionally unchanged: port 39872, existing
packet framing, presentation timestamp, flags, config magic, and config flag.

## Required staging setup — do not skip

1. Create or select a non-production Supabase staging project with the same
   `restaurants` and `restaurant_members` schema and the
   `private.is_restaurant_owner(uuid)` helper.
2. Confirm Supabase Auth Anonymous Sign-Ins are enabled for the staging project.
   Apply CAPTCHA or equivalent Auth abuse controls before exposing this flow.
3. Add the Edge Function secret `ROYAL_TV_RATE_LIMIT_SALT` with a long random
   secret. The function also requires the standard Supabase URL, publishable
   key, and service-role key; the service-role key must remain server-side.
4. Apply
   `database/migrations/202611050001_royal_tv_device_auth_isolation.sql`
   to staging and check the migration output.
5. Deploy `supabase/functions/royal-tv-device-auth` to staging.
6. In Realtime project settings, turn off **Allow public access** so private
   channel authorization is enforced globally.
7. Do not enable production Anonymous Sign-Ins or apply this migration to
   production until the staging acceptance checks below pass.

## Staging acceptance checks

Use two owner accounts, two active restaurants (A and B), and at least two TV
devices with distinct anonymous Auth sessions.

- Owner A can issue a short-lived, one-time activation code only for restaurant A.
- Owner B can issue a code only for restaurant B.
- A code cannot be redeemed twice, after expiry, or by an already-registered
  device account.
- TV A is registered to restaurant A and receives only the
  `royal-tv:<A-device-uuid>` private topic.
- TV B is registered to restaurant B and receives only the
  `royal-tv:<B-device-uuid>` private topic.
- Owner A can list and revoke devices only in restaurant A; the same request
  must fail for restaurant B.
- Owner A cannot publish a broadcast to TV B's private topic.
- TV A cannot subscribe to TV B's private topic.
- Revoke TV A, then verify the owner cannot publish new commands to its topic
  and TV A stops listening after its authorization check.
- Deactivate restaurant A and verify TV A fails its next authorization check.
- A revoked TV can be paired again only with a newly issued one-time code.
- A legacy value such as `ROYAL-TV-001` is rejected as a Realtime topic.
- Verify ticker commands can be queued offline with a server-issued private
  topic and are not replayed to legacy display-ID/public topics.
- Confirm normal order, media, YouTube, ticker and remote-control payloads keep
  their existing command names and payload formats.
- Confirm screen/video streaming still uses the existing port 39872 and packet
  framing; this migration does not authorize or modify that transport.

## Operational caveats

- Auth anonymous accounts are permanent user records until explicitly removed.
  Clearing app data or losing the Android Keystore session can orphan the old
  device registration. The restaurant owner can revoke the stale registration
  before pairing again.
- The TV checks registration periodically. Realtime RLS blocks new owner
  broadcasts to a revoked device immediately at the database policy boundary;
  an already-open receiver may take until its next check to close its socket.
- The owner command queue stores both the server-issued private topic and the
  restaurant ID, not the physical display ID. A command queued by restaurant A
  is never replayed while restaurant B is selected. Legacy entries without a
  trusted tenant/topic are discarded rather than sent through the old public route.
- The currently passed owner access token is a point-in-time token. If it
  expires while the owner screen remains open, the owner should refresh/sign in
  again before issuing commands or replaying the offline queue.
- SQL has not been applied on staging, and the Edge Function has not been
  runtime-tested. Do not claim end-to-end isolation until the staging checks
  above pass.

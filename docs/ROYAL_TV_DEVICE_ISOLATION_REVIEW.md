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

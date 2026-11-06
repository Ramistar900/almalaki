# ROYAL TV Plus rollout plan

This is a design/implementation checklist. It does not activate Plus or change production.

## Entitlement rules

- The developer account is free only for the explicitly allowlisted user ID + that user's own restaurant ID.
- The free exception is not inferred from a display name, editable user metadata, or the fact that someone is an administrator.
- Other restaurants need an active, unexpired Plus subscription.
- Only an active restaurant owner can query/use owner-only Plus settings.
- Basic local/offline playback remains available when Plus expires or the internet is unavailable.
- Expiry disables advanced Plus actions; it must not delete media, pairing information, or saved layout settings.
- Subscription state and authorization are enforced by Supabase/Postgres, not just by hiding buttons in Android UI.

## Activation and installation continuation

1. The client requests Plus entitlement from the authenticated server for the currently selected restaurant.
2. The server validates the authenticated user ID, active owner membership, and either the exact free allowlist pair or an active, unexpired paid subscription.
3. If Plus is not active, show the Plus activation/checkout step. Do not invent or hard-code a price or payment provider before it is selected.
4. Once the server confirms activation, check whether the standalone Android package com.almalaki.royaltv is installed.
5. If installed, offer to open ROYAL TV and continue to screen pairing.
6. If not installed, immediately open the current ROYAL TV release/download page resolved from the maintained version metadata (prefer the latest release page; do not hard-code a stale APK asset URL).
7. When the user returns after installation, re-check entitlement and package installation, then continue pairing. Never treat returning from a download page as proof that Plus was paid/activated.
8. If download/install is cancelled or fails, keep the subscription status unchanged and show a retry option.

## Required tests before release

- Allowlisted developer account + its own restaurant: free.
- Same account + a different restaurant: not free unless that restaurant has a paid subscription.
- Another owner + restaurant named الملكي: not free solely because of the name.
- Staff, inactive membership, unauthenticated caller, wrong restaurant ID: denied.
- Active paid subscription: allowed until expiry.
- Expired/cancelled subscription: advanced features denied; basic playback still works.
- Activation with ROYAL TV installed: open app/pairing continuation.
- Activation without ROYAL TV installed: open current download page, then re-check after returning.
- Public Realtime device IDs alone must never be treated as authorization for remote layout/control commands.

## Deployment gate

The migration in supabase/migrations/20261029000000_royal_tv_plus_entitlements.sql is a draft only. Review and test it before applying it to production. No payment provider or price has been chosen.

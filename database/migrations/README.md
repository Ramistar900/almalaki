# Almalaki database migrations

The migration filename prefix is the Supabase migration version. Keep it identical to the applied version reported by Supabase.

## Source files matched to the live project

| Applied version | Migration | Source file |
|---|---|---|
| 20261028022645 | royal_link_phase1 | 20261028022645_royal_link_phase1.sql |
| 20261103021700 | customer_accounts_cloud_linking | 20261103021700_customer_accounts_cloud_linking.sql |
| 20261103031332 | user_devices_registry | 20261103031332_user_devices_registry.sql |
| 20261103031433 | restrict_user_devices_table_privileges | 20261103031433_restrict_user_devices_table_privileges.sql |
| 20261103034045 | complete_tenant_isolation_hardening | 20261103034045_complete_tenant_isolation_hardening.sql |
| 20261103034300 | combine_tenant_select_policies_and_indexes | 20261103034300_combine_tenant_select_policies_and_indexes.sql |
| 20261103034845 | require_active_legacy_menu_and_revoke_unused_helper | 20261103034845_require_active_legacy_menu_and_revoke_unused_helper.sql |
| 20261103035541 | explicitly_deny_direct_admin_registry_access | 20261103035541_explicitly_deny_direct_admin_registry_access.sql |
| 20261103041905 | link_authenticated_orders_to_customer_accounts | 20261103041905_link_authenticated_orders_to_customer_accounts.sql |
| 20261103050916 | restaurant_interface_settings | 20261103050916_restaurant_interface_settings.sql |
| 20261104021748 | owner_activation_codes | 20261104021748_owner_activation_codes.sql |
| 20261104022220 | owner_activation_user_lock | 20261104022220_owner_activation_user_lock.sql |

## Edge Functions

- `supabase/functions/owner-activation/index.ts` — issues one-time owner activation codes, lists and pauses/revokes them for dashboard administrators, and redeems an active code only after the new owner's email is confirmed. The full code is returned once; only its SHA-256 hash is stored.

## Recovered applied migration sources and remaining gaps

Two applied migrations were recovered from the historical `royal-link-phase2-tenant-columns` branch. Their filenames match the versions reported by the live Supabase migration history, and the original commits explicitly say they align the filenames with the applied Supabase versions:

| Applied version | Migration | Historical source commit |
|---|---|---|
| 20261029021146 | royal_link_atomic_checkout | `a596b6073195ef644119b4cff93f2538f39e7eac` |
| 20261029021342 | royal_link_legacy_write_guard | `e6f460c83d504602865e8e3ee258a934425e0fbd` |

The recovered SQL files are now present in this branch under their applied filenames. They have not been replayed against any database. Before enabling local migration replay, compare their function definitions, triggers, grants, and constraints with the live catalog.

These six applied migration sources remain unavailable in the current repository history:

- 20261028023548 — royal_link_phase2_tenant_columns
- 20261028024710 — royal_link_phase2_tenant_policies
- 20261028030546 — revoke_public_truncate_privileges
- 20261029033908 — revoke_anon_orders_select
- 20261105015756 — create_tenant_scoped_restaurant_reservations
- 20261105015922 — restrict_reservation_sequence_access

Do not create guessed replacements for the six remaining versions. Recover their original SQL from deployment records or backups, or reconstruct a reviewed baseline from the live schema without pretending it is the original migration source.

## Repository migrations not yet reported as applied

The live migration list inspected during the isolation audit did not include these repository files:

- `20261105080000_enforce_explicit_owner_restaurant_scope.sql`
- `20261105090000_add_cash_payment_tracking.sql`
- `20261105110000_create_tenant_scoped_restaurant_reservations.sql`

The live database already contains `public.restaurant_reservations` and both reservation RPCs, introduced by the applied versions `20261105015756` and `20261105015922`. Therefore, the later `20261105110000` file overlaps an already-applied feature but is not the original applied migration source. Do not apply it or record it as a replacement version until it has been reconciled with the two original migrations, including sequence privileges and the live constraints, indexes, policies, and function grants.

The explicit owner-scope migration `20261105080000` is also not listed as applied in the live project. Its isolated PostgreSQL harness is `database/tests/strict_owner_restaurant_scope.sql`; a passing harness does not mean the migration has been applied to production.

## Integration-test environment requirements

`database/tests/tenant_isolation_and_checkout.sql` is an integration fixture, not a standalone PostgreSQL script. It depends on Supabase Auth objects, Storage objects and policies, private helper functions, and the applied tenant/checkout migration history. A bare `postgres:16` service does not provide those Supabase schemas or policies.

The repository currently has no `supabase/config.toml` local-stack configuration, and the six remaining applied historical migration sources above are missing. Do not claim the full integration suite passed by running it against a bare PostgreSQL container or production. First recover the original migration sources and prepare a reproducible isolated Supabase test stack. Storage upload/update/delete behavior must be tested through the Storage API; direct writes to `storage.objects` do not simulate real Storage API operations.

## Safe workflow

1. Inspect the live schema and current migration list before changing SQL.
2. Recover and review missing applied migration sources; never guess or create empty replacements.
3. Create a new migration with the exact next version/name only for a genuinely new change.
4. Review SQL against current application code and existing data.
5. Test on an isolated Supabase environment before applying to production.
6. Verify RLS, grants, constraints, policies, and rollback behavior.
7. Commit the exact applied SQL to `main`; never publish a second filename/version for an already-applied migration.

A live database with all migrations applied is not automatically reproducible from GitHub until the the six remaining historical source files above are recovered and the two restored files are verified against the live catalog.

## Current tenant domains

The live application/database currently implements restaurant/café tenants. No `stores`, `store_members`, `store_products`, or `store_orders` domain exists yet, so store-to-store isolation is not claimed as implemented. Do not create placeholder store tables without the product requirements; when the store feature is designed, give it its own tenant ownership, RLS, storage namespace, and cross-tenant tests before exposing it.

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
| 20261104023000 | owner_activation_user_lock | 20261104023000_owner_activation_user_lock.sql |


## Edge Functions

- `supabase/functions/owner-activation/index.ts` — issues one-time owner activation codes, lists and pauses/revokes them for dashboard administrators, and redeems an active code only after the new owner's email is confirmed. The full code is returned once; only its SHA-256 hash is stored.

## Applied historical migrations whose original SQL is not present here

The live Almalaki Supabase project reports these migrations as already applied, but their original SQL source files were not present in this repository during the isolation audit:

- 20261028023548 — royal_link_phase2_tenant_columns
- 20261028024710 — royal_link_phase2_tenant_policies
- 20261028030546 — revoke_public_truncate_privileges
- 20261029021146 — royal_link_atomic_checkout
- 20261029021342 — royal_link_legacy_write_guard
- 20261029033908 — revoke_anon_orders_select

Do not create empty or guessed replacement files for these versions. They must be recovered from the original deployment source or backup and reviewed against the live schema before being committed.

## Safe workflow

1. Inspect the live schema and current migration list before changing SQL.
2. Create the migration file with the exact next version/name before applying it.
3. Review the SQL against current application code and existing data.
4. Apply once to the intended Supabase project.
5. Verify RLS, grants, constraints, policies, and a rollback-only test.
6. Commit the exact applied SQL to `main`; never publish a second filename/version for an already-applied migration.

A live database with all migrations applied is not automatically reproducible from GitHub until the six historical source files above are recovered.

## Current tenant domains

The live application/database currently implements restaurant/café tenants. No `stores`, `store_members`, `store_products`, or `store_orders` domain exists yet, so store-to-store isolation is not claimed as implemented. Do not create placeholder store tables without the product requirements; when the store feature is designed, give it its own tenant ownership, RLS, storage namespace, and cross-tenant tests before exposing it.

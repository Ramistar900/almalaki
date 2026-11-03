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

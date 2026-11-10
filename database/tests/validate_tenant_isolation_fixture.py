from pathlib import Path
import re
import sys

sql_path = Path(__file__).with_name("tenant_isolation_and_checkout.sql")
sql = sql_path.read_text(encoding="utf-8")
errors = []

def require(label: str, pattern: str) -> None:
    if re.search(pattern, sql, flags=re.IGNORECASE | re.MULTILINE) is None:
        errors.append(f"missing required fixture guard: {label}")

if len(re.findall(r"(?im)^\s*BEGIN;\s*$", sql)) != 1:
    errors.append("fixture must contain exactly one top-level BEGIN")
if len(re.findall(r"(?im)^\s*ROLLBACK;\s*$", sql)) != 1:
    errors.append("fixture must contain exactly one top-level ROLLBACK")

rollback = re.search(r"(?im)^\s*ROLLBACK;\s*$", sql)
cleanup = re.search(r"(?im)^\s*DO\s+\$test_cleanup\$", sql)
if rollback is None or cleanup is None or rollback.start() >= cleanup.start():
    errors.append("post-rollback cleanup assertions must run after ROLLBACK")

if re.search(
    r"\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+storage\.objects\b",
    sql,
    flags=re.IGNORECASE,
):
    errors.append("do not simulate Storage API operations with direct storage.objects DML")

required = {
    "production execution warning": r"Never run this fixture script on production",
    "failure-injection rollback test": r"Injected atomic checkout rollback test failure",
    "private checkout idempotency": r"private\.checkout_idempotency",
    "two-owner same-local-section test": r"tenant-test-shared-section",
    "customer reservation creation": r"create_restaurant_reservation",
    "customer cross-tenant reservation read denial": r"Customer D can read Customer C reservations",
    "reverse customer reservation read denial": r"Customer C can read Customer D reservations",
    "owner cross-tenant reservation update denial": r"Owner A attempted to manage Owner B reservation scope",
    "legacy checkout compatibility": r"create_restaurant_order\(",
    "legacy payment checkout compatibility": r"create_restaurant_order_with_payment\(",
    "post-rollback user cleanup": r"Rollback left test users",
    "post-rollback reservation cleanup": r"Rollback left test reservations",
    "post-rollback idempotency cleanup": r"Rollback left idempotency records",
}
for label, pattern in required.items():
    require(label, pattern)

if errors:
    print("Tenant integration fixture safety validation FAILED:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"Tenant integration fixture safety validation passed ({len(required)} required checks).")

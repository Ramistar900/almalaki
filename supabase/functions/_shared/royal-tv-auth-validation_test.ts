import {
  allowedRoyalTvCommands,
  isCanonicalUuid,
  isPayloadWithinLimit,
  isRoyalTvCommandClaimLeaseExpired,
  isValidDeviceId,
  isValidDeviceName,
} from "./royal-tv-auth-validation.ts";

Deno.test("accepts canonical UUIDs and rejects malformed UUIDs", () => {
  if (!isCanonicalUuid("123e4567-e89b-12d3-a456-426614174000")) {
    throw new Error("canonical UUID should be accepted");
  }
  for (const invalid of [
    "",
    "123e4567-e89b-12d3-a456-42661417400",
    "123e4567-e89b-12d3-a456-426614174000-extra",
    "123e4567--e89b-12d3-a456-426614174000",
  ]) {
    if (isCanonicalUuid(invalid)) throw new Error(`invalid UUID accepted: ${invalid}`);
  }
});

Deno.test("accepts only the server allow-list of command types", () => {
  for (const allowed of [
    "SHOW_ORDER", "SHOW_YOUTUBE", "SHOW_MEDIA", "SHOW_AD",
    "SHOW_CONTENT", "REMOTE_COMMAND", "CLEAR_SCREEN",
  ]) {
    if (!allowedRoyalTvCommands.has(allowed)) throw new Error(`missing command: ${allowed}`);
  }
  if (allowedRoyalTvCommands.has("DROP_TENANT_DATA")) {
    throw new Error("unexpected command type allowed");
  }
});

Deno.test("validates device identifier and display name bounds", () => {
  if (!isValidDeviceId("tv-device-001")) throw new Error("valid device id rejected");
  if (isValidDeviceId(" short ")) throw new Error("whitespace-padded device id accepted");
  if (isValidDeviceId("x".repeat(129))) throw new Error("oversized device id accepted");
  if (!isValidDeviceName("ROYAL TV")) throw new Error("valid device name rejected");
  if (isValidDeviceName("  ")) throw new Error("blank device name accepted");
  if (isValidDeviceName("x".repeat(101))) throw new Error("oversized device name accepted");
});

Deno.test("rejects payloads larger than the configured character limit", () => {
  if (!isPayloadWithinLimit({ message: "ok" })) throw new Error("small payload rejected");
  if (isPayloadWithinLimit({ message: "x".repeat(32769) })) {
    throw new Error("oversized payload accepted");
  }
  if (isPayloadWithinLimit(undefined)) throw new Error("undefined payload should be rejected");
});


Deno.test("recovers only ROYAL TV claims whose 30-second lease has elapsed", () => {
  const now = Date.parse("2026-11-03T08:00:30.000Z");
  if (!isRoyalTvCommandClaimLeaseExpired("2026-11-03T08:00:00.000Z", now)) {
    throw new Error("claim at the lease boundary should be recoverable");
  }
  if (isRoyalTvCommandClaimLeaseExpired("2026-11-03T08:00:00.001Z", now)) {
    throw new Error("claim within the active lease must not be recovered");
  }
  if (isRoyalTvCommandClaimLeaseExpired(null, now)) {
    throw new Error("claim without a timestamp must not be recovered");
  }
  if (isRoyalTvCommandClaimLeaseExpired("not-a-timestamp", now)) {
    throw new Error("malformed claim timestamp must not be recovered");
  }
});


Deno.test("ROYAL TV schema migration keeps the server-only tables isolated", async () => {
  const sql = await Deno.readTextFile(
    new URL("../../../database/migrations/202610300001_royal_tv_server_authorization.sql", import.meta.url),
  );
  for (const table of [
    "royal_tv_pairing_challenges",
    "royal_tv_paired_devices",
    "royal_tv_commands",
  ]) {
    if (!sql.includes(`alter table public.${table} enable row level security`)) {
      throw new Error(`RLS is not enabled for ${table}`);
    }
    if (!sql.includes(`revoke all on table public.${table}`) ||
        !sql.includes("from public, anon, authenticated")) {
      throw new Error(`Client privileges are not revoked for ${table}`);
    }
    if (!sql.includes(`grant all on table public.${table} to service_role`)) {
      throw new Error(`Service role grant is missing for ${table}`);
    }
  }
});

Deno.test("ROYAL TV pairing RPC is atomic and server-role only", async () => {
  const sql = await Deno.readTextFile(
    new URL("../../../database/migrations/202610310001_royal_tv_pairing_atomicity.sql", import.meta.url),
  );
  if (!/language plpgsql\s+security definer\s+set search_path = ''/i.test(sql)) {
    throw new Error("Pairing RPC must be SECURITY DEFINER with an empty search_path");
  }
  if (!sql.includes("c.consumed_at is null") || !sql.includes("c.expires_at > now()")) {
    throw new Error("Pairing challenge must be atomically unconsumed and unexpired");
  }
  if (!sql.includes("from public, anon, authenticated") ||
      !sql.includes("to service_role")) {
    throw new Error("Pairing RPC must be executable only by service_role");
  }
});


Deno.test("ROYAL TV rate-limit migration is server-only and atomically enforces fixed windows", async () => {
  const sql = await Deno.readTextFile(
    new URL("../../../database/migrations/202611050001_royal_tv_rate_limits.sql", import.meta.url),
  );
  for (const required of [
    "alter table public.royal_tv_rate_limits enable row level security",
    "revoke all on table public.royal_tv_rate_limits from public, anon, authenticated",
    "grant all on table public.royal_tv_rate_limits to service_role",
    "security definer",
    "set search_path = ''",
    "where public.royal_tv_rate_limits.request_count < p_limit",
    "grant execute on function public.consume_royal_tv_rate_limit(text, integer, integer)",
  ]) {
    if (!sql.toLowerCase().includes(required.toLowerCase())) {
      throw new Error(`rate-limit migration is missing: ${required}`);
    }
  }
});

Deno.test("ROYAL TV Edge Function applies action-specific server rate limits", async () => {
  const source = await Deno.readTextFile(
    new URL("../royal-tv-authorize/index.ts", import.meta.url),
  );
  for (const action of [
    "list_owned_restaurants", "create_challenge", "pair_device",
    "list_paired_devices", "send_command", "poll_commands",
    "acknowledge", "revoke_device",
  ]) {
    if (!source.includes(`enforceRateLimit("${action}"`)) {
      throw new Error(`rate limit missing for action: ${action}`);
    }
  }
  if (!source.includes('fail(429, "Too many requests; try again later")')) {
    throw new Error("HTTP 429 response is missing");
  }
});

Deno.test("ROYAL TV audit migration is server-only and stores no raw credentials", async () => {
  const sql = await Deno.readTextFile(
    new URL("../../../database/migrations/202611060001_royal_tv_audit_events.sql", import.meta.url),
  );
  for (const required of [
    "alter table public.royal_tv_audit_events enable row level security",
    "revoke all on table public.royal_tv_audit_events from public, anon, authenticated",
    "revoke all on table public.royal_tv_audit_events from service_role",
    "grant insert on table public.royal_tv_audit_events to service_role",
    "'attempted', 'succeeded'",
    "request_id uuid not null",
    "paired_device_id uuid",
    "target_command_id uuid",
  ]) {
    if (!sql.toLowerCase().includes(required.toLowerCase())) {
      throw new Error(`audit migration is missing: ${required}`);
    }
  }
  const tableDefinition = sql.match(
    /create table if not exists public\\.royal_tv_audit_events \\(([\\s\\S]*?)\\n\\);/i,
  )?.[1] ?? "";
  if (!tableDefinition) throw new Error("audit table definition could not be inspected");
  for (const forbidden of [
    "pairing_code", "device_credential", "credential_hash",
    "access_token", "refresh_token", "command_payload",
  ]) {
    if (tableDefinition.toLowerCase().includes(forbidden)) {
      throw new Error(`raw sensitive field must not be stored in audit table: ${forbidden}`);
    }
  }
});

Deno.test("ROYAL TV sensitive mutations require durable audit attempts", async () => {
  const source = await Deno.readTextFile(
    new URL("../royal-tv-authorize/index.ts", import.meta.url),
  );
  for (const action of [
    "create_challenge", "pair_device", "send_command",
    "acknowledge", "revoke_device",
  ]) {
    if (!source.includes(`recordAuditAttempt("${action}"`)) {
      throw new Error(`durable audit attempt missing for action: ${action}`);
    }
    if (!source.includes(`recordAuditCompletion("${action}"`)) {
      throw new Error(`audit success event missing for action: ${action}`);
    }
  }
  if (!source.includes('throw new Error("AUDIT_LOG_FAILED")')) {
    throw new Error("audit failure must fail closed before sensitive mutations");
  }
  if (!source.includes("ROYAL TV audit completion event could not be persisted")) {
    throw new Error("completion event failure handling is missing");
  }
});

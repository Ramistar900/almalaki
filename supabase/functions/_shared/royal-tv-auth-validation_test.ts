import {
  allowedRoyalTvCommands,
  isCanonicalUuid,
  isPayloadWithinLimit,
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

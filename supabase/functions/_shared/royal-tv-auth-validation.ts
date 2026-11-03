export const allowedRoyalTvCommands = new Set([
  "SHOW_ORDER",
  "SHOW_YOUTUBE",
  "SHOW_MEDIA",
  "SHOW_AD",
  "SHOW_CONTENT",
  "REMOTE_COMMAND",
  "CLEAR_SCREEN",
]);

const UUID_PATTERN = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;

export function isCanonicalUuid(value: string): boolean {
  return UUID_PATTERN.test(value);
}

export function isValidDeviceId(value: string): boolean {
  return value.length >= 8 && value.length <= 128 && value.trim() === value;
}

export function isValidDeviceName(value: string): boolean {
  return value.length >= 1 && value.length <= 100 && value.trim() === value;
}

export function isPayloadWithinLimit(value: unknown, maxChars = 32768): boolean {
  try {
    return JSON.stringify(value).length <= maxChars;
  } catch {
    return false;
  }
}


export const ROYAL_TV_COMMAND_CLAIM_LEASE_MS = 30_000;

/** True only when a valid claim timestamp is at or before the lease cutoff. */
export function isRoyalTvCommandClaimLeaseExpired(
  claimedAt: string | null,
  nowMs: number,
  leaseMs = ROYAL_TV_COMMAND_CLAIM_LEASE_MS,
): boolean {
  if (claimedAt === null || !Number.isFinite(nowMs) || !Number.isFinite(leaseMs) || leaseMs <= 0) {
    return false;
  }
  const claimedAtMs = Date.parse(claimedAt);
  return Number.isFinite(claimedAtMs) && claimedAtMs <= nowMs - leaseMs;
}

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

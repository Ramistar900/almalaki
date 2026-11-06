// ROYAL TV authorization API.
// Deploy only after reviewing the paired schema migration.
// Required Edge Function secrets: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY.
// Never ship SUPABASE_SERVICE_ROLE_KEY to Android clients.

import { createClient } from "npm:@supabase/supabase-js@2";
import {
  allowedRoyalTvCommands,
  isCanonicalUuid,
  isPayloadWithinLimit,
  isValidDeviceId,
  isValidDeviceName,
  ROYAL_TV_COMMAND_CLAIM_LEASE_MS,
} from "../_shared/royal-tv-auth-validation.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const CLAIM_LEASE_MS = ROYAL_TV_COMMAND_CLAIM_LEASE_MS;

const corsHeaders = {
  // Browser access is limited to the official dashboard origin. Native clients
  // are not governed by browser CORS, and still require real request credentials.
  "Access-Control-Allow-Origin": "https://ramistar900.github.io",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-royal-device-credential",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
  "Content-Type": "application/json; charset=utf-8",
};

function response(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function fail(status: number, message: string) {
  return response(status, { error: message });
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest)).map((v) => v.toString(16).padStart(2, "0")).join("");
}

function randomToken(bytes = 32): string {
  const data = crypto.getRandomValues(new Uint8Array(bytes));
  return btoa(String.fromCharCode(...data)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function bearer(request: Request): string | null {
  const value = request.headers.get("authorization") ?? "";
  const match = value.match(/^Bearer\s+(.+)$/i);
  return match?.[1] ?? null;
}

async function ownerIdentity(request: Request) {
  const token = bearer(request);
  if (!token) throw new Error("AUTH_REQUIRED");
  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await userClient.auth.getUser(token);
  if (error || !data.user) throw new Error("AUTH_INVALID");
  return data.user;
}

async function assertOwner(userId: string, restaurantId: string) {
  const { data, error } = await admin.from("restaurant_members")
    .select("restaurant_id")
    .eq("restaurant_id", restaurantId)
    .eq("user_id", userId)
    .eq("role", "owner")
    .eq("is_active", true)
    .maybeSingle();
  if (error) throw new Error("AUTH_CHECK_FAILED");
  if (!data) throw new Error("OWNER_REQUIRED");
  const { data: restaurant, error: restaurantError } = await admin.from("restaurants")
    .select("id").eq("id", restaurantId).eq("status", "active").maybeSingle();
  if (restaurantError) throw new Error("RESTAURANT_CHECK_FAILED");
  if (!restaurant) throw new Error("RESTAURANT_INACTIVE");
}

async function activeDevice(request: Request) {
  const credential = request.headers.get("x-royal-device-credential") ?? "";
  if (credential.length < 32 || credential.length > 256) throw new Error("DEVICE_AUTH_REQUIRED");
  const hash = await sha256(credential);
  const { data, error } = await admin.from("royal_tv_paired_devices")
    .select("id,restaurant_id,device_id,status")
    .eq("credential_hash", hash)
    .eq("status", "active")
    .maybeSingle();
  if (error) throw new Error("DEVICE_CHECK_FAILED");
  if (!data) throw new Error("DEVICE_REVOKED_OR_UNKNOWN");

  // A paired device must not continue receiving queued commands after its
  // restaurant is suspended. Re-check tenant status for every device action.
  const { data: restaurant, error: restaurantError } = await admin.from("restaurants")
    .select("id")
    .eq("id", data.restaurant_id)
    .eq("status", "active")
    .maybeSingle();
  if (restaurantError) throw new Error("RESTAURANT_CHECK_FAILED");
  if (!restaurant) throw new Error("RESTAURANT_INACTIVE");

  return data;
}

async function enforceRateLimit(
  action: string,
  subject: string,
  limit: number,
  windowSeconds: number,
) {
  const bucketKey = await sha256(`${action}:${subject}`);
  const { data, error } = await admin.rpc("consume_royal_tv_rate_limit", {
    p_bucket_key: bucketKey,
    p_limit: limit,
    p_window_seconds: windowSeconds,
  });
  if (error) throw new Error("RATE_LIMIT_CHECK_FAILED");
  if (data !== true) throw new Error("RATE_LIMITED");
}

type RoyalTvAuditAction =
  | "create_challenge"
  | "pair_device"
  | "send_command"
  | "acknowledge"
  | "revoke_device";

type RoyalTvAuditContext = {
  actorUserId?: string | null;
  restaurantId?: string | null;
  pairedDeviceId?: string | null;
  targetCommandId?: string | null;
  details?: Record<string, string | number | boolean | null>;
};

async function recordAuditAttempt(
  action: RoyalTvAuditAction,
  context: RoyalTvAuditContext = {},
): Promise<string> {
  const requestId = crypto.randomUUID();
  const { error } = await admin.from("royal_tv_audit_events").insert({
    request_id: requestId,
    action,
    outcome: "attempted",
    actor_user_id: context.actorUserId ?? null,
    restaurant_id: context.restaurantId ?? null,
    paired_device_id: context.pairedDeviceId ?? null,
    target_command_id: context.targetCommandId ?? null,
    details: context.details ?? {},
  });
  // Fail closed: a security-sensitive operation does not start unless its
  // attempt has been durably recorded.
  if (error) throw new Error("AUDIT_LOG_FAILED");
  return requestId;
}

async function recordAuditCompletion(
  action: RoyalTvAuditAction,
  requestId: string,
  context: RoyalTvAuditContext = {},
): Promise<void> {
  const { error } = await admin.from("royal_tv_audit_events").insert({
    request_id: requestId,
    action,
    outcome: "succeeded",
    actor_user_id: context.actorUserId ?? null,
    restaurant_id: context.restaurantId ?? null,
    paired_device_id: context.pairedDeviceId ?? null,
    target_command_id: context.targetCommandId ?? null,
    details: context.details ?? {},
  });
  // The attempt row remains durable. A completion-write failure must not
  // misreport or roll back a mutation that has already succeeded.
  if (error) console.error("ROYAL TV audit completion event could not be persisted");
}

function errorResponse(error: unknown) {
  const message = error instanceof Error ? error.message : "INTERNAL_ERROR";
  if (message === "AUTH_REQUIRED") return fail(401, "Authentication required");
  if (message === "AUTH_INVALID") return fail(401, "Invalid session");
  if (message === "OWNER_REQUIRED") return fail(403, "Active restaurant owner membership required");
  if (message === "RESTAURANT_INACTIVE") return fail(403, "Restaurant is inactive");
  if (message === "DEVICE_AUTH_REQUIRED") return fail(401, "Device credential required");
  if (message === "RATE_LIMITED") return fail(429, "Too many requests; try again later");
  if (message === "DEVICE_REVOKED_OR_UNKNOWN") return fail(403, "Device is revoked or unknown");
  if (message.endsWith("_FAILED")) return fail(503, "Authorization service unavailable");
  return fail(400, "Request rejected");
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return fail(405, "POST required");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) return fail(503, "Server configuration missing");

  try {
    const declaredLength = Number(request.headers.get("content-length") ?? "0");
    if (Number.isFinite(declaredLength) && declaredLength > 40000) {
      return fail(413, "Request body too large");
    }
    const rawBody = await request.text();
    if (new TextEncoder().encode(rawBody).byteLength > 40000) {
      return fail(413, "Request body too large");
    }
    let body: Record<string, unknown>;
    try {
      const parsed: unknown = JSON.parse(rawBody);
      if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
        return fail(400, "JSON object required");
      }
      body = parsed as Record<string, unknown>;
    } catch {
      return fail(400, "Invalid JSON");
    }
    const action = typeof body.action === "string" ? body.action : "";

    if (action === "list_owned_restaurants") {
      const user = await ownerIdentity(request);
      await enforceRateLimit("list_owned_restaurants", user.id, 60, 60);
      const { data: memberships, error: membershipError } = await admin
        .from("restaurant_members")
        .select("restaurant_id")
        .eq("user_id", user.id)
        .eq("role", "owner")
        .eq("is_active", true);
      if (membershipError) return fail(503, "Could not load owner restaurants");

      const ids = Array.from(new Set((memberships ?? [])
        .map((row) => String(row.restaurant_id ?? ""))
        .filter((id) => /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i.test(id))));
      if (ids.length === 0) return response(200, { restaurants: [] });

      const { data: activeRestaurants, error: restaurantError } = await admin
        .from("restaurants")
        .select("id")
        .in("id", ids)
        .eq("status", "active");
      if (restaurantError) return fail(503, "Could not verify owner restaurants");

      return response(200, {
        restaurants: (activeRestaurants ?? []).map((row) => ({ id: row.id })),
      });
    }

    if (action === "create_challenge") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      if (!isCanonicalUuid(restaurantId)) return fail(400, "Invalid restaurant");
      await assertOwner(user.id, restaurantId);
      await enforceRateLimit("create_challenge", `${user.id}:${restaurantId}`, 10, 600);
      const auditRequestId = await recordAuditAttempt("create_challenge", {
        actorUserId: user.id,
        restaurantId,
      });

      // High-entropy code; store only its digest and return plaintext once.
      const code = randomToken(24);
      const codeHash = await sha256(code);
      const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString();
      const { error } = await admin.from("royal_tv_pairing_challenges").insert({
        restaurant_id: restaurantId,
        created_by: user.id,
        code_hash: codeHash,
        expires_at: expiresAt,
      });
      if (error) return fail(503, "Could not create pairing challenge");
      await recordAuditCompletion("create_challenge", auditRequestId, {
        actorUserId: user.id,
        restaurantId,
      });
      return response(200, { pairing_code: code, expires_at: expiresAt });
    }

    if (action === "pair_device") {
      const code = String(body.pairing_code ?? "");
      const deviceId = String(body.device_id ?? "").trim();
      const deviceName = String(body.device_name ?? "ROYAL TV").trim().slice(0, 100);
      if (code.length < 24 || code.length > 128) return fail(400, "Invalid pairing code");
      if (!isValidDeviceId(deviceId)) return fail(400, "Invalid device identifier");
      if (!isValidDeviceName(deviceName)) return fail(400, "Invalid device name");
      const clientIp = request.headers.get("cf-connecting-ip") ?? "";
      await enforceRateLimit("pair_device", clientIp || deviceId, 10, 300);
      const auditRequestId = await recordAuditAttempt("pair_device");

      const codeHash = await sha256(code);
      const credential = randomToken(32);
      const credentialHash = await sha256(credential);

      // A database transaction consumes the challenge and inserts the device
      // together. If registration fails, the challenge consumption rolls back.
      const { data: paired, error: pairError } = await admin.rpc(
        "pair_royal_tv_device",
        {
          p_code_hash: codeHash,
          p_device_id: deviceId,
          p_device_name: deviceName,
          p_credential_hash: credentialHash,
        },
      );
      if (pairError) {
        // Do not reveal whether a code, owner, or device identifier was valid.
        return fail(409, "Pairing code invalid, expired, or device registration rejected");
      }
      const pairedRow = Array.isArray(paired) ? paired[0] : paired;
      if (!pairedRow?.restaurant_id) {
        return fail(503, "Pairing registration returned an invalid result");
      }
      // Resolve only internal identifiers for audit correlation. Never read or
      // store the raw pairing code or newly issued device credential.
      const { data: pairedDevice } = await admin.from("royal_tv_paired_devices")
        .select("id,paired_by")
        .eq("restaurant_id", pairedRow.restaurant_id)
        .eq("device_id", deviceId)
        .maybeSingle();
      await recordAuditCompletion("pair_device", auditRequestId, {
        actorUserId: pairedDevice?.paired_by ?? null,
        restaurantId: pairedRow.restaurant_id,
        pairedDeviceId: pairedDevice?.id ?? null,
      });
      // This is the only response containing the raw device credential.
      return response(200, {
        device_credential: credential,
        restaurant_id: pairedRow.restaurant_id,
      });
    }

    if (action === "list_paired_devices") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      if (!isCanonicalUuid(restaurantId)) {
        return fail(400, "Invalid restaurant");
      }
      await assertOwner(user.id, restaurantId);
      await enforceRateLimit("list_paired_devices", `${user.id}:${restaurantId}`, 60, 60);
      const { data, error } = await admin.from("royal_tv_paired_devices")
        .select("id,device_id,device_name,status,paired_at,last_seen_at,revoked_at")
        .eq("restaurant_id", restaurantId)
        .order("paired_at", { ascending: false })
        .limit(100);
      if (error) return fail(503, "Could not load paired devices");
      return response(200, { devices: data ?? [] });
    }

    if (action === "send_command") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      const deviceId = String(body.device_id ?? "");
      const commandType = String(body.command_type ?? "");
      const payload = body.payload ?? {};
      if (!isCanonicalUuid(restaurantId)) return fail(400, "Invalid restaurant");
      if (!isValidDeviceId(deviceId)) return fail(400, "Invalid device identifier");
      if (!allowedRoyalTvCommands.has(commandType)) return fail(400, "Unsupported command type");
      if (!isPayloadWithinLimit(payload)) return fail(413, "Payload too large");
      await assertOwner(user.id, restaurantId);
      await enforceRateLimit("send_command", `${user.id}:${restaurantId}`, 30, 60);

      const { data: device, error: deviceError } = await admin.from("royal_tv_paired_devices")
        .select("id,restaurant_id")
        .eq("device_id", deviceId)
        .eq("restaurant_id", restaurantId)
        .eq("status", "active")
        .maybeSingle();
      if (deviceError) return fail(503, "Could not verify target device");
      if (!device) return fail(404, "No active paired device for this restaurant");

      const createdAt = new Date();
      const auditRequestId = await recordAuditAttempt("send_command", {
        actorUserId: user.id,
        restaurantId,
        pairedDeviceId: device.id,
        details: { command_type: commandType },
      });
      const { data: command, error } = await admin.from("royal_tv_commands").insert({
        restaurant_id: restaurantId,
        paired_device_id: device.id,
        command_type: commandType,
        payload,
        created_by: user.id,
        expires_at: new Date(createdAt.getTime() + 2 * 60 * 1000).toISOString(),
      }).select("id,created_at,expires_at").single();
      if (error) return fail(503, "Could not queue command");
      await recordAuditCompletion("send_command", auditRequestId, {
        actorUserId: user.id,
        restaurantId,
        pairedDeviceId: device.id,
        targetCommandId: command.id,
        details: { command_type: commandType },
      });
      return response(200, { command });
    }

    if (action === "poll_commands") {
      const device = await activeDevice(request);
      await enforceRateLimit("poll_commands", device.id, 120, 60);
      const nowDate = new Date();
      const now = nowDate.toISOString();
      const staleClaimCutoff = new Date(nowDate.getTime() - CLAIM_LEASE_MS).toISOString();
      const { error: touchError } = await admin.from("royal_tv_paired_devices")
        .update({ last_seen_at: now }).eq("id", device.id).eq("status", "active");
      if (touchError) return fail(503, "Could not update device status");

      // Expire old commands and recover only claims whose lease elapsed.
      // All updates remain scoped to this exact paired device and restaurant.
      const { error: expireError } = await admin.from("royal_tv_commands")
        .update({ status: "expired" })
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .in("status", ["pending", "claimed"])
        .lte("expires_at", now);
      if (expireError) return fail(503, "Could not expire old commands");

      const { error: recoveryError } = await admin.from("royal_tv_commands")
        .update({ status: "pending", claimed_at: null })
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .eq("status", "claimed")
        .lte("claimed_at", staleClaimCutoff)
        .gt("expires_at", now);
      if (recoveryError) return fail(503, "Could not recover expired command claims");

      // Claim each row with a conditional update so concurrent polls cannot
      // both receive the same pending command. A claim is leased for 30 seconds;
      // only this device can recover it after the lease expires.
      const { data: pending, error: loadError } = await admin.from("royal_tv_commands")
        .select("id")
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .eq("status", "pending")
        .gt("expires_at", now)
        .order("created_at", { ascending: true })
        .limit(20);
      if (loadError) return fail(503, "Could not load commands");

      const commands = [];
      for (const row of pending ?? []) {
        const claimedAt = new Date().toISOString();
        const { data: claimed, error: claimError } = await admin.from("royal_tv_commands")
          .update({ status: "claimed", claimed_at: claimedAt })
          .eq("id", row.id)
          .eq("paired_device_id", device.id)
          .eq("restaurant_id", device.restaurant_id)
          .eq("status", "pending")
          .gt("expires_at", claimedAt)
          .select("id,command_type,payload,created_at,expires_at")
          .maybeSingle();
        if (claimError) return fail(503, "Could not claim command");
        if (claimed) commands.push(claimed);
      }
      return response(200, { commands });
    }

    if (action === "acknowledge") {
      const device = await activeDevice(request);
      await enforceRateLimit("acknowledge", device.id, 120, 60);
      const commandId = String(body.command_id ?? "");
      if (!/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i.test(commandId)) return fail(400, "Invalid command id");
      const auditRequestId = await recordAuditAttempt("acknowledge", {
        pairedDeviceId: device.id,
        targetCommandId: commandId,
      });
      const acknowledgedAt = new Date();
      const acknowledgementTime = acknowledgedAt.toISOString();
      const activeLeaseCutoff = new Date(acknowledgedAt.getTime() - CLAIM_LEASE_MS).toISOString();
      const { data, error } = await admin.from("royal_tv_commands")
        .update({ status: "acked", acknowledged_at: acknowledgementTime })
        .eq("id", commandId)
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .eq("status", "claimed")
        .is("acknowledged_at", null)
        .gt("claimed_at", activeLeaseCutoff)
        .gt("expires_at", acknowledgementTime)
        .select("id")
        .maybeSingle();
      if (error) return fail(503, "Could not acknowledge command");
      if (!data) return fail(409, "Command expired, already processed, or belongs to another device");
      await recordAuditCompletion("acknowledge", auditRequestId, {
        pairedDeviceId: device.id,
        targetCommandId: commandId,
        restaurantId: device.restaurant_id,
      });
      return response(200, { acknowledged: true });
    }

    if (action === "revoke_device") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      const deviceId = String(body.device_id ?? "");
      if (!isCanonicalUuid(restaurantId)) return fail(400, "Invalid restaurant");
      if (!isValidDeviceId(deviceId)) return fail(400, "Invalid device identifier");
      await assertOwner(user.id, restaurantId);
      await enforceRateLimit("revoke_device", `${user.id}:${restaurantId}`, 10, 60);
      const { data: targetDevice, error: targetDeviceError } = await admin.from("royal_tv_paired_devices")
        .select("id")
        .eq("restaurant_id", restaurantId)
        .eq("device_id", deviceId)
        .eq("status", "active")
        .maybeSingle();
      if (targetDeviceError) return fail(503, "Could not verify target device");
      if (!targetDevice) return fail(404, "Active paired device not found");
      const auditRequestId = await recordAuditAttempt("revoke_device", {
        actorUserId: user.id,
        restaurantId,
        pairedDeviceId: targetDevice.id,
      });
      const { data, error } = await admin.from("royal_tv_paired_devices")
        .update({ status: "revoked", revoked_at: new Date().toISOString() })
        .eq("id", targetDevice.id)
        .eq("restaurant_id", restaurantId)
        .eq("status", "active")
        .select("id")
        .maybeSingle();
      if (error) return fail(503, "Could not revoke device");
      if (!data) return fail(404, "Active paired device not found");
      await recordAuditCompletion("revoke_device", auditRequestId, {
        actorUserId: user.id,
        restaurantId,
        pairedDeviceId: data.id,
      });
      return response(200, { revoked: true });
    }

    return fail(400, "Unknown action");
  } catch (error) {
    return errorResponse(error);
  }
});

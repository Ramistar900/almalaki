// ROYAL TV authorization API.
// Deploy only after reviewing the paired schema migration.
// Required Edge Function secrets: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY.
// Never ship SUPABASE_SERVICE_ROLE_KEY to Android clients.

import { createClient } from "npm:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const allowedCommands = new Set([
  "SHOW_ORDER",
  "SHOW_YOUTUBE",
  "SHOW_MEDIA",
  "SHOW_AD",
  "SHOW_CONTENT",
  "REMOTE_COMMAND",
  "CLEAR_SCREEN",
]);

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-royal-device-credential",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
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
  return data;
}

function errorResponse(error: unknown) {
  const message = error instanceof Error ? error.message : "INTERNAL_ERROR";
  if (message === "AUTH_REQUIRED") return fail(401, "Authentication required");
  if (message === "AUTH_INVALID") return fail(401, "Invalid session");
  if (message === "OWNER_REQUIRED") return fail(403, "Active restaurant owner membership required");
  if (message === "RESTAURANT_INACTIVE") return fail(403, "Restaurant is inactive");
  if (message === "DEVICE_AUTH_REQUIRED") return fail(401, "Device credential required");
  if (message === "DEVICE_REVOKED_OR_UNKNOWN") return fail(403, "Device is revoked or unknown");
  if (message.endsWith("_FAILED")) return fail(503, "Authorization service unavailable");
  return fail(400, "Request rejected");
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return fail(405, "POST required");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) return fail(503, "Server configuration missing");

  try {
    const body = await request.json();
    const action = typeof body?.action === "string" ? body.action : "";

    if (action === "create_challenge") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      if (!/^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(restaurantId)) return fail(400, "Invalid restaurant");
      await assertOwner(user.id, restaurantId);

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
      return response(200, { pairing_code: code, expires_at: expiresAt });
    }

    if (action === "pair_device") {
      const code = String(body.pairing_code ?? "");
      const deviceId = String(body.device_id ?? "").trim();
      const deviceName = String(body.device_name ?? "ROYAL TV").trim().slice(0, 100);
      if (code.length < 24 || code.length > 128) return fail(400, "Invalid pairing code");
      if (deviceId.length < 8 || deviceId.length > 128) return fail(400, "Invalid device identifier");
      if (!deviceName) return fail(400, "Invalid device name");

      const codeHash = await sha256(code);
      const { data: challenge, error: challengeError } = await admin.from("royal_tv_pairing_challenges")
        .select("id,restaurant_id,created_by")
        .eq("code_hash", codeHash)
        .is("consumed_at", null)
        .gt("expires_at", new Date().toISOString())
        .maybeSingle();
      if (challengeError) return fail(503, "Could not validate pairing code");
      if (!challenge) return fail(403, "Pairing code expired or already used");

      // Conditional UPDATE is atomic: only one concurrent request can consume this challenge.
      const { data: consumed, error: consumeError } = await admin.from("royal_tv_pairing_challenges")
        .update({ consumed_at: new Date().toISOString() })
        .eq("id", challenge.id)
        .is("consumed_at", null)
        .gt("expires_at", new Date().toISOString())
        .select("id")
        .maybeSingle();
      if (consumeError) return fail(503, "Could not consume pairing code");
      if (!consumed) return fail(403, "Pairing code expired or already used");

      const credential = randomToken(32);
      const credentialHash = await sha256(credential);
      const { error: insertError } = await admin.from("royal_tv_paired_devices").insert({
        restaurant_id: challenge.restaurant_id,
        device_id: deviceId,
        device_name: deviceName,
        credential_hash: credentialHash,
        paired_by: challenge.created_by,
        status: "active",
      });
      if (insertError) return fail(409, "Device already paired or could not be registered");
      // This is the only response containing the raw device credential.
      return response(200, { device_credential: credential, restaurant_id: challenge.restaurant_id });
    }

    if (action === "send_command") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      const deviceId = String(body.device_id ?? "");
      const commandType = String(body.command_type ?? "");
      const payload = body.payload ?? {};
      if (!allowedCommands.has(commandType)) return fail(400, "Unsupported command type");
      if (JSON.stringify(payload).length > 32768) return fail(413, "Payload too large");
      await assertOwner(user.id, restaurantId);

      const { data: device, error: deviceError } = await admin.from("royal_tv_paired_devices")
        .select("id,restaurant_id")
        .eq("device_id", deviceId)
        .eq("restaurant_id", restaurantId)
        .eq("status", "active")
        .maybeSingle();
      if (deviceError) return fail(503, "Could not verify target device");
      if (!device) return fail(404, "No active paired device for this restaurant");

      const createdAt = new Date();
      const { data: command, error } = await admin.from("royal_tv_commands").insert({
        restaurant_id: restaurantId,
        paired_device_id: device.id,
        command_type: commandType,
        payload,
        created_by: user.id,
        expires_at: new Date(createdAt.getTime() + 2 * 60 * 1000).toISOString(),
      }).select("id,created_at,expires_at").single();
      if (error) return fail(503, "Could not queue command");
      return response(200, { command });
    }

    if (action === "poll_commands") {
      const device = await activeDevice(request);
      const { error: touchError } = await admin.from("royal_tv_paired_devices")
        .update({ last_seen_at: new Date().toISOString() }).eq("id", device.id).eq("status", "active");
      if (touchError) return fail(503, "Could not update device status");

      const { data, error } = await admin.from("royal_tv_commands")
        .select("id,command_type,payload,created_at,expires_at")
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .eq("status", "pending")
        .gt("expires_at", new Date().toISOString())
        .order("created_at", { ascending: true })
        .limit(20);
      if (error) return fail(503, "Could not load commands");
      return response(200, { commands: data ?? [] });
    }

    if (action === "acknowledge") {
      const device = await activeDevice(request);
      const commandId = String(body.command_id ?? "");
      if (!/^[0-9a-f-]{36}$/i.test(commandId)) return fail(400, "Invalid command id");
      const { data, error } = await admin.from("royal_tv_commands")
        .update({ status: "acked", acknowledged_at: new Date().toISOString() })
        .eq("id", commandId)
        .eq("paired_device_id", device.id)
        .eq("restaurant_id", device.restaurant_id)
        .eq("status", "pending")
        .gt("expires_at", new Date().toISOString())
        .select("id")
        .maybeSingle();
      if (error) return fail(503, "Could not acknowledge command");
      if (!data) return fail(409, "Command expired, already processed, or belongs to another device");
      return response(200, { acknowledged: true });
    }

    if (action === "revoke_device") {
      const user = await ownerIdentity(request);
      const restaurantId = String(body.restaurant_id ?? "");
      const deviceId = String(body.device_id ?? "");
      await assertOwner(user.id, restaurantId);
      const { data, error } = await admin.from("royal_tv_paired_devices")
        .update({ status: "revoked", revoked_at: new Date().toISOString() })
        .eq("restaurant_id", restaurantId)
        .eq("device_id", deviceId)
        .eq("status", "active")
        .select("id")
        .maybeSingle();
      if (error) return fail(503, "Could not revoke device");
      if (!data) return fail(404, "Active paired device not found");
      return response(200, { revoked: true });
    }

    return fail(400, "Unknown action");
  } catch (error) {
    return errorResponse(error);
  }
});

import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const ALLOWED_ORIGIN = "https://ramistar900.github.io";

const corsHeaders = {
  "Access-Control-Allow-Origin": ALLOWED_ORIGIN,
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

function json(status: number, body: Record<string, unknown>, extra: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, ...extra, "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });
}

function cleanString(value: unknown, max = 160): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, "0")).join("");
}

function ownerClient(accessToken: string) {
  return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${accessToken}` } },
  });
}

function serviceClient() {
  return createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SUPABASE_SERVICE_ROLE_KEY) {
    return json(500, { error: "server_not_configured" });
  }

  const origin = req.headers.get("origin");
  if (origin && origin !== ALLOWED_ORIGIN) return json(403, { error: "origin_not_allowed" });

  let body: Record<string, unknown>;
  try {
    const raw = await req.text();
    if (raw.length > 12_000) return json(413, { error: "request_too_large" });
    body = JSON.parse(raw);
    if (!body || typeof body !== "object" || Array.isArray(body)) throw new Error("invalid_json");
  } catch {
    return json(400, { error: "invalid_request" });
  }

  const action = cleanString(body.action, 40);

  try {
    if (action === "redeem") {
      const activationCode = cleanString(body.activation_code, 128);
      const deviceId = cleanString(body.device_id, 64);
      const channelDeviceId = cleanString(body.channel_device_id, 80);
      const deviceName = cleanString(body.device_name, 80);
      if (!/^[0-9a-f]{64}$/.test(activationCode) ||
          !isUuid(deviceId) ||
          !/^ROYAL-TV-[0-9a-f]{12}$/i.test(channelDeviceId) ||
          deviceName.length < 1) {
        return json(400, { error: "invalid_activation_request" });
      }

      const anon = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        auth: { persistSession: false, autoRefreshToken: false },
      });
      const { data, error } = await anon.rpc("redeem_royal_tv_activation_code", {
        p_activation_code: activationCode,
        p_device_id: deviceId,
        p_channel_device_id: channelDeviceId,
        p_device_name: deviceName,
      });
      if (error || !Array.isArray(data) || data.length !== 1) {
        return json(403, { error: "activation_failed" });
      }
      return json(200, {
        device_id: data[0].registered_device_id,
        device_token: data[0].device_token,
        restaurant_id: data[0].restaurant_id,
      });
    }

    if (action === "device_check") {
      const deviceToken = cleanString(body.device_token, 256);
      if (!/^[0-9a-f]{64}$/.test(deviceToken)) return json(401, { error: "device_unauthorized" });
      const tokenHash = await sha256Hex(deviceToken);
      const admin = serviceClient();
      const { data, error } = await admin
        .from("royal_tv_devices")
        .select("device_id, channel_device_id, restaurant_id, revoked_at, restaurants!inner(status)")
        .eq("device_token_hash", tokenHash)
        .maybeSingle();
      if (error || !data || data.revoked_at !== null || data.restaurants?.status !== "active") {
        return json(401, { error: "device_unauthorized" });
      }
      const { data: touched, error: touchError } = await admin
        .from("royal_tv_devices")
        .update({ last_seen_at: new Date().toISOString() })
        .eq("device_id", data.device_id)
        .is("revoked_at", null)
        .select("device_id");
      if (touchError) return json(503, { error: "device_check_unavailable" });
      if (!touched?.length) return json(401, { error: "device_unauthorized" });
      return json(200, {
        authorized: true,
        device_id: data.device_id,
        channel_device_id: data.channel_device_id,
      });
    }

    const authHeader = req.headers.get("authorization") ?? "";
    const match = authHeader.match(/^Bearer\s+(.+)$/i);
    if (!match) return json(401, { error: "owner_auth_required" });
    const accessToken = match[1].trim();
    if (!accessToken || accessToken.length > 8192) return json(401, { error: "owner_auth_required" });

    const owner = ownerClient(accessToken);
    const { data: userData, error: userError } = await owner.auth.getUser(accessToken);
    if (userError || !userData.user) return json(401, { error: "owner_auth_invalid" });

    if (action === "issue_code") {
      const restaurantId = cleanString(body.restaurant_id, 64);
      if (!isUuid(restaurantId)) return json(400, { error: "invalid_restaurant" });
      const { data, error } = await owner.rpc("issue_royal_tv_activation_code", {
        p_restaurant_id: restaurantId,
      });
      if (error || !Array.isArray(data) || data.length !== 1) return json(403, { error: "activation_issue_failed" });
      return json(200, { activation_code: data[0].activation_code, expires_at: data[0].expires_at });
    }

    if (action === "list_devices") {
      const restaurantId = cleanString(body.restaurant_id, 64);
      if (!/^[0-9a-f-]{36}$/i.test(restaurantId)) return json(400, { error: "invalid_restaurant" });
      const { data, error } = await owner
        .from("royal_tv_devices")
        .select("device_id, channel_device_id, restaurant_id, device_name, created_at, last_seen_at, revoked_at")
        .eq("restaurant_id", restaurantId)
        .order("created_at", { ascending: false });
      if (error) return json(403, { error: "device_list_failed" });
      return json(200, { devices: data ?? [] });
    }

    if (action === "revoke_device") {
      const deviceId = cleanString(body.device_id, 64);
      if (!isUuid(deviceId)) return json(400, { error: "invalid_device" });
      const { data, error } = await owner
        .from("royal_tv_devices")
        .update({ revoked_at: new Date().toISOString() })
        .eq("device_id", deviceId)
        .is("revoked_at", null)
        .select("device_id");
      if (error) return json(403, { error: "device_revoke_failed" });
      if (!data?.length) return json(404, { error: "device_not_found_or_already_revoked" });
      return json(200, { revoked: true });
    }

    return json(400, { error: "unknown_action" });
  } catch {
    // Deliberately do not log request bodies, activation codes, or bearer tokens.
    return json(500, { error: "request_failed" });
  }
});

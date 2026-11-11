import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const DASHBOARD_ORIGIN = "https://ramistar900.github.io";

const corsHeaders = {
  "Access-Control-Allow-Origin": DASHBOARD_ORIGIN,
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

function respond(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

function clean(value: unknown, max = 160): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) =>
    byte.toString(16).padStart(2, "0")
  ).join("");
}

function makePassword(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(48)), (byte) =>
    byte.toString(16).padStart(2, "0")
  ).join("");
}

function ownerClient(accessToken: string) {
  return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${accessToken}` } },
  });
}

function adminClient() {
  return createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

async function isRestaurantOwner(
  admin: ReturnType<typeof adminClient>,
  userId: string,
  restaurantId: string,
): Promise<boolean> {
  const { data, error } = await admin
    .from("restaurant_members")
    .select("restaurant_id")
    .eq("restaurant_id", restaurantId)
    .eq("user_id", userId)
    .eq("role", "owner")
    .eq("is_active", true)
    .maybeSingle();

  return !error && data !== null;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return respond(405, { error: "method_not_allowed" });

  if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SUPABASE_SERVICE_ROLE_KEY) {
    return respond(503, { error: "service_not_configured" });
  }

  const origin = req.headers.get("origin");
  if (origin && origin !== DASHBOARD_ORIGIN) {
    return respond(403, { error: "origin_not_allowed" });
  }

  let body: Record<string, unknown>;
  try {
    const raw = await req.text();
    if (raw.length > 8_000) return respond(413, { error: "request_too_large" });
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("invalid_json");
    body = parsed as Record<string, unknown>;
  } catch {
    return respond(400, { error: "invalid_request" });
  }

  try {
    const admin = adminClient();
    const action = clean(body.action, 32);

    if (action === "redeem") {
      const activationCode = clean(body.activation_code, 64).toLowerCase();
      const deviceId = clean(body.device_id, 36).toLowerCase();
      const channelDeviceId = clean(body.channel_device_id, 80).toUpperCase();
      const deviceName = clean(body.device_name, 80);

      if (!/^[0-9a-f]{64}$/.test(activationCode) ||
          !isUuid(deviceId) ||
          !/^ROYAL-TV-(?:[0-9A-F]{12}|[0-9]{1,})$/.test(channelDeviceId) ||
          deviceName.length < 1) {
        return respond(400, { error: "invalid_activation_request" });
      }

      const codeHash = await sha256Hex(activationCode);
      const { data: codeRow, error: codeLookupError } = await admin
        .from("royal_tv_activation_codes")
        .select("id, expires_at, consumed_at")
        .eq("code_hash", codeHash)
        .maybeSingle();

      if (codeLookupError ||
          !codeRow ||
          codeRow.consumed_at !== null ||
          Date.parse(codeRow.expires_at) <= Date.now()) {
        return respond(403, { error: "activation_code_invalid_or_used" });
      }

      // Each TV receives its own isolated Supabase Auth identity. The random
      // credentials never leave this function; only the resulting session is
      // returned to the TV once over TLS.
      const email = `tv-${deviceId}@royal-tv.invalid`;
      const password = makePassword();
      const { data: created, error: createError } =
        await admin.auth.admin.createUser({
          email,
          password,
          email_confirm: true,
          user_metadata: {
            royal_tv_device: true,
            royal_tv_device_id: deviceId,
          },
        });

      if (createError || !created.user) {
        return respond(409, { error: "device_registration_unavailable" });
      }

      const { data: redeemed, error: redeemError } = await admin.rpc(
        "redeem_royal_tv_activation_code",
        {
          p_code_hash: codeHash,
          p_device_id: deviceId,
          p_channel_device_id: channelDeviceId,
          p_device_name: deviceName,
          p_auth_user_id: created.user.id,
        },
      );

      if (redeemError || !Array.isArray(redeemed) || redeemed.length !== 1) {
        await admin.auth.admin.deleteUser(created.user.id);
        return respond(403, { error: "activation_code_invalid_or_used" });
      }

      const deviceAuth = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        auth: { persistSession: false, autoRefreshToken: false },
      });
      const { data: signedIn, error: signInError } =
        await deviceAuth.auth.signInWithPassword({ email, password });

      if (signInError || !signedIn.session) {
        await admin.from("royal_tv_devices")
          .update({ revoked_at: new Date().toISOString() })
          .eq("device_id", deviceId)
          .is("revoked_at", null);
        await admin.auth.admin.updateUserById(created.user.id, {
          ban_duration: "876000h",
        });
        return respond(503, { error: "device_session_creation_failed_request_new_code" });
      }

      const row = redeemed[0] as { registered_restaurant_id: string };
      return respond(200, {
        device_id: deviceId,
        restaurant_id: row.registered_restaurant_id,
        access_token: signedIn.session.access_token,
        refresh_token: signedIn.session.refresh_token,
        expires_in: signedIn.session.expires_in,
        token_type: signedIn.session.token_type,
      });
    }

    const authorization = req.headers.get("authorization") ?? "";
    const match = authorization.match(/^Bearer\s+(.+)$/i);
    if (!match) return respond(401, { error: "owner_auth_required" });
    const accessToken = match[1].trim();
    if (!accessToken || accessToken.length > 8192) return respond(401, { error: "owner_auth_required" });

    const owner = ownerClient(accessToken);
    const { data: userData, error: userError } = await owner.auth.getUser(accessToken);
    if (userError || !userData.user) return respond(401, { error: "owner_auth_invalid" });

    if (action === "issue_code") {
      const restaurantId = clean(body.restaurant_id, 36).toLowerCase();
      if (!isUuid(restaurantId)) return respond(400, { error: "invalid_restaurant" });

      // The issuance RPC performs the authoritative owner + active-restaurant
      // checks through private.is_restaurant_owner() using this owner's JWT.
      const { data, error } = await owner.rpc("issue_royal_tv_activation_code", {
        p_restaurant_id: restaurantId,
      });
      if (error || !Array.isArray(data) || data.length !== 1) {
        return respond(403, { error: "activation_issue_failed" });
      }
      return respond(200, {
        activation_code: data[0].activation_code,
        expires_at: data[0].expires_at,
      });
    }

    if (action === "list_devices") {
      const restaurantId = clean(body.restaurant_id, 36).toLowerCase();
      if (!isUuid(restaurantId)) return respond(400, { error: "invalid_restaurant" });

      if (!(await isRestaurantOwner(admin, userData.user.id, restaurantId))) {
        return respond(403, { error: "restaurant_owner_required" });
      }

      const { data, error } = await admin
        .from("royal_tv_devices")
        .select("device_id, channel_device_id, device_name, created_at, last_seen_at, revoked_at")
        .eq("restaurant_id", restaurantId)
        .order("created_at", { ascending: false });
      if (error) return respond(503, { error: "device_list_unavailable" });
      return respond(200, { devices: data ?? [] });
    }

    if (action === "revoke_device") {
      const deviceId = clean(body.device_id, 36).toLowerCase();
      if (!isUuid(deviceId)) return respond(400, { error: "invalid_device" });

      const { data: device, error: lookupError } = await admin
        .from("royal_tv_devices")
        .select("device_id, restaurant_id, auth_user_id, revoked_at")
        .eq("device_id", deviceId)
        .maybeSingle();
      if (lookupError || !device) return respond(404, { error: "device_not_found" });

      if (!(await isRestaurantOwner(admin, userData.user.id, device.restaurant_id))) {
        return respond(403, { error: "restaurant_owner_required" });
      }
      if (device.revoked_at) return respond(409, { error: "device_already_revoked" });

      const revokedAt = new Date().toISOString();
      const { error: revokeError } = await admin
        .from("royal_tv_devices")
        .update({ revoked_at: revokedAt })
        .eq("device_id", deviceId)
        .is("revoked_at", null);
      if (revokeError) return respond(503, { error: "device_revoke_failed" });

      // Prevent future token refreshes. An already-open Realtime socket may
      // retain its join authorization until its current JWT expires; the owner
      // publisher must re-check active registration on each fresh channel join.
      const { error: banError } = await admin.auth.admin.updateUserById(
        device.auth_user_id,
        { ban_duration: "876000h" },
      );
      if (banError) return respond(503, { error: "device_revoked_but_session_ban_pending" });

      return respond(200, { revoked: true, revoked_at: revokedAt });
    }

    return respond(400, { error: "unknown_action" });
  } catch {
    // Never log activation codes, passwords, access tokens, or refresh tokens.
    return respond(500, { error: "request_failed" });
  }
});

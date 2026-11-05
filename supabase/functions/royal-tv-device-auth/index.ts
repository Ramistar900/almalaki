import { createClient } from "https://esm.sh/@supabase/supabase-js@2.118.0";

const ALLOWED_ORIGIN = "https://ramistar900.github.io";
const corsHeaders = {
  "Access-Control-Allow-Origin": ALLOWED_ORIGIN,
  "Vary": "Origin",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

type Action =
  | "issue_code"
  | "redeem"
  | "device_check"
  | "list_devices"
  | "revoke_device";

type RequestBody = {
  action?: Action;
  restaurant_id?: string;
  activation_code?: string;
  channel_device_id?: string;
  device_name?: string;
  device_id?: string;
};

function respond(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: corsHeaders,
  });
}

function isUuid(value: unknown): value is string {
  return typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
      .test(value);
}

function cleanText(value: unknown, maxLength: number): string | null {
  if (typeof value !== "string") return null;
  const clean = value.trim();
  return clean.length > 0 && clean.length <= maxLength ? clean : null;
}

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(value),
  );
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function getClientIp(req: Request): string | null {
  // Trust the proxy-provided Cloudflare address only. Client-controlled
  // X-Forwarded-For values must not be allowed to reset the guess limit.
  const cloudflareIp = req.headers.get("cf-connecting-ip")?.trim();
  return cloudflareIp || null;
}

Deno.serve(async (req: Request): Promise<Response> => {
  const origin = req.headers.get("Origin");
  if (origin && origin !== ALLOWED_ORIGIN) {
    return respond(403, { error: "Origin is not allowed" });
  }

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return respond(405, { error: "Method not allowed" });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return respond(503, { error: "Server configuration is incomplete" });
    }

    const authorization = req.headers.get("Authorization") ?? "";
    const tokenMatch = authorization.match(/^Bearer\s+(.+)$/i);
    if (!tokenMatch) {
      return respond(401, { error: "Authentication required" });
    }
    const accessToken = tokenMatch[1];

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${accessToken}` } },
      auth: {
        persistSession: false,
        autoRefreshToken: false,
        detectSessionInUrl: false,
      },
    });
    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
        detectSessionInUrl: false,
      },
    });

    const { data: authData, error: authError } =
      await userClient.auth.getUser(accessToken);
    const user = authData.user;
    if (authError || !user) {
      return respond(401, { error: "Invalid or expired session" });
    }

    let body: RequestBody;
    try {
      body = await req.json();
    } catch {
      return respond(400, { error: "Invalid JSON body" });
    }

    const action = body.action;
    if (
      action !== "issue_code" &&
      action !== "redeem" &&
      action !== "device_check" &&
      action !== "list_devices" &&
      action !== "revoke_device"
    ) {
      return respond(400, { error: "Unsupported action" });
    }

    if (action === "issue_code") {
      if (user.is_anonymous) {
        return respond(403, { error: "Restaurant owner sign-in is required" });
      }
      if (!isUuid(body.restaurant_id)) {
        return respond(400, { error: "Invalid restaurant ID" });
      }

      const { data, error } = await userClient.rpc(
        "issue_royal_tv_activation_code",
        { p_restaurant_id: body.restaurant_id },
      );
      if (error) {
        const status = error.code === "42501" ? 403 : 400;
        return respond(status, { error: error.message });
      }
      const row = Array.isArray(data) ? data[0] : data;
      if (!row?.activation_code || !row?.expires_at) {
        return respond(502, { error: "Activation code was not created" });
      }

      return respond(200, {
        activation_code: row.activation_code,
        expires_at: row.expires_at,
      });
    }

    if (action === "redeem") {
      if (user.is_anonymous !== true) {
        return respond(403, { error: "A TV device session is required" });
      }
      const activationCode = cleanText(body.activation_code, 16);
      const channelDeviceId = cleanText(body.channel_device_id, 80);
      const deviceName = cleanText(body.device_name, 80);
      if (
        !activationCode ||
        !/^[0-9a-f]{16}$/.test(activationCode) ||
        !channelDeviceId ||
        !deviceName
      ) {
        return respond(400, { error: "Invalid activation details" });
      }

      const rateLimitSalt = Deno.env.get("ROYAL_TV_RATE_LIMIT_SALT");
      const clientIp = getClientIp(req);
      if (!rateLimitSalt || !clientIp) {
        return respond(503, { error: "Activation protection is not configured" });
      }

      const ipHash = await sha256Hex(`${rateLimitSalt}:${clientIp}`);
      const { data: allowed, error: rateError } = await admin.rpc(
        "record_royal_tv_activation_attempt",
        {
          p_auth_user_id: user.id,
          p_ip_hash: ipHash,
        },
      );
      if (rateError) {
        return respond(503, { error: "Unable to verify activation protection" });
      }
      if (allowed !== true) {
        return respond(429, { error: "Too many activation attempts" });
      }

      const { data, error } = await admin.rpc(
        "redeem_royal_tv_activation_code",
        {
          p_activation_code: activationCode,
          p_auth_user_id: user.id,
          p_channel_device_id: channelDeviceId,
          p_device_name: deviceName,
        },
      );
      if (error) {
        const status = error.code === "23505" ? 409 : 400;
        return respond(status, { error: error.message });
      }
      const row = Array.isArray(data) ? data[0] : data;
      if (!row?.registered_device_id || !row?.channel_topic) {
        return respond(502, { error: "Device registration was not completed" });
      }

      return respond(200, {
        registered: true,
        device_id: row.registered_device_id,
        channel_topic: row.channel_topic,
      });
    }

    if (action === "device_check") {
      if (user.is_anonymous !== true) {
        return respond(403, { error: "A TV device session is required" });
      }
      const { data: device, error: deviceError } = await admin
        .from("royal_tv_devices")
        .select(
          "device_id, auth_user_id, restaurant_id, channel_device_id, device_name, channel_topic, revoked_at",
        )
        .eq("auth_user_id", user.id)
        .maybeSingle();

      if (deviceError) {
        return respond(503, { error: "Unable to verify device registration" });
      }
      if (!device) {
        return respond(404, { registered: false, error: "TV is not paired" });
      }
      if (device.revoked_at) {
        return respond(409, { error: "This TV registration has been revoked" });
      }

      const { data: restaurant, error: restaurantError } = await admin
        .from("restaurants")
        .select("status")
        .eq("id", device.restaurant_id)
        .maybeSingle();

      if (restaurantError || !restaurant || restaurant.status !== "active") {
        return respond(403, { error: "The linked restaurant is not active" });
      }

      const { data: refreshedDevice, error: updateError } = await admin
        .from("royal_tv_devices")
        .update({ last_seen_at: new Date().toISOString() })
        .eq("device_id", device.device_id)
        .eq("auth_user_id", user.id)
        .is("revoked_at", null)
        .select("device_id")
        .maybeSingle();

      if (updateError) {
        return respond(503, { error: "Unable to refresh device status" });
      }
      if (!refreshedDevice) {
        return respond(409, { error: "This TV registration has been revoked" });
      }

      return respond(200, {
        registered: true,
        device_id: device.device_id,
        channel_topic: device.channel_topic,
        channel_device_id: device.channel_device_id,
        device_name: device.device_name,
      });
    }

    if (user.is_anonymous) {
      return respond(403, { error: "Restaurant owner sign-in is required" });
    }
    if (!isUuid(body.restaurant_id)) {
      return respond(400, { error: "Invalid restaurant ID" });
    }

    const restaurantId = body.restaurant_id;
    const { data: membership, error: membershipError } = await admin
      .from("restaurant_members")
      .select("restaurant_id")
      .eq("restaurant_id", restaurantId)
      .eq("user_id", user.id)
      .eq("role", "owner")
      .eq("is_active", true)
      .maybeSingle();

    if (membershipError || !membership) {
      return respond(403, { error: "Restaurant owner permission required" });
    }

    const { data: restaurant, error: restaurantError } = await admin
      .from("restaurants")
      .select("status")
      .eq("id", restaurantId)
      .maybeSingle();

    if (restaurantError || !restaurant || restaurant.status !== "active") {
      return respond(403, { error: "The linked restaurant is not active" });
    }

    if (action === "list_devices") {
      const { data: devices, error } = await admin
        .from("royal_tv_devices")
        .select(
          "device_id, channel_device_id, device_name, channel_topic, created_at, last_seen_at, revoked_at",
        )
        .eq("restaurant_id", restaurantId)
        .order("created_at", { ascending: false });

      if (error) {
        return respond(503, { error: "Unable to list restaurant devices" });
      }
      return respond(200, { devices: devices ?? [] });
    }

    if (!isUuid(body.device_id)) {
      return respond(400, { error: "Invalid device ID" });
    }

    const { data: revoked, error } = await admin
      .from("royal_tv_devices")
      .update({ revoked_at: new Date().toISOString() })
      .eq("device_id", body.device_id)
      .eq("restaurant_id", restaurantId)
      .is("revoked_at", null)
      .select("device_id")
      .maybeSingle();

    if (error) {
      return respond(503, { error: "Unable to revoke device" });
    }
    if (!revoked) {
      return respond(404, { error: "Active device registration was not found" });
    }

    return respond(200, { revoked: true, device_id: revoked.device_id });
  } catch {
    return respond(500, { error: "Unexpected server error" });
  }
});

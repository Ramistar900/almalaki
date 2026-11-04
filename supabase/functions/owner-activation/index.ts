import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function respond(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function normalizeCode(value: string): string {
  return value.toUpperCase().replace(/[\s-]/g, "");
}

function base32(bytes: Uint8Array): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let bits = 0;
  let value = 0;
  let output = "";
  for (const byte of bytes) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      output += alphabet[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) output += alphabet[(value << (5 - bits)) & 31];
  return output;
}

function formatCode(raw: string): string {
  const groups = raw.match(/.{1,4}/g) ?? [];
  return "ROYAL-OWN-" + groups.join("-");
}

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(normalizeCode(value)),
  );
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

Deno.serve(async (req: Request): Promise<Response> => {
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
      return respond(503, { error: "Owner activation service is not configured" });
    }

    const authorization = req.headers.get("Authorization") ?? "";
    const match = authorization.match(/^Bearer\s+(.+)$/i);
    if (!match) return respond(401, { error: "Authentication required" });
    const accessToken = match[1];

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${accessToken}` } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: userData, error: userError } =
      await userClient.auth.getUser(accessToken);
    if (userError || !userData.user) {
      return respond(401, { error: "Invalid or expired session" });
    }

    let body: {
      action?: string;
      note?: string;
      id?: string;
      status?: string;
      code?: string;
      restaurant_name?: string;
    };
    try {
      const parsedBody = await req.json();
      if (!parsedBody || typeof parsedBody !== "object" || Array.isArray(parsedBody)) {
        return respond(400, { error: "Invalid JSON body" });
      }
      body = parsedBody as typeof body;
    } catch {
      return respond(400, { error: "Invalid JSON body" });
    }

    const action = body.action;
    if (!["issue", "list", "set_status", "redeem"].includes(action ?? "")) {
      return respond(400, { error: "Unsupported action" });
    }

    if (action === "redeem") {
      if (!userData.user.email_confirmed_at) {
        return respond(403, { error: "Confirm your email before activating an owner account" });
      }
      const code = typeof body.code === "string" ? body.code.trim() : "";
      const restaurantName = typeof body.restaurant_name === "string"
        ? body.restaurant_name.trim()
        : "";
      if (code.length < 20 || code.length > 100) {
        return respond(400, { error: "Invalid or inactive owner activation code" });
      }
      if (restaurantName.length < 2 || restaurantName.length > 100) {
        return respond(400, { error: "Restaurant name must be between 2 and 100 characters" });
      }
      const adminClient = createClient(supabaseUrl, serviceRoleKey, {
        auth: { persistSession: false, autoRefreshToken: false },
      });
      const { data, error } = await adminClient.rpc("redeem_owner_activation_code", {
        p_user_id: userData.user.id,
        p_code_hash: await sha256Hex(code),
        p_restaurant_name: restaurantName,
      });
      if (error) {
        const message = error.message.includes("already has an active owner")
          ? "This account already has an active owner membership"
          : error.message.includes("Invalid or inactive")
          ? "Invalid or inactive owner activation code"
          : "Could not activate this owner account";
        return respond(400, { error: message });
      }
      const row = Array.isArray(data) ? data[0] : data;
      return respond(200, {
        ok: true,
        restaurant_id: row?.created_restaurant_id,
        restaurant_name: row?.created_restaurant_name,
      });
    }

    const { data: isAdmin, error: adminError } =
      await userClient.rpc("is_dashboard_admin");
    if (adminError || isAdmin !== true) {
      return respond(403, { error: "Developer administrator access required" });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    if (action === "issue") {
      const randomBytes = crypto.getRandomValues(new Uint8Array(20));
      const rawCode = base32(randomBytes);
      const code = formatCode(rawCode);
      const codeHash = await sha256Hex(code);
      const note = typeof body.note === "string" ? body.note.trim().slice(0, 120) : "";
      const { data, error } = await adminClient
        .from("owner_activation_codes")
        .insert({
          code_hash: codeHash,
          code_prefix: code.slice(0, 14),
          status: "active",
          created_by: userData.user.id,
          note: note || null,
        })
        .select("id, code_prefix, status, created_at, note")
        .single();
      if (error || !data) {
        return respond(500, { error: "Could not issue an owner activation code" });
      }
      // The full secret is returned only once; only its SHA-256 hash is stored.
      return respond(200, { ok: true, code, activation: data });
    }

    if (action === "list") {
      const { data, error } = await adminClient
        .from("owner_activation_codes")
        .select("id, code_prefix, status, created_at, updated_at, expires_at, redeemed_at, restaurant_id, note")
        .order("created_at", { ascending: false })
        .limit(100);
      if (error) return respond(500, { error: "Could not load owner activation codes" });
      return respond(200, { ok: true, activations: data ?? [] });
    }

    const id = typeof body.id === "string" ? body.id.trim() : "";
    const nextStatus = typeof body.status === "string" ? body.status : "";
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
      return respond(400, { error: "Invalid activation record" });
    }
    if (!["active", "paused", "revoked"].includes(nextStatus ?? "")) {
      return respond(400, { error: "Invalid activation status" });
    }
    const { data, error } = await adminClient
      .from("owner_activation_codes")
      .update({ status: nextStatus, updated_at: new Date().toISOString() })
      .eq("id", id)
      .in("status", ["active", "paused"])
      .select("id, code_prefix, status, updated_at")
      .maybeSingle();
    if (error) return respond(500, { error: "Could not update activation status" });
    if (!data) {
      return respond(409, { error: "This code is already redeemed, revoked, or unavailable" });
    }
    return respond(200, { ok: true, activation: data });
  } catch {
    return respond(500, { error: "Unexpected owner activation service error" });
  }
});

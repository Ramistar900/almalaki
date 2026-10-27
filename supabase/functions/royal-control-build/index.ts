import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://ramistar900.github.io",
  "Vary": "Origin",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

const OWNER = "Ramistar900";
const REPO = "almalaki";
const API_VERSION = "2022-11-28";

const WORKFLOWS = {
  almalaki: "build-apk.yml",
  royaltv: "build-royaltv.yml",
} as const;

type AppName = keyof typeof WORKFLOWS;
type ActionName = "status";

function respond(
  status: number,
  body: Record<string, unknown>,
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: corsHeaders,
  });
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
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
    const githubToken = Deno.env.get("GITHUB_ROYAL_CONTROL_TOKEN");

    if (!supabaseUrl || !supabaseAnonKey) {
      return respond(500, {
        error: "Supabase server configuration is incomplete",
      });
    }

    const authorization = req.headers.get("Authorization") ?? "";
    const tokenMatch = authorization.match(/^Bearer\s+(.+)$/i);

    if (!tokenMatch) {
      return respond(401, { error: "Authentication required" });
    }

    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: {
        headers: { Authorization: `Bearer ${tokenMatch[1]}` },
      },
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    });

    const { data: userData, error: userError } =
      await supabase.auth.getUser(tokenMatch[1]);

    if (userError || !userData.user) {
      return respond(401, { error: "Invalid or expired session" });
    }

    const { data: isAdmin, error: adminError } =
      await supabase.rpc("is_dashboard_admin");

    if (adminError || isAdmin !== true) {
      return respond(403, { error: "Dashboard administrator access required" });
    }

    let body: { action?: ActionName; app?: AppName };

    try {
      body = await req.json();
    } catch {
      return respond(400, { error: "Invalid JSON body" });
    }

    const action = body.action;
    const app = body.app;

    if (action !== "status") {
      return respond(400, { error: "Unsupported action" });
    }

    if (app !== "almalaki" && app !== "royaltv") {
      return respond(400, { error: "Unsupported app" });
    }

    if (!githubToken) {
      return respond(503, {
        error: "GitHub server token is not configured",
      });
    }

    const workflow = WORKFLOWS[app];
    const apiBase = `https://api.github.com/repos/${OWNER}/${REPO}/actions/workflows/${workflow}`;

    const githubHeaders = {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${githubToken}`,
      "X-GitHub-Api-Version": API_VERSION,
    };

    if (action === "status") {
      const response = await fetch(
        `${apiBase}/runs?branch=main&per_page=10`,
        { headers: githubHeaders },
      );

      if (!response.ok) {
        return respond(502, {
          error: "Unable to retrieve workflow status from GitHub",
          githubStatus: response.status,
        });
      }

      const result = await response.json();
      const runs = Array.isArray(result.workflow_runs)
        ? result.workflow_runs
        : [];

      return respond(200, {
        app,
        workflow,
        runs: runs.map((run: Record<string, unknown>) => ({
          id: run.id,
          name: run.name,
          status: run.status,
          conclusion: run.conclusion,
          branch: run.head_branch,
          createdAt: run.created_at,
          updatedAt: run.updated_at,
          url: run.html_url,
          runNumber: run.run_number,
        })),
      });
    }

    return respond(400, { error: "Unsupported action" });
  } catch {
    return respond(500, { error: "Unexpected server error" });
  }
});

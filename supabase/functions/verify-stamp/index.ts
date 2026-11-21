// Edge Function entry. Config via env only: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY,
// PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON, PLAY_INTEGRITY_PACKAGE_NAME, KELANA_ENV.
import { createClient } from "npm:@supabase/supabase-js@2";
import { handleRequest } from "./handler.ts";
import { verifierFromEnv } from "./integrity.ts";
import type { StampDb } from "./verify.ts";

const sb = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const db: StampDb = {
  async distance(placeId, lat, lng) {
    const { data, error } = await sb.rpc("stamp_distance", { p_place_id: placeId, p_lat: lat, p_lng: lng });
    if (error) throw new Error(error.code ?? "rpc_failed");
    return (data && data[0]) ?? null;
  },
  async hasStamp(userId, placeId) {
    const { data, error } = await sb.from("stamps").select("id").eq("user_id", userId).eq("place_id", placeId)
      .maybeSingle();
    if (error) throw new Error(error.code ?? "select_failed");
    return data !== null;
  },
  async insertStamp(userId, placeId) {
    const { error } = await sb.from("stamps").insert({ user_id: userId, place_id: placeId });
    if (!error) return "inserted";
    if (error.code === "23505") return "exists";
    throw new Error(error.code ?? "insert_failed");
  },
};

const verifier = verifierFromEnv(Deno.env);

Deno.serve((req) =>
  handleRequest(req, {
    now: () => new Date(),
    db,
    verifier,
    async authenticate(r) {
      const m = /^Bearer (.+)$/.exec(r.headers.get("authorization") ?? "");
      if (!m) return null;
      const { data, error } = await sb.auth.getUser(m[1]);
      return error || !data.user ? null : data.user.id;
    },
  })
);

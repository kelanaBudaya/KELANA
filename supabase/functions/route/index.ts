// Edge Function entry: road-following routes via public Valhalla, cached in public.route_cache.
// Env only: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY. verify_jwt = true (anon key or user JWT; guests allowed).
//
// Example (replace <ref> and <anon-key>):
//   curl -s -X POST https://<ref>.supabase.co/functions/v1/route \
//     -H "Authorization: Bearer <anon-key>" -H "apikey: <anon-key>" -H "Content-Type: application/json" \
//     -d '{"profile":"car","points":[{"lat":-7.8052,"lng":110.3642},{"lat":-7.7520,"lng":110.4914},{"lat":-7.8100,"lng":110.3590}]}'
import { createClient } from "npm:@supabase/supabase-js@2";
import { handleRequest } from "./handler.ts";
import { isFresh, type Leg, Throttle } from "./route.ts";

const sb = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

// Per-instance only; see Throttle for the rationale.
const throttle = new Throttle(() => Date.now(), (ms) => new Promise((r) => setTimeout(r, ms)));

Deno.serve((req) =>
  handleRequest(req, {
    async cacheGet(key) {
      const { data, error } = await sb.from("route_cache").select("legs, created_at").eq("key", key).maybeSingle();
      if (error) throw new Error(error.code ?? "select_failed");
      if (!data || !isFresh(data.created_at, new Date())) return null;
      return data.legs as Leg[];
    },
    async cachePut(key, profile, legs) {
      const { error } = await sb.from("route_cache").upsert({
        key,
        profile,
        legs,
        created_at: new Date().toISOString(),
      });
      if (error) throw new Error(error.code ?? "upsert_failed");
    },
    acquire: () => throttle.acquire(),
    fetch: (url, init) => fetch(url, init),
  })
);

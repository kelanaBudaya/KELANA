// Edge Function entry. Config via env only: SUPABASE_URL, SUPABASE_ANON_KEY (both set by the platform).
// No service role here: every read runs as the caller (their JWT + the anon key), so Row Level Security applies,
// and each query also filters on the caller's id.
import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";
import type { ExportSource, ProfileRow } from "./export.ts";
import { handleRequest } from "./handler.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;

/** PostgREST returns at most `max_rows` (1000) per request: read in pages. */
const PAGE = 1000;

const PLACE = "place:places(name,cap_code)";

// deno-lint-ignore no-explicit-any
type Query = any;

async function all<T>(build: () => Query): Promise<T[]> {
  const out: T[] = [];
  for (let from = 0;; from += PAGE) {
    const { data, error } = await build().range(from, from + PAGE - 1);
    if (error) throw new Error("query_failed");
    const rows = (data ?? []) as T[];
    out.push(...rows);
    if (rows.length < PAGE) return out;
  }
}

function sourceFor(sb: SupabaseClient): ExportSource {
  return {
    async profile(userId) {
      const { data, error } = await sb.from("profiles")
        .select("id,display_name,created_at,home_region:regions(code,name)")
        .eq("id", userId)
        .maybeSingle();
      if (error) throw new Error("query_failed");
      // A many-to-one embed comes back as one object (the untyped client types it as an array).
      return data as unknown as ProfileRow | null;
    },
    stamps: (userId) =>
      all(() =>
        sb.from("stamps").select(`user_id,place_id,stamped_at,${PLACE}`).eq("user_id", userId)
          .order("stamped_at", { ascending: true }).order("place_id", { ascending: true })
      ),
    postcards: (userId) =>
      all(() =>
        sb.from("postcards")
          .select(`id,user_id,place_id,message,rating,card_style,is_public,photo_path,created_at,${PLACE}`)
          .eq("user_id", userId).order("created_at", { ascending: true }).order("id", { ascending: true })
      ),
    videos: (userId) =>
      all(() =>
        sb.from("videos").select(`id,user_id,place_id,caption,duration_s,created_at,${PLACE}`)
          .eq("user_id", userId).order("created_at", { ascending: true }).order("id", { ascending: true })
      ),
    reports: (userId) =>
      all(() =>
        sb.from("reports")
          .select(`id,user_id,place_id,postcard_id,video_id,reason,details,photo_path,created_at,${PLACE}`)
          .eq("user_id", userId).order("created_at", { ascending: true }).order("id", { ascending: true })
      ),
    subscriptions: (userId) =>
      all(() =>
        sb.from("subscriptions").select(`id,user_id,category,place_id,created_at,${PLACE}`)
          .eq("user_id", userId).order("created_at", { ascending: true }).order("id", { ascending: true })
      ),
    async signedUrl(bucket, path, expiresInS) {
      try {
        const { data, error } = await sb.storage.from(bucket).createSignedUrl(path, expiresInS);
        return error || !data?.signedUrl ? null : data.signedUrl;
      } catch {
        return null;
      }
    },
  };
}

Deno.serve((req) =>
  handleRequest(req, {
    now: () => new Date(),
    async authenticate(r) {
      const m = /^Bearer (.+)$/.exec(r.headers.get("authorization") ?? "");
      if (!m) return null;
      const jwt = m[1];
      const sb = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        global: { headers: { Authorization: `Bearer ${jwt}` } },
        auth: { persistSession: false, autoRefreshToken: false },
      });
      // Verifies the token with the Auth server; a deleted or forged user gets null (401).
      const { data, error } = await sb.auth.getUser(jwt);
      if (error || !data.user) return null;
      return {
        user: { id: data.user.id, email: data.user.email ?? null, createdAt: data.user.created_at ?? null },
        source: sourceFor(sb),
      };
    },
  })
);

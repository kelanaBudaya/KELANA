// Edge Function entry. Config via env only: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY (both set by the platform).
// The service role key stays on the server; the client only sends its own user JWT.
import { createClient } from "npm:@supabase/supabase-js@2";
import type { AccountAdmin } from "./account.ts";
import { handleRequest } from "./handler.ts";

const sb = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const admin: AccountAdmin = {
  async list(bucket, prefix, offset, limit) {
    const { data, error } = await sb.storage.from(bucket).list(prefix, {
      limit,
      offset,
      sortBy: { column: "name", order: "asc" },
    });
    if (error) throw new Error("storage_list_failed");
    // Folders come back with id null; files have an id.
    return (data ?? []).map((e) => ({ name: e.name, isFolder: e.id === null }));
  },
  async remove(bucket, paths) {
    const { error } = await sb.storage.from(bucket).remove(paths);
    if (error) throw new Error("storage_remove_failed");
  },
  async deleteUser(userId) {
    // Hard delete (shouldSoftDelete = false): the database cascades and nulls as the migrations say.
    const { error } = await sb.auth.admin.deleteUser(userId, false);
    if (error) throw new Error("delete_user_failed");
  },
};

Deno.serve((req) =>
  handleRequest(req, {
    admin,
    async authenticate(r) {
      const m = /^Bearer (.+)$/.exec(r.headers.get("authorization") ?? "");
      if (!m) return null;
      // Verifies the token with the Auth server; a deleted or forged user gets null (401).
      const { data, error } = await sb.auth.getUser(m[1]);
      return error || !data.user ? null : data.user.id;
    },
  })
);

// Pure logic of "Hapus akun". No network here: storage and auth admin are injected, so it is testable.
//
// What is deleted, and where that is decided:
//   - Postcard photos in Storage (bucket `postcards`, path `<user_id>/<place_id>/<file>`): removed here, through
//     the Storage API, BEFORE the user. If that fails the user is not deleted and the client may retry.
//   - The auth user: `auth.admin.deleteUser` (hard delete). The database then cascades profiles, stamps,
//     postcards, videos and subscriptions, and sets `reports.user_id` to null (migration
//     20261123080000_account_deletion.sql). Reports and their photos (bucket `report-photos`) are kept.
// The user id always comes from the verified JWT (see handler.ts), never from the request body.

/** The confirmation word the user typed. The client sends it; the server refuses without it. */
export const CONFIRM_WORD = "HAPUS";

/** Bucket holding postcard photos under `<user_id>/...`. */
export const POSTCARD_BUCKET = "postcards";

/** Page size for listing and batch size for removing Storage objects. */
export const STORAGE_PAGE = 100;

/** Folder depth under the user's folder: `<user_id>/<place_id>/<file>` is depth 2. One spare level. */
const MAX_DEPTH = 3;

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface StorageEntry {
  name: string;
  /** True for a folder (the Storage API lists folders with `id: null`). */
  isFolder: boolean;
}

export interface AccountAdmin {
  /** One page of entries directly under `prefix` in `bucket`. Throws on failure. */
  list(bucket: string, prefix: string, offset: number, limit: number): Promise<StorageEntry[]>;
  /** Removes the given object paths from `bucket`. Throws on failure. */
  remove(bucket: string, paths: string[]): Promise<void>;
  /** Hard-deletes the auth user. Throws on failure. */
  deleteUser(userId: string): Promise<void>;
}

/** Every object path under `<userId>/` in `bucket`, walking folders. */
export async function collectUserObjects(admin: AccountAdmin, bucket: string, userId: string): Promise<string[]> {
  const out: string[] = [];
  async function walk(prefix: string, depth: number): Promise<void> {
    for (let offset = 0;; offset += STORAGE_PAGE) {
      const page = await admin.list(bucket, prefix, offset, STORAGE_PAGE);
      for (const entry of page) {
        // A name with a slash or a dot path could step out of the user's folder: never follow or remove it.
        if (!entry.name || entry.name.includes("/") || entry.name === "." || entry.name === "..") continue;
        const path = `${prefix}/${entry.name}`;
        if (entry.isFolder) {
          if (depth < MAX_DEPTH) await walk(path, depth + 1);
        } else {
          out.push(path);
        }
      }
      if (page.length < STORAGE_PAGE) return;
    }
  }
  await walk(userId, 1);
  return out;
}

/**
 * Deletes the account of [userId]: first the user's postcard photos, then the auth user.
 * Throws when any step fails; the caller answers 500 and nothing claims success.
 */
export async function deleteAccount(userId: string, admin: AccountAdmin): Promise<{ removedObjects: number }> {
  // The id comes from a verified JWT; still refuse anything that is not a UUID, since it becomes a Storage prefix.
  if (!UUID_RE.test(userId)) throw new Error("invalid_user_id");

  const paths = await collectUserObjects(admin, POSTCARD_BUCKET, userId);
  for (const path of paths) {
    if (!path.startsWith(`${userId}/`)) throw new Error("path_outside_user_folder");
  }
  for (let i = 0; i < paths.length; i += STORAGE_PAGE) {
    await admin.remove(POSTCARD_BUCKET, paths.slice(i, i + STORAGE_PAGE));
  }
  await admin.deleteUser(userId);
  return { removedObjects: paths.length };
}

/** True when the body is `{ "confirm": "HAPUS" }` (exact, case-sensitive). Other fields are ignored. */
export function isConfirmed(body: unknown): boolean {
  return typeof body === "object" && body !== null && !Array.isArray(body) &&
    (body as Record<string, unknown>).confirm === CONFIRM_WORD;
}

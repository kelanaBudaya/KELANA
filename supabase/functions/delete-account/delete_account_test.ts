import assert from "node:assert/strict";
import {
  type AccountAdmin,
  collectUserObjects,
  CONFIRM_WORD,
  deleteAccount,
  isConfirmed,
  POSTCARD_BUCKET,
  STORAGE_PAGE,
  type StorageEntry,
} from "./account.ts";
import { handleRequest, type HandlerDeps } from "./handler.ts";

const USER = "11111111-1111-4111-8111-111111111111";
const OTHER = "99999999-9999-4999-8999-999999999999";
const PLACE_A = "22222222-2222-4222-8222-222222222222";
const PLACE_B = "33333333-3333-4333-8333-333333333333";

/** In-memory Storage + auth admin. Objects are full paths per bucket. */
class FakeAdmin implements AccountAdmin {
  objects = new Map<string, Set<string>>();
  users = new Set<string>([USER, OTHER]);
  removeCalls: string[][] = [];
  listCalls: string[] = [];
  failList = false;
  failRemove = false;
  failDeleteUser = false;
  /** Extra raw entries returned when listing a given prefix (to test hostile names). */
  extra = new Map<string, StorageEntry[]>();

  put(bucket: string, path: string) {
    if (!this.objects.has(bucket)) this.objects.set(bucket, new Set());
    this.objects.get(bucket)!.add(path);
  }

  has(bucket: string, path: string) {
    return this.objects.get(bucket)?.has(path) ?? false;
  }

  list(bucket: string, prefix: string, offset: number, limit: number): Promise<StorageEntry[]> {
    this.listCalls.push(`${bucket}:${prefix}:${offset}`);
    if (this.failList) return Promise.reject(new Error("list"));
    const children = new Map<string, boolean>();
    for (const path of this.objects.get(bucket) ?? []) {
      if (!path.startsWith(`${prefix}/`)) continue;
      const rest = path.slice(prefix.length + 1);
      const slash = rest.indexOf("/");
      if (slash < 0) children.set(rest, false);
      else children.set(rest.slice(0, slash), true);
    }
    const entries: StorageEntry[] = [...children.entries()].sort(([a], [b]) => a.localeCompare(b))
      .map(([name, isFolder]) => ({ name, isFolder }));
    const all = [...entries, ...(this.extra.get(prefix) ?? [])];
    return Promise.resolve(all.slice(offset, offset + limit));
  }

  remove(bucket: string, paths: string[]): Promise<void> {
    this.removeCalls.push(paths);
    if (this.failRemove) return Promise.reject(new Error("remove"));
    for (const p of paths) this.objects.get(bucket)?.delete(p);
    return Promise.resolve();
  }

  deleteUser(userId: string): Promise<void> {
    if (this.failDeleteUser) return Promise.reject(new Error("delete"));
    this.users.delete(userId);
    return Promise.resolve();
  }
}

const seeded = () => {
  const a = new FakeAdmin();
  a.put(POSTCARD_BUCKET, `${USER}/${PLACE_A}/one.jpg`);
  a.put(POSTCARD_BUCKET, `${USER}/${PLACE_A}/two.jpg`);
  a.put(POSTCARD_BUCKET, `${USER}/${PLACE_B}/three.webp`);
  a.put(POSTCARD_BUCKET, `${OTHER}/${PLACE_A}/other.jpg`);
  a.put("report-photos", `${USER}/report.png`);
  return a;
};

const req = (body: unknown, init: RequestInit = {}, raw?: string) =>
  new Request("http://localhost/delete-account", {
    method: "POST",
    headers: { authorization: "Bearer test-jwt", "content-type": "application/json" },
    body: raw ?? JSON.stringify(body),
    ...init,
  });

const deps = (admin: AccountAdmin, user: string | null = USER): HandlerDeps => ({
  admin,
  authenticate: () => Promise.resolve(user),
});

const quiet = async <T>(fn: () => Promise<T>): Promise<T> => {
  const log = console.log;
  const err = console.error;
  console.log = () => {};
  console.error = () => {};
  try {
    return await fn();
  } finally {
    console.log = log;
    console.error = err;
  }
};

Deno.test("confirmation must be exactly HAPUS", () => {
  assert.equal(CONFIRM_WORD, "HAPUS");
  assert.ok(isConfirmed({ confirm: "HAPUS" }));
  assert.ok(isConfirmed({ confirm: "HAPUS", userId: OTHER }));
  for (const bad of [undefined, null, [], "HAPUS", {}, { confirm: "hapus" }, { confirm: " HAPUS" }, { confirm: true }]) {
    assert.equal(isConfirmed(bad), false, JSON.stringify(bad));
  }
});

Deno.test("collects every object under the user's folder only", async () => {
  const a = seeded();
  const paths = await collectUserObjects(a, POSTCARD_BUCKET, USER);
  assert.deepEqual(paths.sort(), [
    `${USER}/${PLACE_A}/one.jpg`,
    `${USER}/${PLACE_A}/two.jpg`,
    `${USER}/${PLACE_B}/three.webp`,
  ]);
});

Deno.test("pages through large folders", async () => {
  const a = new FakeAdmin();
  const n = STORAGE_PAGE * 2 + 5;
  for (let i = 0; i < n; i++) a.put(POSTCARD_BUCKET, `${USER}/${PLACE_A}/${String(i).padStart(4, "0")}.jpg`);
  const paths = await collectUserObjects(a, POSTCARD_BUCKET, USER);
  assert.equal(paths.length, n);
  assert.equal(a.listCalls.filter((c) => c.startsWith(`${POSTCARD_BUCKET}:${USER}/${PLACE_A}:`)).length, 3);
});

Deno.test("hostile entry names are never followed or removed", async () => {
  const a = seeded();
  a.extra.set(USER, [
    { name: "..", isFolder: true },
    { name: `../${OTHER}`, isFolder: true },
    { name: "", isFolder: false },
  ]);
  const paths = await collectUserObjects(a, POSTCARD_BUCKET, USER);
  assert.equal(paths.length, 3);
  assert.ok(paths.every((p) => p.startsWith(`${USER}/`)));
});

Deno.test("deleteAccount removes the user's postcard photos, then the user", async () => {
  const a = seeded();
  const r = await deleteAccount(USER, a);
  assert.equal(r.removedObjects, 3);
  assert.equal(a.has(POSTCARD_BUCKET, `${USER}/${PLACE_A}/one.jpg`), false);
  assert.equal(a.has(POSTCARD_BUCKET, `${USER}/${PLACE_B}/three.webp`), false);
  assert.equal(a.users.has(USER), false);
  // Other user untouched.
  assert.ok(a.has(POSTCARD_BUCKET, `${OTHER}/${PLACE_A}/other.jpg`));
  assert.ok(a.users.has(OTHER));
  // Report photos are kept with the (now anonymous) report.
  assert.ok(a.has("report-photos", `${USER}/report.png`));
});

Deno.test("removes in batches of STORAGE_PAGE", async () => {
  const a = new FakeAdmin();
  for (let i = 0; i < STORAGE_PAGE + 1; i++) a.put(POSTCARD_BUCKET, `${USER}/${PLACE_A}/${i}.jpg`);
  await deleteAccount(USER, a);
  assert.deepEqual(a.removeCalls.map((c) => c.length), [STORAGE_PAGE, 1]);
});

Deno.test("a user without photos is still deleted, with no remove call", async () => {
  const a = new FakeAdmin();
  const r = await deleteAccount(USER, a);
  assert.equal(r.removedObjects, 0);
  assert.equal(a.removeCalls.length, 0);
  assert.equal(a.users.has(USER), false);
});

Deno.test("storage failure keeps the user (client can retry)", async () => {
  for (const flag of ["failList", "failRemove"] as const) {
    const a = seeded();
    a[flag] = true;
    await assert.rejects(() => deleteAccount(USER, a));
    assert.ok(a.users.has(USER), flag);
  }
});

Deno.test("a non-UUID user id is refused before anything is touched", async () => {
  const a = seeded();
  await assert.rejects(() => deleteAccount("../x", a), /invalid_user_id/);
  assert.equal(a.listCalls.length, 0);
  assert.equal(a.users.size, 2);
});

Deno.test("HTTP: method, auth and confirmation", async () => {
  const a = seeded();
  assert.equal((await handleRequest(new Request("http://localhost/x", { method: "GET" }), deps(a))).status, 405);
  const unauth = await handleRequest(req({ confirm: "HAPUS" }), deps(a, null));
  assert.equal(unauth.status, 401);
  assert.deepEqual(await unauth.json(), { error: "unauthorized" });
  for (const body of [{}, { confirm: "hapus" }]) {
    const r = await handleRequest(req(body), deps(a));
    assert.equal(r.status, 400);
    assert.deepEqual(await r.json(), { error: "bad_request" });
  }
  assert.equal((await handleRequest(req(null, {}, "{not json"), deps(a))).status, 400);
  // Nothing was deleted by any of the refused calls.
  assert.equal(a.users.size, 2);
  assert.equal(a.removeCalls.length, 0);
});

Deno.test("HTTP: deletes the caller and answers 200", async () => {
  const a = seeded();
  const r = await quiet(() => handleRequest(req({ confirm: "HAPUS" }), deps(a)));
  assert.equal(r.status, 200);
  assert.deepEqual(await r.json(), { status: "deleted" });
  assert.equal(a.users.has(USER), false);
});

Deno.test("HTTP: the user id comes from auth, never from the body", async () => {
  const a = seeded();
  await quiet(() => handleRequest(req({ confirm: "HAPUS", userId: OTHER, user_id: OTHER, sub: OTHER }), deps(a)));
  assert.equal(a.users.has(USER), false);
  assert.ok(a.users.has(OTHER));
  assert.ok(a.has(POSTCARD_BUCKET, `${OTHER}/${PLACE_A}/other.jpg`));
});

Deno.test("HTTP: 500 without details when deletion fails, user kept", async () => {
  const a = seeded();
  a.failDeleteUser = true;
  const lines: string[] = [];
  const err = console.error;
  console.error = (...args: unknown[]) => lines.push(args.join(" "));
  try {
    const r = await handleRequest(req({ confirm: "HAPUS" }), deps(a));
    assert.equal(r.status, 500);
    assert.deepEqual(await r.json(), { error: "server_error" });
  } finally {
    console.error = err;
  }
  assert.ok(a.users.has(USER));
  assert.ok(lines.every((l) => !l.includes(USER)), "logs never carry the user id");
});

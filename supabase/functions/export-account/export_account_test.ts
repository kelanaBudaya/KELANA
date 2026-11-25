import assert from "node:assert/strict";
import {
  type AccountUser,
  buildExport,
  EXPORT_FORMAT,
  EXPORT_VERSION,
  type ExportSource,
  isOwnPath,
  PHOTO_URL_TTL_S,
  POSTCARD_BUCKET,
  type PostcardRow,
  REPORT_PHOTO_BUCKET,
  type ReportRow,
  type StampRow,
  type SubscriptionRow,
  type VideoRow,
} from "./export.ts";
import { type Caller, type HandlerDeps, handleRequest } from "./handler.ts";

const USER = "11111111-1111-4111-8111-111111111111";
const OTHER = "99999999-9999-4999-8999-999999999999";
const PLACE_A = "22222222-2222-4222-8222-222222222222";
const PLACE_B = "33333333-3333-4333-8333-333333333333";
const NOW = new Date("2026-11-23T10:00:00.000Z");

const ME: AccountUser = { id: USER, email: "aku@contoh.id", createdAt: "2026-11-01T00:00:00Z" };

/**
 * In-memory source. Like a misconfigured database, it ignores the user id and returns every row (including other
 * users' rows and internal columns), so the tests prove the export filters and allowlists by itself.
 */
class FakeSource implements ExportSource {
  stampRows: StampRow[] = [];
  postcardRows: PostcardRow[] = [];
  videoRows: VideoRow[] = [];
  reportRows: ReportRow[] = [];
  subscriptionRows: SubscriptionRow[] = [];
  profileRow: Record<string, unknown> | null = null;
  fail: string | null = null;
  signCalls: string[] = [];
  failSign = false;

  private read<T>(name: string, rows: T[]): Promise<T[]> {
    return this.fail === name ? Promise.reject(new Error(name)) : Promise.resolve(rows);
  }

  profile() {
    // deno-lint-ignore no-explicit-any
    return this.fail === "profile" ? Promise.reject(new Error("profile")) : Promise.resolve(this.profileRow as any);
  }
  stamps() {
    return this.read("stamps", this.stampRows);
  }
  postcards() {
    return this.read("postcards", this.postcardRows);
  }
  videos() {
    return this.read("videos", this.videoRows);
  }
  reports() {
    return this.read("reports", this.reportRows);
  }
  subscriptions() {
    return this.read("subscriptions", this.subscriptionRows);
  }
  signedUrl(bucket: string, path: string, expiresInS: number): Promise<string | null> {
    this.signCalls.push(`${bucket}:${path}:${expiresInS}`);
    return Promise.resolve(this.failSign ? null : `https://signed.example/${bucket}/${path}?ttl=${expiresInS}`);
  }
}

/** Adds columns the export must never copy (internal flags, storage ids, a location). */
const withInternals = <T>(row: T): T => ({ ...row, is_sample: true, stream_uid: "secret-stream", location: "0101" });

const seeded = () => {
  const s = new FakeSource();
  s.profileRow = {
    id: USER,
    display_name: "Ayu",
    avatar_path: "internal/avatar.png",
    is_sample: false,
    created_at: "2026-11-01T00:00:00Z",
    home_region: { code: "yogyakarta", name: "DI Yogyakarta", status: "available" },
  };
  s.stampRows = [
    withInternals({
      user_id: USER,
      place_id: PLACE_A,
      stamped_at: "2026-11-20T03:00:00Z",
      place: { name: "Keraton", cap_code: "YK-01" },
    }),
    { user_id: OTHER, place_id: PLACE_B, stamped_at: "2026-11-20T04:00:00Z", place: { name: "X", cap_code: "YK-02" } },
  ];
  s.postcardRows = [
    withInternals({
      id: "p1",
      user_id: USER,
      place_id: PLACE_A,
      message: "Indah sekali",
      rating: 5,
      card_style: "cap",
      is_public: false,
      photo_path: `${USER}/${PLACE_A}/one.jpg`,
      created_at: "2026-11-20T05:00:00Z",
      place: { name: "Keraton", cap_code: "YK-01" },
    }),
    {
      id: "p2",
      user_id: USER,
      place_id: PLACE_A,
      message: "Tanpa foto",
      created_at: "2026-11-20T06:00:00Z",
      place: null,
    },
    {
      id: "p-other",
      user_id: OTHER,
      place_id: PLACE_A,
      message: "Punya orang lain",
      is_public: true,
      photo_path: `${OTHER}/${PLACE_A}/x.jpg`,
      created_at: "2026-11-20T07:00:00Z",
    },
  ];
  s.videoRows = [
    withInternals({
      id: "v1",
      user_id: USER,
      place_id: PLACE_A,
      caption: "Gamelan",
      duration_s: 42,
      created_at: "2026-11-20T08:00:00Z",
      place: { name: "Keraton", cap_code: "YK-01" },
    }),
    { id: "v-other", user_id: OTHER, place_id: PLACE_A, caption: "x", created_at: "2026-11-20T08:00:00Z" },
  ];
  s.reportRows = [
    {
      id: "r1",
      user_id: USER,
      place_id: PLACE_B,
      reason: "hours_or_price_changed",
      details: "Tutup lebih awal",
      photo_path: `${USER}/report.png`,
      created_at: "2026-11-21T01:00:00Z",
      place: { name: "Taman Sari", cap_code: "YK-02" },
    },
    { id: "r-anon", user_id: null, reason: "other", created_at: "2026-11-21T02:00:00Z" },
  ];
  s.subscriptionRows = [
    { id: "s1", user_id: USER, category: "tari", created_at: "2026-11-21T03:00:00Z" },
    { id: "s2", user_id: USER, place_id: PLACE_B, created_at: "2026-11-21T04:00:00Z", place: { name: "Taman Sari" } },
    { id: "s-other", user_id: OTHER, category: "candi", created_at: "2026-11-21T05:00:00Z" },
  ];
  return s;
};

const deps = (source: ExportSource, user: AccountUser | null = ME): HandlerDeps => ({
  now: () => NOW,
  authenticate: () => Promise.resolve(user ? ({ user, source } as Caller) : null),
});

const req = (method = "POST", body: unknown = {}) =>
  new Request("http://localhost/export-account", {
    method,
    headers: { authorization: "Bearer test-jwt", "content-type": "application/json" },
    body: method === "GET" ? undefined : JSON.stringify(body),
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

Deno.test("exports the caller's own data with the expected shape", async () => {
  const doc = await buildExport(ME, seeded(), NOW);
  assert.equal(doc.format, EXPORT_FORMAT);
  assert.equal(doc.version, EXPORT_VERSION);
  assert.equal(doc.exportedAt, "2026-11-23T10:00:00.000Z");
  assert.deepEqual(doc.account, { id: USER, email: "aku@contoh.id", createdAt: "2026-11-01T00:00:00Z" });
  assert.deepEqual(doc.profile, {
    displayName: "Ayu",
    homeRegion: { code: "yogyakarta", name: "DI Yogyakarta" },
    createdAt: "2026-11-01T00:00:00Z",
  });
  assert.deepEqual(doc.stamps, [
    { place: { id: PLACE_A, name: "Keraton", capCode: "YK-01" }, stampedAt: "2026-11-20T03:00:00Z" },
  ]);
  assert.deepEqual(doc.postcards.map((p) => [p.id, p.visibility, p.rating, p.cardStyle]), [
    ["p1", "private", 5, "cap"],
    ["p2", "public", null, null],
  ]);
  assert.deepEqual(doc.postcards[1].place, { id: PLACE_A, name: null, capCode: null });
  assert.deepEqual(doc.videos, [{
    id: "v1",
    place: { id: PLACE_A, name: "Keraton", capCode: "YK-01" },
    caption: "Gamelan",
    durationS: 42,
    visibility: "public",
    createdAt: "2026-11-20T08:00:00Z",
  }]);
  assert.deepEqual(doc.reports.map((r) => [r.id, r.reason, r.details, r.place?.name]), [
    ["r1", "hours_or_price_changed", "Tutup lebih awal", "Taman Sari"],
  ]);
  assert.deepEqual(doc.subscriptions.map((s) => [s.id, s.category, s.place?.id ?? null]), [
    ["s1", "tari", null],
    ["s2", null, PLACE_B],
  ]);
});

Deno.test("never includes another user's rows, even if the source returns them", async () => {
  const doc = await buildExport(ME, seeded(), NOW);
  const text = JSON.stringify(doc);
  assert.ok(!text.includes(OTHER), "no other user id anywhere");
  for (const id of ["p-other", "v-other", "s-other", "r-anon", "Punya orang lain"]) {
    assert.ok(!text.includes(id), id);
  }
});

Deno.test("a profile row of someone else is not exported", async () => {
  const s = seeded();
  s.profileRow = { id: OTHER, display_name: "Orang lain" };
  const doc = await buildExport(ME, s, NOW);
  assert.equal(doc.profile, null);
  assert.ok(!JSON.stringify(doc).includes("Orang lain"));
});

Deno.test("internal columns and location never appear", async () => {
  const text = JSON.stringify(await buildExport(ME, seeded(), NOW));
  for (
    const key of [
      "is_sample",
      "isSample",
      "stream_uid",
      "avatar_path",
      "location",
      "lat",
      "lng",
      "photo_path",
      "status",
    ]
  ) {
    assert.ok(!text.includes(`"${key}"`), key);
  }
  for (const value of ["secret-stream", "internal/avatar.png", "0101"]) assert.ok(!text.includes(value), value);
});

Deno.test("photo links are short-lived signed URLs, only for own-folder paths", async () => {
  const s = seeded();
  const doc = await buildExport(ME, s, NOW);
  assert.deepEqual(s.signCalls.sort(), [
    `${POSTCARD_BUCKET}:${USER}/${PLACE_A}/one.jpg:${PHOTO_URL_TTL_S}`,
    `${REPORT_PHOTO_BUCKET}:${USER}/report.png:${PHOTO_URL_TTL_S}`,
  ]);
  assert.ok(PHOTO_URL_TTL_S <= 900, "links live minutes, not days");
  const expires = new Date(NOW.getTime() + PHOTO_URL_TTL_S * 1000).toISOString();
  assert.deepEqual(doc.postcards[0].photo?.expiresAt, expires);
  assert.ok(doc.postcards[0].photo?.url.includes(`${USER}/${PLACE_A}/one.jpg`));
  assert.equal(doc.postcards[1].photo, null);
  assert.equal(doc.reports[0].photo?.expiresAt, expires);
});

Deno.test("a photo path outside the caller's folder is never signed", async () => {
  const s = seeded();
  s.postcardRows = [{
    id: "p1",
    user_id: USER,
    place_id: PLACE_A,
    message: "m",
    created_at: "2026-11-20T05:00:00Z",
    photo_path: `${OTHER}/${PLACE_A}/x.jpg`,
  }, {
    id: "p2",
    user_id: USER,
    place_id: PLACE_A,
    message: "m",
    created_at: "2026-11-20T05:00:00Z",
    photo_path: `${USER}/../${OTHER}/x.jpg`,
  }];
  s.reportRows = [];
  const doc = await buildExport(ME, s, NOW);
  assert.equal(s.signCalls.length, 0);
  assert.ok(doc.postcards.every((p) => p.photo === null));
});

Deno.test("isOwnPath", () => {
  assert.ok(isOwnPath(USER, `${USER}/a/b.jpg`));
  for (
    const bad of [null, undefined, "", USER, `${USER}`, `${OTHER}/a.jpg`, `${USER}/../x`, `${USER}//x`, `x/${USER}/a`]
  ) {
    assert.equal(isOwnPath(USER, bad), false, String(bad));
  }
});

Deno.test("a failed signing keeps the export, without the link", async () => {
  const s = seeded();
  s.failSign = true;
  const doc = await buildExport(ME, s, NOW);
  assert.equal(doc.postcards[0].photo, null);
  assert.equal(doc.reports[0].photo, null);
});

Deno.test("a failed read fails the whole export", async () => {
  for (const name of ["profile", "stamps", "postcards", "videos", "reports", "subscriptions"]) {
    const s = seeded();
    s.fail = name;
    await assert.rejects(() => buildExport(ME, s, NOW), Error, name);
  }
});

Deno.test("a non-UUID user id is refused before anything is read", async () => {
  const s = seeded();
  s.fail = "stamps";
  await assert.rejects(() => buildExport({ ...ME, id: "../x" }, s, NOW), /invalid_user_id/);
});

Deno.test("an empty account exports empty lists", async () => {
  const doc = await buildExport(ME, new FakeSource(), NOW);
  assert.equal(doc.profile, null);
  assert.deepEqual([doc.stamps, doc.postcards, doc.videos, doc.reports, doc.subscriptions], [[], [], [], [], []]);
});

Deno.test("HTTP: method and auth", async () => {
  const s = seeded();
  assert.equal((await handleRequest(req("GET"), deps(s))).status, 405);
  const unauth = await handleRequest(req(), deps(s, null));
  assert.equal(unauth.status, 401);
  assert.deepEqual(await unauth.json(), { error: "unauthorized" });
});

Deno.test("HTTP: 200 with the document, not cacheable", async () => {
  const r = await quiet(() => handleRequest(req(), deps(seeded())));
  assert.equal(r.status, 200);
  assert.equal(r.headers.get("cache-control"), "no-store");
  assert.match(r.headers.get("content-type") ?? "", /application\/json/);
  const body = await r.json();
  assert.equal(body.format, EXPORT_FORMAT);
  assert.equal(body.account.id, USER);
});

Deno.test("HTTP: the user comes from auth, never from the body", async () => {
  const r = await quiet(() =>
    handleRequest(req("POST", { userId: OTHER, user_id: OTHER, sub: OTHER }), deps(seeded()))
  );
  const body = await r.json();
  assert.equal(body.account.id, USER);
  assert.ok(!JSON.stringify(body).includes(OTHER));
});

Deno.test("HTTP: 500 without details when a read fails", async () => {
  const s = seeded();
  s.fail = "postcards";
  const lines: string[] = [];
  const err = console.error;
  console.error = (...args: unknown[]) => lines.push(args.join(" "));
  try {
    const r = await handleRequest(req(), deps(s));
    assert.equal(r.status, 500);
    assert.deepEqual(await r.json(), { error: "server_error" });
  } finally {
    console.error = err;
  }
  assert.ok(lines.every((l) => !l.includes(USER) && !l.includes("aku@contoh.id")), "logs carry no personal data");
});

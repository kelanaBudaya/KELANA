import assert from "node:assert/strict";
import { handleRequest } from "./handler.ts";
import {
  DEV_TOKEN,
  DevStubVerifier,
  FailClosedVerifier,
  GoogleIntegrityVerifier,
  payloadIsValid,
  verifierFromEnv,
} from "./integrity.ts";
import {
  canonicalString,
  computeNonce,
  decide,
  type Deps,
  type DistanceRow,
  MAX_ACCURACY_M,
  MAX_CAPTURE_AGE_MS,
  MAX_FUTURE_SKEW_MS,
  type StampDb,
} from "./verify.ts";

const NOW = new Date("2026-11-21T10:00:00.000Z");
const USER = "11111111-1111-4111-8111-111111111111";
const PLACE = "22222222-2222-4222-8222-222222222222";
const LAT = -7.805;
const LNG = 110.364;

const iso = (offsetMs: number) => new Date(NOW.getTime() + offsetMs).toISOString();
const goodBody = (over: Record<string, unknown> = {}) => ({
  placeId: PLACE,
  lat: LAT,
  lng: LNG,
  accuracyM: 20,
  capturedAt: iso(-60_000),
  isMock: false,
  integrityToken: DEV_TOKEN,
  ...over,
});

class FakeDb implements StampDb {
  stamps = new Set<string>();
  inserts = 0;
  row: DistanceRow | null = { distance_m: 10, cap_radius_m: 50, within_radius: true };
  raceExists = false;
  fail = false;
  distance() {
    if (this.fail) return Promise.reject(new Error("boom"));
    return Promise.resolve(this.row);
  }
  hasStamp(u: string, p: string) {
    return Promise.resolve(this.stamps.has(`${u}|${p}`));
  }
  insertStamp(u: string, p: string) {
    if (this.raceExists || this.stamps.has(`${u}|${p}`)) return Promise.resolve("exists" as const);
    this.stamps.add(`${u}|${p}`);
    this.inserts++;
    return Promise.resolve("inserted" as const);
  }
}

function deps(db = new FakeDb(), verifier = new DevStubVerifier() as Deps["verifier"]): Deps & { db: FakeDb } {
  return { now: () => NOW, db, verifier } as Deps & { db: FakeDb };
}

// ---- reject reasons ----
Deno.test("stamped on the happy path", async () => {
  const d = deps();
  assert.deepEqual(await decide(goodBody(), USER, d), { status: "stamped" });
  assert.equal(d.db.inserts, 1);
});

Deno.test("bad_request for malformed bodies", async () => {
  const bad: unknown[] = [
    null,
    "x",
    [],
    {},
    goodBody({ placeId: "nope" }),
    goodBody({ lat: "1" }),
    goodBody({ lat: 91 }),
    goodBody({ lng: -181 }),
    goodBody({ accuracyM: "5" }),
    goodBody({ capturedAt: "2026-11-21T09:59:00Z" }),
    goodBody({ capturedAt: "not a date" }),
    goodBody({ isMock: "false" }),
    goodBody({ isMock: undefined }),
    goodBody({ integrityToken: "" }),
    goodBody({ integrityToken: 5 }),
  ];
  for (const b of bad) {
    assert.deepEqual(await decide(b, USER, deps()), { status: "rejected", reason: "bad_request" }, JSON.stringify(b));
  }
});

Deno.test("mock_location", async () => {
  const d = deps();
  assert.deepEqual(await decide(goodBody({ isMock: true }), USER, d), { status: "rejected", reason: "mock_location" });
  assert.equal(d.db.inserts, 0);
});

Deno.test("low_accuracy and its boundary", async () => {
  assert.equal((await decide(goodBody({ accuracyM: MAX_ACCURACY_M }), USER, deps())).status, "stamped");
  assert.equal((await decide(goodBody({ accuracyM: 100.1 }), USER, deps())).reason, "low_accuracy");
  assert.equal((await decide(goodBody({ accuracyM: -1 }), USER, deps())).reason, "low_accuracy");
  assert.equal((await decide(goodBody({ accuracyM: 0 }), USER, deps())).status, "stamped");
});

Deno.test("expired and its boundary (exactly 30 min is accepted)", async () => {
  assert.equal(MAX_CAPTURE_AGE_MS, 30 * 60 * 1000);
  assert.equal((await decide(goodBody({ capturedAt: iso(-MAX_CAPTURE_AGE_MS) }), USER, deps())).status, "stamped");
  assert.deepEqual(
    await decide(goodBody({ capturedAt: iso(-MAX_CAPTURE_AGE_MS - 1) }), USER, deps()),
    { status: "rejected", reason: "expired" },
  );
});

Deno.test("future_timestamp and its boundary (exactly 2 min ahead accepted)", async () => {
  assert.equal((await decide(goodBody({ capturedAt: iso(MAX_FUTURE_SKEW_MS) }), USER, deps())).status, "stamped");
  assert.deepEqual(
    await decide(goodBody({ capturedAt: iso(MAX_FUTURE_SKEW_MS + 1) }), USER, deps()),
    { status: "rejected", reason: "future_timestamp" },
  );
});

Deno.test("integrity_failed for a wrong token, a throwing verifier, and a false verifier", async () => {
  assert.equal((await decide(goodBody({ integrityToken: "other" }), USER, deps())).reason, "integrity_failed");
  const throwing = { verify: () => Promise.reject(new Error("x")) };
  assert.equal((await decide(goodBody(), USER, deps(new FakeDb(), throwing))).reason, "integrity_failed");
  assert.equal(
    (await decide(goodBody(), USER, deps(new FakeDb(), new FailClosedVerifier()))).reason,
    "integrity_failed",
  );
});

Deno.test("verifier receives the nonce of the request", async () => {
  let seen = "";
  const v = { verify: (_t: string, n: string) => (seen = n, Promise.resolve(true)) };
  const body = goodBody();
  await decide(body, USER, deps(new FakeDb(), v));
  assert.equal(seen, await computeNonce(body as never));
});

Deno.test("place_unknown, radius_not_set (never stamps), too_far with distanceM", async () => {
  const d1 = deps();
  d1.db.row = null;
  assert.equal((await decide(goodBody(), USER, d1)).reason, "place_unknown");

  const d2 = deps();
  d2.db.row = { distance_m: 0, cap_radius_m: null, within_radius: null };
  assert.deepEqual(await decide(goodBody(), USER, d2), { status: "rejected", reason: "radius_not_set" });
  assert.equal(d2.db.inserts, 0);

  const d3 = deps();
  d3.db.row = { distance_m: 51.4, cap_radius_m: 50, within_radius: false };
  assert.deepEqual(await decide(goodBody(), USER, d3), { status: "rejected", reason: "too_far", distanceM: 51 });
  assert.equal(d3.db.inserts, 0);
});

Deno.test("radius edge: within_radius from the RPC decides (distance == radius stamps)", async () => {
  const d = deps();
  d.db.row = { distance_m: 50, cap_radius_m: 50, within_radius: true };
  assert.equal((await decide(goodBody(), USER, d)).status, "stamped");
});

Deno.test("distanceM only appears on too_far", async () => {
  const r = await decide(goodBody({ isMock: true }), USER, deps());
  assert.equal("distanceM" in r, false);
  assert.equal("distanceM" in (await decide(goodBody(), USER, deps())), false);
});

// ---- idempotency ----
Deno.test("second request is already_stamped, one row only", async () => {
  const d = deps();
  assert.equal((await decide(goodBody(), USER, d)).status, "stamped");
  assert.deepEqual(await decide(goodBody(), USER, d), { status: "already_stamped" });
  assert.equal(d.db.inserts, 1);
});

Deno.test("unique-violation race maps to already_stamped", async () => {
  const d = deps();
  d.db.raceExists = true;
  assert.deepEqual(await decide(goodBody(), USER, d), { status: "already_stamped" });
});

Deno.test("DB fault throws (handler maps to 500)", async () => {
  const d = deps();
  d.db.fail = true;
  await assert.rejects(decide(goodBody(), USER, d));
});

// ---- nonce ----
Deno.test("canonical format is fixed and documented", () => {
  assert.equal(
    canonicalString({
      placeId: PLACE,
      lat: -7.805,
      lng: 110.364,
      accuracyM: 20,
      capturedAt: "2026-11-21T09:59:00.000Z",
    }),
    `${PLACE}|-7.805000|110.364000|20.0|2026-11-21T09:59:00.000Z`,
  );
  assert.equal(
    canonicalString({ placeId: PLACE, lat: -0.0000001, lng: 0, accuracyM: 4.96, capturedAt: "x" }),
    `${PLACE}|0.000000|0.000000|5.0|x`,
  );
});

Deno.test("nonce is base64url(sha256(canonical)) with no padding", async () => {
  const base = { placeId: PLACE, lat: LAT, lng: LNG, accuracyM: 20, capturedAt: iso(-1000) };
  const n = await computeNonce(base);
  assert.match(n, /^[A-Za-z0-9_-]{43}$/);
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(canonicalString(base)));
  const expect = btoa(String.fromCharCode(...new Uint8Array(d))).replaceAll("+", "-").replaceAll("/", "_").replace(
    /=+$/,
    "",
  );
  assert.equal(n, expect);
});

Deno.test("changing any bound field changes the nonce", async () => {
  const base = { placeId: PLACE, lat: LAT, lng: LNG, accuracyM: 20, capturedAt: iso(-1000) };
  const n0 = await computeNonce(base);
  const variants = [
    { ...base, placeId: "33333333-3333-4333-8333-333333333333" },
    { ...base, lat: LAT + 0.000001 },
    { ...base, lng: LNG + 0.000001 },
    { ...base, accuracyM: 20.1 },
    { ...base, capturedAt: iso(-999) },
  ];
  for (const v of variants) assert.notEqual(await computeNonce(v), n0);
});

// ---- integrity ----
Deno.test("fail closed without secret; stub only with KELANA_ENV=dev", async () => {
  const env = (m: Record<string, string>) => ({ get: (k: string) => m[k] });
  const closed = verifierFromEnv(env({}));
  assert.ok(closed instanceof FailClosedVerifier);
  assert.equal(await closed.verify(DEV_TOKEN, "n"), false);
  assert.ok(verifierFromEnv(env({ KELANA_ENV: "prod" })) instanceof FailClosedVerifier);
  const dev = verifierFromEnv(env({ KELANA_ENV: "dev" }));
  assert.equal(await dev.verify(DEV_TOKEN, "n"), true);
  assert.equal(await dev.verify("x", "n"), false);
  assert.ok(verifierFromEnv(env({ PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON: "{}" })) instanceof GoogleIntegrityVerifier);
});

Deno.test("payloadIsValid checks nonce, package and verdicts", () => {
  const ok = {
    requestDetails: { nonce: "N", requestPackageName: "id.kelana.app" },
    appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED", packageName: "id.kelana.app" },
    deviceIntegrity: { deviceRecognitionVerdict: ["MEETS_DEVICE_INTEGRITY"] },
  };
  assert.equal(payloadIsValid(ok, "N"), true);
  assert.equal(payloadIsValid(ok, "other"), false);
  assert.equal(payloadIsValid({ ...ok, requestDetails: { nonce: "N", requestPackageName: "evil.app" } }, "N"), false);
  assert.equal(
    payloadIsValid({ ...ok, appIntegrity: { ...ok.appIntegrity, appRecognitionVerdict: "UNRECOGNIZED_VERSION" } }, "N"),
    false,
  );
  assert.equal(payloadIsValid({ ...ok, deviceIntegrity: { deviceRecognitionVerdict: [] } }, "N"), false);
  assert.equal(payloadIsValid(null, "N"), false);
});

Deno.test("GoogleIntegrityVerifier fails closed when the network call fails (fake fetch)", async () => {
  const v = new GoogleIntegrityVerifier("not json", "id.kelana.app", () => Promise.reject(new Error("no net")));
  assert.equal(await v.verify("t", "n"), false);
});

// ---- HTTP + logging ----
function req(body: unknown, headers: Record<string, string> = {}, raw?: string) {
  return new Request("http://x/verify-stamp", {
    method: "POST",
    headers: { "content-type": "application/json", ...headers },
    body: raw ?? JSON.stringify(body),
  });
}
const hdeps = (db = new FakeDb(), user: string | null = USER) => ({
  ...deps(db),
  authenticate: () => Promise.resolve(user),
});

Deno.test("401 without a valid user, 400 for malformed, 200 otherwise", async () => {
  assert.equal((await handleRequest(req(goodBody()), hdeps(new FakeDb(), null))).status, 401);
  const bad = await handleRequest(req(null, {}, "{not json"), hdeps());
  assert.equal(bad.status, 400);
  assert.equal((await bad.json()).reason, "bad_request");
  const ok = await handleRequest(req(goodBody()), hdeps());
  assert.equal(ok.status, 200);
  assert.deepEqual(await ok.json(), { status: "stamped" });
  assert.equal((await handleRequest(req(goodBody({ accuracyM: 500 })), hdeps())).status, 200);
});

Deno.test("user id comes from auth, never from the body", async () => {
  const db = new FakeDb();
  await handleRequest(req(goodBody({ userId: "99999999-9999-4999-8999-999999999999" })), hdeps(db));
  assert.ok(db.stamps.has(`${USER}|${PLACE}`));
  assert.equal(db.stamps.size, 1);
});

Deno.test("500 on DB fault, body without details", async () => {
  const db = new FakeDb();
  db.fail = true;
  const originalError = console.error;
  console.error = () => {};
  try {
    const r = await handleRequest(req(goodBody()), hdeps(db));
    assert.equal(r.status, 500);
    assert.deepEqual(await r.json(), { error: "server_error" });
  } finally {
    console.error = originalError;
  }
});

Deno.test("no lat, lng or accuracy ever reaches console output", async () => {
  const lines: string[] = [];
  const saved = {
    log: console.log,
    error: console.error,
    warn: console.warn,
    info: console.info,
    debug: console.debug,
  };
  const spy = (...a: unknown[]) => void lines.push(a.map(String).join(" "));
  console.log =
    console.error =
    console.warn =
    console.info =
    console.debug =
      spy;
  try {
    const body = goodBody({ lat: -7.805123, lng: 110.364987, accuracyM: 17.3 });
    await handleRequest(req(body), hdeps());
    await handleRequest(req(body), hdeps()); // already_stamped
    await handleRequest(req({ ...body, accuracyM: 900 }), hdeps());
    await handleRequest(req({ ...body, isMock: true }), hdeps());
    const far = new FakeDb();
    far.row = { distance_m: 4321.5, cap_radius_m: 50, within_radius: false };
    await handleRequest(req(body), hdeps(far));
    const broken = new FakeDb();
    broken.fail = true;
    await handleRequest(req(body), hdeps(broken));
  } finally {
    Object.assign(console, saved);
  }
  assert.ok(lines.length >= 5);
  const all = lines.join("\n");
  for (const secret of ["-7.805123", "110.364987", "17.3", "7.805", "110.364", PLACE, DEV_TOKEN, "4321"]) {
    assert.ok(!all.includes(secret), `leaked ${secret}`);
  }
  for (const l of lines) {
    const j = JSON.parse(l);
    assert.deepEqual(Object.keys(j).filter((k) => !["fn", "status", "reason", "fault"].includes(k)), []);
  }
});

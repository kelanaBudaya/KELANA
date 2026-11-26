import assert from "node:assert/strict";
import { handleRequest, type HandlerDeps } from "./handler.ts";
import { cacheKey, isFresh, type Leg, mapValhalla, parseRequest, Throttle, valhallaBody } from "./route.ts";

const P = [{ lat: -7.8052, lng: 110.3642 }, { lat: -7.752, lng: 110.4914 }];
const good = { profile: "car", points: P };

Deno.test("parseRequest accepts valid and rejects invalid input", () => {
  assert.ok(parseRequest(good));
  for (
    const bad of [
      undefined,
      null,
      {},
      { profile: "bike", points: P },
      { profile: "car", points: [P[0]] },
      { profile: "car", points: Array(11).fill(P[0]) },
      { profile: "car", points: [P[0], { lat: 7, lng: 110 }] },
      { profile: "car", points: [P[0], { lat: -7, lng: 94.9 }] },
      { profile: "car", points: [P[0], { lat: "-7", lng: 110 }] },
      { profile: "car", points: [P[0], { lat: NaN, lng: 110 }] },
      { profile: "car", points: [P[0], null] },
    ]
  ) assert.equal(parseRequest(bad), null);
  assert.ok(parseRequest({ profile: "walk", points: Array(10).fill(P[0]) }));
});

Deno.test("cacheKey rounds to 5 decimals, is ordered and profile-specific", async () => {
  const a = await cacheKey("car", P);
  assert.match(a, /^[0-9a-f]{64}$/);
  const near = P.map((p) => ({ lat: p.lat + 0.000001, lng: p.lng - 0.000001 }));
  assert.equal(await cacheKey("car", near), a);
  assert.notEqual(await cacheKey("walk", P), a);
  assert.notEqual(await cacheKey("car", [...P].reverse()), a);
});

Deno.test("valhallaBody maps costing and lon", () => {
  const b = valhallaBody("motorcycle", P);
  assert.equal(b.costing, "motor_scooter");
  assert.equal(valhallaBody("walk", P).costing, "pedestrian");
  assert.equal(valhallaBody("car", P).costing, "auto");
  assert.deepEqual(b.locations[0], { lat: -7.8052, lon: 110.3642 });
  assert.equal(b.directions_type, "none");
});

const trip = (legs: unknown[]) => ({ trip: { legs } });
const leg = { shape: "abc", summary: { time: 100.4, length: 1.2346 } };

Deno.test("mapValhalla converts km to meters and validates", () => {
  assert.deepEqual(mapValhalla(trip([leg]), 2), [{ seconds: 100, meters: 1235, polyline: "abc" }]);
  assert.equal(mapValhalla(trip([leg]), 3), null);
  assert.equal(mapValhalla({}, 2), null);
  assert.equal(mapValhalla(null, 2), null);
  assert.equal(mapValhalla(trip([{ ...leg, shape: 1 }]), 2), null);
  assert.equal(mapValhalla(trip([{ shape: "a", summary: { time: "x", length: 1 } }]), 2), null);
});

Deno.test("isFresh honours 30-day TTL", () => {
  const now = new Date("2026-11-26T00:00:00Z");
  assert.equal(isFresh("2026-11-01T00:00:00Z", now), true);
  assert.equal(isFresh("2026-10-01T00:00:00Z", now), false);
  assert.equal(isFresh("garbage", now), false);
});

Deno.test("Throttle spaces calls by 1 s and refuses when wait > 3 s", async () => {
  let t = 0;
  const slept: number[] = [];
  const th = new Throttle(() => t, (ms) => {
    slept.push(ms);
    return Promise.resolve();
  });
  assert.equal(await th.acquire(), true); // slot 0
  assert.equal(await th.acquire(), true); // slot 1000
  assert.equal(await th.acquire(), true); // 2000
  assert.equal(await th.acquire(), true); // 3000
  assert.equal(await th.acquire(), false); // 4000 > 3000 wait
  assert.deepEqual(slept, [1000, 2000, 3000]);
  t = 10_000;
  assert.equal(await th.acquire(), true);
});

function deps(over: Partial<HandlerDeps> = {}) {
  const calls = { fetch: 0, put: 0 };
  const d: HandlerDeps = {
    cacheGet: () => Promise.resolve(null),
    cachePut: () => {
      calls.put++;
      return Promise.resolve();
    },
    acquire: () => Promise.resolve(true),
    fetch: () => {
      calls.fetch++;
      return Promise.resolve(new Response(JSON.stringify(trip([leg])), { status: 200 }));
    },
    ...over,
  };
  return { d, calls };
}
const req = (body: unknown, method = "POST") =>
  new Request("http://x/route", { method, body: method === "POST" ? JSON.stringify(body) : undefined });

Deno.test("handler: valhalla path, caches result", async () => {
  const { d, calls } = deps();
  const res = await handleRequest(req(good), d);
  assert.equal(res.status, 200);
  const j = await res.json();
  assert.equal(j.source, "valhalla");
  assert.equal(j.legs.length, 1);
  assert.equal(calls.put, 1);
});

Deno.test("handler: cache hit skips throttle and upstream", async () => {
  const cached: Leg[] = [{ seconds: 1, meters: 2, polyline: "z" }];
  const { d, calls } = deps({
    cacheGet: () => Promise.resolve(cached),
    acquire: () => Promise.reject(new Error("must not throttle")),
  });
  const j = await (await handleRequest(req(good), d)).json();
  assert.deepEqual(j, { legs: cached, source: "cache" });
  assert.equal(calls.fetch, 0);
});

Deno.test("handler: 400 invalid, 405, 429, 502 variants", async () => {
  const { d } = deps();
  const bad = await handleRequest(req({ profile: "x", points: P }), d);
  assert.equal(bad.status, 400);
  assert.deepEqual(await bad.json(), { error: "invalid" });
  const notJson = await handleRequest(new Request("http://x", { method: "POST", body: "nope" }), d);
  assert.equal(notJson.status, 400);
  assert.equal((await handleRequest(req(null, "GET"), d)).status, 405);

  const thr = await handleRequest(req(good), deps({ acquire: () => Promise.resolve(false) }).d);
  assert.equal(thr.status, 429);
  assert.deepEqual(await thr.json(), { error: "throttled" });

  for (
    const f of [
      () => Promise.resolve(new Response("secret upstream body", { status: 500 })),
      () => Promise.reject(new Error("timeout")),
      () => Promise.resolve(new Response(JSON.stringify({ trip: {} }), { status: 200 })),
    ]
  ) {
    const r = await handleRequest(req(good), deps({ fetch: f }).d);
    assert.equal(r.status, 502);
    assert.equal(await r.text(), JSON.stringify({ error: "upstream" }));
  }
});

Deno.test("handler: cache faults do not break the response", async () => {
  const { d } = deps({
    cacheGet: () => Promise.reject(new Error("db")),
    cachePut: () => Promise.reject(new Error("db")),
  });
  assert.equal((await handleRequest(req(good), d)).status, 200);
});

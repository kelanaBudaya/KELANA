// HTTP layer with injected cache/upstream/throttle so it is testable without network.
import { cacheKey, type Leg, mapValhalla, parseRequest, type Point, type Profile, valhallaBody } from "./route.ts";

export interface HandlerDeps {
  /** Fresh (within TTL) cached legs for the key, or null. Throws on DB fault. */
  cacheGet(key: string): Promise<Leg[] | null>;
  /** Upsert (overwrites stale rows). */
  cachePut(key: string, profile: Profile, legs: Leg[]): Promise<void>;
  /** Reserve an upstream slot; false = would wait too long. */
  acquire(): Promise<boolean>;
  fetch(url: string, init: RequestInit): Promise<Response>;
}

export const VALHALLA_URL = "https://valhalla1.openstreetmap.de/route";
export const USER_AGENT = "KelanaApp/1.0 (id.kelana.app; route-proxy)";
const TIMEOUT_MS = 8000;

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

// Logs only a status/source label: never coordinates, never caller identity.
const log = (status: number, source: string) => console.log(JSON.stringify({ fn: "route", status, source }));

async function upstream(profile: Profile, points: Point[], deps: HandlerDeps): Promise<Leg[] | null> {
  try {
    const res = await deps.fetch(VALHALLA_URL, {
      method: "POST",
      headers: { "content-type": "application/json", "user-agent": USER_AGENT },
      body: JSON.stringify(valhallaBody(profile, points)),
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!res.ok) {
      await res.body?.cancel();
      return null;
    }
    return mapValhalla(await res.json(), points.length);
  } catch {
    return null;
  }
}

export async function handleRequest(req: Request, deps: HandlerDeps): Promise<Response> {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    body = undefined;
  }
  const parsed = parseRequest(body);
  if (!parsed) {
    log(400, "invalid");
    return json({ error: "invalid" }, 400);
  }
  const { profile, points } = parsed;
  const key = await cacheKey(profile, points);

  try {
    const hit = await deps.cacheGet(key);
    if (hit && hit.length === points.length - 1) {
      log(200, "cache");
      return json({ legs: hit, source: "cache" }, 200);
    }
  } catch {
    // Cache is an optimisation: a read fault falls through to upstream.
    console.error(JSON.stringify({ fn: "route", fault: "cache_read" }));
  }

  if (!(await deps.acquire())) {
    log(429, "throttled");
    return json({ error: "throttled" }, 429);
  }
  const legs = await upstream(profile, points, deps);
  if (!legs) {
    log(502, "upstream");
    return json({ error: "upstream" }, 502);
  }
  try {
    await deps.cachePut(key, profile, legs);
  } catch {
    console.error(JSON.stringify({ fn: "route", fault: "cache_write" }));
  }
  log(200, "valhalla");
  return json({ legs, source: "valhalla" }, 200);
}

// Pure helpers for the `route` function: validation, cache key, Valhalla request/response mapping, throttle.
export type Profile = "car" | "motorcycle" | "walk";
export interface Point {
  lat: number;
  lng: number;
}
export interface Leg {
  seconds: number;
  meters: number;
  polyline: string;
}
export interface RouteRequest {
  profile: Profile;
  points: Point[];
}

export const MIN_POINTS = 2;
export const MAX_POINTS = 10;
export const CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000;

const COSTING: Record<Profile, string> = { car: "auto", motorcycle: "motor_scooter", walk: "pedestrian" };
export const costingFor = (p: Profile): string => COSTING[p];

const num = (v: unknown, lo: number, hi: number): v is number =>
  typeof v === "number" && Number.isFinite(v) && v >= lo && v <= hi;

/** Returns the typed request, or null when anything is off (profile, count, finite numbers, Indonesia bbox). */
export function parseRequest(body: unknown): RouteRequest | null {
  if (typeof body !== "object" || body === null) return null;
  const { profile, points } = body as Record<string, unknown>;
  if (profile !== "car" && profile !== "motorcycle" && profile !== "walk") return null;
  if (!Array.isArray(points) || points.length < MIN_POINTS || points.length > MAX_POINTS) return null;
  const out: Point[] = [];
  for (const p of points) {
    if (typeof p !== "object" || p === null) return null;
    const { lat, lng } = p as Record<string, unknown>;
    if (!num(lat, -11, 6) || !num(lng, 95, 141)) return null;
    out.push({ lat, lng });
  }
  return { profile, points: out };
}

/** sha256 hex of `profile|lat,lng|lat,lng...` with coordinates rounded to 5 decimals, in order. */
export async function cacheKey(profile: Profile, points: Point[]): Promise<string> {
  const text = profile + "|" + points.map((p) => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`).join("|");
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, "0")).join("");
}

export function valhallaBody(profile: Profile, points: Point[]) {
  return {
    locations: points.map((p) => ({ lat: p.lat, lon: p.lng })),
    costing: costingFor(profile),
    directions_type: "none",
  };
}

/** Maps a Valhalla trip to legs, or null if it is malformed or the leg count is wrong. */
// deno-lint-ignore no-explicit-any
export function mapValhalla(json: any, pointCount: number): Leg[] | null {
  const legs = json?.trip?.legs;
  if (!Array.isArray(legs) || legs.length !== pointCount - 1) return null;
  const out: Leg[] = [];
  for (const l of legs) {
    const shape = l?.shape;
    const time = l?.summary?.time;
    const km = l?.summary?.length;
    if (typeof shape !== "string" || shape === "") return null;
    if (typeof time !== "number" || !Number.isFinite(time) || time < 0) return null;
    if (typeof km !== "number" || !Number.isFinite(km) || km < 0) return null;
    out.push({ seconds: Math.round(time), meters: Math.round(km * 1000), polyline: shape });
  }
  return out;
}

/** True when a cached row is still within the TTL. */
export const isFresh = (createdAt: string, now: Date): boolean => {
  const t = Date.parse(createdAt);
  return Number.isFinite(t) && now.getTime() - t < CACHE_TTL_MS;
};

/**
 * Upstream guard for Valhalla's fair use (~1 request/s). Each uncached call reserves the next free
 * 1 s slot on a per-instance timeline. If the slot is more than maxWaitMs away it is refused (-> 429),
 * otherwise the caller sleeps until its slot. Reserving before sleeping keeps concurrent requests
 * spaced out. Cache hits never go through this.
 */
export class Throttle {
  private next = 0;
  constructor(
    private now: () => number,
    private sleep: (ms: number) => Promise<void>,
    private intervalMs = 1000,
    private maxWaitMs = 3000,
  ) {}
  async acquire(): Promise<boolean> {
    const t = this.now();
    const slot = Math.max(t, this.next);
    const wait = slot - t;
    if (wait > this.maxWaitMs) return false;
    this.next = slot + this.intervalMs;
    if (wait > 0) await this.sleep(wait);
    return true;
  }
}

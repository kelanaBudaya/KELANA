// Pure logic for verify-stamp: validation, time window, accuracy, nonce, decision.
// No network, no Deno APIs except Web Crypto. Location values are never logged or stored here.

// ---- PROVISIONAL decisions (see supabase/README.md). Change here only. ----
/** A fix older than this is rejected ("expired"). Matches the 30-minute client queue expiry. */
export const MAX_CAPTURE_AGE_MS = 30 * 60 * 1000;
/** A capture time further in the future than this is rejected ("future_timestamp"). */
export const MAX_FUTURE_SKEW_MS = 2 * 60 * 1000;
/** Fixes with accuracy worse (greater) than this many metres are rejected ("low_accuracy"). */
export const MAX_ACCURACY_M = 100;
/** Android package name the Play Integrity token must belong to. */
export const EXPECTED_PACKAGE_NAME = "com.gajahbasudara.kelana";

export type Reason =
  | "too_far"
  | "expired"
  | "future_timestamp"
  | "low_accuracy"
  | "mock_location"
  | "integrity_failed"
  | "place_unknown"
  | "radius_not_set"
  | "bad_request";

export interface VerifyResult {
  status: "stamped" | "already_stamped" | "rejected";
  reason?: Reason;
  /** Only set when reason is "too_far". */
  distanceM?: number;
}

export interface StampRequest {
  placeId: string;
  lat: number;
  lng: number;
  accuracyM: number;
  capturedAt: string;
  isMock: boolean;
  integrityToken: string;
}

export interface IntegrityVerifier {
  /** True only if the token is valid, bound to expectedNonce, for our app and a genuine device. */
  verify(token: string, expectedNonce: string): Promise<boolean>;
}

export interface DistanceRow {
  distance_m: number;
  cap_radius_m: number | null;
  within_radius: boolean | null;
}

export interface StampDb {
  /** Row from public.stamp_distance, or null when the place does not exist. */
  distance(placeId: string, lat: number, lng: number): Promise<DistanceRow | null>;
  hasStamp(userId: string, placeId: string): Promise<boolean>;
  /** "exists" when the unique (user_id, place_id) constraint already holds a row. */
  insertStamp(userId: string, placeId: string): Promise<"inserted" | "exists">;
}

export interface Deps {
  now: () => Date;
  db: StampDb;
  verifier: IntegrityVerifier;
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
// Exactly UTC with milliseconds: 2026-11-21T10:15:30.123Z
const CAPTURED_AT_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;

export function parseRequest(body: unknown): StampRequest | null {
  if (typeof body !== "object" || body === null || Array.isArray(body)) return null;
  const b = body as Record<string, unknown>;
  const { placeId, lat, lng, accuracyM, capturedAt, isMock, integrityToken } = b;
  if (typeof placeId !== "string" || !UUID_RE.test(placeId)) return null;
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90) return null;
  if (typeof lng !== "number" || !Number.isFinite(lng) || lng < -180 || lng > 180) return null;
  // accuracyM must be a number; non-finite or negative values are judged later (low_accuracy).
  if (typeof accuracyM !== "number") return null;
  if (typeof capturedAt !== "string" || !CAPTURED_AT_RE.test(capturedAt)) return null;
  if (Number.isNaN(Date.parse(capturedAt))) return null;
  if (typeof isMock !== "boolean") return null;
  if (typeof integrityToken !== "string" || integrityToken.length === 0) return null;
  return { placeId, lat, lng, accuracyM, capturedAt, isMock, integrityToken };
}

/** Fixed-point text; "-0.000000" is normalised to "0.000000" so the format is unambiguous. */
function fixed(n: number, digits: number): string {
  const s = n.toFixed(digits);
  return /^-0(\.0+)?$/.test(s) ? s.slice(1) : s;
}

/** placeId|lat(6 decimals)|lng(6 decimals)|accuracyM(1 decimal)|capturedAt(as sent) */
export function canonicalString(
  r: Pick<StampRequest, "placeId" | "lat" | "lng" | "accuracyM" | "capturedAt">,
): string {
  return [r.placeId, fixed(r.lat, 6), fixed(r.lng, 6), fixed(r.accuracyM, 1), r.capturedAt].join("|");
}

function b64url(bytes: Uint8Array): string {
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

/** base64url (no padding) of SHA-256 over the UTF-8 canonical string. */
export async function computeNonce(r: Parameters<typeof canonicalString>[0]): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(canonicalString(r)));
  return b64url(new Uint8Array(digest));
}

export function checkAccuracy(accuracyM: number): boolean {
  return Number.isFinite(accuracyM) && accuracyM >= 0 && accuracyM <= MAX_ACCURACY_M;
}

export function checkWindow(capturedAt: string, now: Date): "ok" | "expired" | "future_timestamp" {
  const t = Date.parse(capturedAt);
  if (now.getTime() - t > MAX_CAPTURE_AGE_MS) return "expired";
  if (t - now.getTime() > MAX_FUTURE_SKEW_MS) return "future_timestamp";
  return "ok";
}

const reject = (reason: Reason, extra: Partial<VerifyResult> = {}): VerifyResult => ({
  status: "rejected",
  reason,
  ...extra,
});

/** Decide one stamp request. Throws only on DB faults (caller maps to 500). */
export async function decide(body: unknown, userId: string, deps: Deps): Promise<VerifyResult> {
  const req = parseRequest(body);
  if (!req) return reject("bad_request");
  if (req.isMock) return reject("mock_location");
  if (!checkAccuracy(req.accuracyM)) return reject("low_accuracy");
  const w = checkWindow(req.capturedAt, deps.now());
  if (w !== "ok") return reject(w);

  const nonce = await computeNonce(req);
  let ok = false;
  try {
    ok = await deps.verifier.verify(req.integrityToken, nonce);
  } catch {
    ok = false; // fail closed
  }
  if (!ok) return reject("integrity_failed");

  if (await deps.db.hasStamp(userId, req.placeId)) return { status: "already_stamped" };

  const row = await deps.db.distance(req.placeId, req.lat, req.lng);
  if (!row) return reject("place_unknown");
  if (row.cap_radius_m === null || row.within_radius === null) return reject("radius_not_set");
  if (!row.within_radius) return reject("too_far", { distanceM: Math.round(row.distance_m) });

  const res = await deps.db.insertStamp(userId, req.placeId);
  return { status: res === "inserted" ? "stamped" : "already_stamped" };
}

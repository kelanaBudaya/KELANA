// Pure logic of "Unduh dataku dulu" (data export before "Hapus akun"). No network here: the rows come from an
// injected source, so it is testable.
//
// Who can read what:
//   - The user id is the verified JWT's (handler.ts), never from the request.
//   - index.ts reads with a client that carries the caller's own JWT and the anon key (no service role), so
//     Row Level Security applies on top of the `user_id` filters there.
//   - This file filters again: a row whose owner is not the caller is dropped, and every output object is built
//     field by field (allowlist), so internal columns (is_sample, storage or stream ids) never leak.
// What is in the document: account (id, email, created), profile, stamps with place and time, postcards (message,
// rating, card style, visibility, photo link), videos (caption, duration, visibility), reports the caller sent,
// and "Kabari aku" subscriptions. No location: KELANA does not store one (stamps keep only the verified result).
// Plans, needs and notifications live on the device only and are not on the server.

export const EXPORT_FORMAT = "kelana-account-export";
export const EXPORT_VERSION = 1;

/** Lifetime of a signed photo link in the export. Short on purpose: the file may be kept or shared later. */
export const PHOTO_URL_TTL_S = 600;

export const POSTCARD_BUCKET = "postcards";
export const REPORT_PHOTO_BUCKET = "report-photos";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** The verified caller, from the Auth server. */
export interface AccountUser {
  id: string;
  email: string | null;
  createdAt: string | null;
}

/** Embedded `places` row (only what the export shows). */
export interface PlaceEmbed {
  name?: string | null;
  cap_code?: string | null;
}

export interface ProfileRow {
  id: string;
  display_name?: string | null;
  created_at?: string | null;
  home_region?: { code?: string | null; name?: string | null } | null;
}

export interface StampRow {
  user_id: string;
  place_id: string;
  stamped_at: string;
  place?: PlaceEmbed | null;
}

export interface PostcardRow {
  id: string;
  user_id: string;
  place_id: string;
  message: string;
  rating?: number | null;
  card_style?: string | null;
  is_public?: boolean | null;
  photo_path?: string | null;
  created_at: string;
  place?: PlaceEmbed | null;
}

export interface VideoRow {
  id: string;
  user_id: string;
  place_id: string;
  caption?: string | null;
  duration_s?: number | null;
  created_at: string;
  place?: PlaceEmbed | null;
}

export interface ReportRow {
  id: string;
  user_id: string | null;
  place_id?: string | null;
  postcard_id?: string | null;
  video_id?: string | null;
  reason: string;
  details?: string | null;
  photo_path?: string | null;
  created_at: string;
  place?: PlaceEmbed | null;
}

export interface SubscriptionRow {
  id: string;
  user_id: string;
  category?: string | null;
  place_id?: string | null;
  created_at: string;
  place?: PlaceEmbed | null;
}

/** Reads the caller's rows. Every list/profile method throws on failure; the export then fails as a whole (500). */
export interface ExportSource {
  profile(userId: string): Promise<ProfileRow | null>;
  stamps(userId: string): Promise<StampRow[]>;
  postcards(userId: string): Promise<PostcardRow[]>;
  videos(userId: string): Promise<VideoRow[]>;
  reports(userId: string): Promise<ReportRow[]>;
  subscriptions(userId: string): Promise<SubscriptionRow[]>;
  /** A signed URL for an object, or null when it cannot be made. Must not throw. */
  signedUrl(bucket: string, path: string, expiresInS: number): Promise<string | null>;
}

export interface PlaceRef {
  id: string;
  name: string | null;
  capCode: string | null;
}

export interface PhotoLink {
  url: string;
  expiresAt: string;
}

export interface AccountExport {
  format: typeof EXPORT_FORMAT;
  version: typeof EXPORT_VERSION;
  exportedAt: string;
  account: { id: string; email: string | null; createdAt: string | null };
  profile: {
    displayName: string | null;
    homeRegion: { code: string | null; name: string | null } | null;
    createdAt: string | null;
  } | null;
  stamps: { place: PlaceRef; stampedAt: string }[];
  postcards: {
    id: string;
    place: PlaceRef;
    message: string;
    rating: number | null;
    cardStyle: string | null;
    visibility: "public" | "private";
    photo: PhotoLink | null;
    createdAt: string;
  }[];
  videos: {
    id: string;
    place: PlaceRef;
    caption: string | null;
    durationS: number | null;
    visibility: "public";
    createdAt: string;
  }[];
  reports: {
    id: string;
    reason: string;
    details: string | null;
    place: PlaceRef | null;
    postcardId: string | null;
    videoId: string | null;
    photo: PhotoLink | null;
    createdAt: string;
  }[];
  subscriptions: { id: string; category: string | null; place: PlaceRef | null; createdAt: string }[];
}

const place = (id: string, embed: PlaceEmbed | null | undefined): PlaceRef => ({
  id,
  name: embed?.name ?? null,
  capCode: embed?.cap_code ?? null,
});

/** True for a Storage path inside the caller's own folder, with no empty or dot segments. */
export function isOwnPath(userId: string, path: string | null | undefined): path is string {
  if (!path || !path.startsWith(`${userId}/`)) return false;
  return path.split("/").every((seg) => seg !== "" && seg !== "." && seg !== "..");
}

async function photo(
  source: ExportSource,
  userId: string,
  bucket: string,
  path: string | null | undefined,
  now: Date,
): Promise<PhotoLink | null> {
  if (!isOwnPath(userId, path)) return null;
  const url = await source.signedUrl(bucket, path, PHOTO_URL_TTL_S);
  if (!url) return null;
  return { url, expiresAt: new Date(now.getTime() + PHOTO_URL_TTL_S * 1000).toISOString() };
}

/**
 * Builds the export of [user]. Throws when any read fails (no partial document pretends to be complete).
 * Rows that do not belong to the caller are dropped even if the source returned them.
 */
export async function buildExport(user: AccountUser, source: ExportSource, now: Date): Promise<AccountExport> {
  const userId = user.id;
  // The id comes from a verified JWT; still refuse anything that is not a UUID (it becomes a Storage prefix).
  if (!UUID_RE.test(userId)) throw new Error("invalid_user_id");

  const [profileRow, stampRows, postcardRows, videoRows, reportRows, subscriptionRows] = await Promise.all([
    source.profile(userId),
    source.stamps(userId),
    source.postcards(userId),
    source.videos(userId),
    source.reports(userId),
    source.subscriptions(userId),
  ]);
  const own = <T extends { user_id: string | null }>(rows: T[]) => rows.filter((r) => r.user_id === userId);

  const profile = profileRow && profileRow.id === userId
    ? {
      displayName: profileRow.display_name ?? null,
      homeRegion: profileRow.home_region
        ? { code: profileRow.home_region.code ?? null, name: profileRow.home_region.name ?? null }
        : null,
      createdAt: profileRow.created_at ?? null,
    }
    : null;

  const postcards = await Promise.all(
    own(postcardRows).map(async (r) => ({
      id: r.id,
      place: place(r.place_id, r.place),
      message: r.message,
      rating: r.rating ?? null,
      cardStyle: r.card_style ?? null,
      visibility: r.is_public === false ? "private" as const : "public" as const,
      photo: await photo(source, userId, POSTCARD_BUCKET, r.photo_path, now),
      createdAt: r.created_at,
    })),
  );

  const reports = await Promise.all(
    own(reportRows).map(async (r) => ({
      id: r.id,
      reason: r.reason,
      details: r.details ?? null,
      place: r.place_id ? place(r.place_id, r.place) : null,
      postcardId: r.postcard_id ?? null,
      videoId: r.video_id ?? null,
      photo: await photo(source, userId, REPORT_PHOTO_BUCKET, r.photo_path, now),
      createdAt: r.created_at,
    })),
  );

  return {
    format: EXPORT_FORMAT,
    version: EXPORT_VERSION,
    exportedAt: now.toISOString(),
    account: { id: userId, email: user.email, createdAt: user.createdAt },
    profile,
    stamps: own(stampRows).map((r) => ({ place: place(r.place_id, r.place), stampedAt: r.stamped_at })),
    postcards,
    // Videos have no visibility column: every video is shown to logged-in users.
    videos: own(videoRows).map((r) => ({
      id: r.id,
      place: place(r.place_id, r.place),
      caption: r.caption ?? null,
      durationS: r.duration_s ?? null,
      visibility: "public" as const,
      createdAt: r.created_at,
    })),
    reports,
    subscriptions: own(subscriptionRows).map((r) => ({
      id: r.id,
      category: r.category ?? null,
      place: r.place_id ? place(r.place_id, r.place) : null,
      createdAt: r.created_at,
    })),
  };
}

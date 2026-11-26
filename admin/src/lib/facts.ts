// Official facts as the admin panel sees them: kinds, editable fields, labels, status and formatting.
// All copy is Bahasa Indonesia (the panel is used by the team in Indonesia); dates are id-ID, Asia/Jakarta.

export type FactKind = "opening_hours" | "closures" | "ticket_prices" | "events";
export const FACT_KINDS: FactKind[] = ["opening_hours", "closures", "ticket_prices", "events"];

export function isFactKind(v: unknown): v is FactKind {
  return typeof v === "string" && (FACT_KINDS as string[]).includes(v);
}

export type StaffRole = "checker" | "reviewer" | "admin";

export interface QueueRow {
  fact_table: FactKind;
  fact_id: string;
  place_id: string;
  place_name: string;
  place_category: string;
  data: Record<string, unknown>;
  review_status: "pending" | "published";
  checked_at: string | null;
  source_url: string | null;
  internal_note: string | null;
  manager_confirmation: string | null;
  has_conflict: boolean;
  due_on: string | null;
  checker_id: string | null;
  checker_name: string | null;
  checker_marked_at: string | null;
  reviewer_id: string | null;
  reviewer_name: string | null;
  reviewed_at: string | null;
  check_is_current: boolean;
}

export interface HistoryRow {
  id: number;
  fact_table: FactKind;
  fact_id: string;
  place_id: string | null;
  action: "insert" | "update" | "delete" | "checked" | "reviewed";
  before: Record<string, unknown> | null;
  after: Record<string, unknown> | null;
  source_url: string | null;
  changed_by: string | null;
  changed_at: string;
}

export const KIND_LABEL: Record<FactKind, string> = {
  opening_hours: "Jam buka",
  closures: "Hari tutup",
  ticket_prices: "Harga tiket",
  events: "Jadwal pertunjukan",
};

export const ROLE_LABEL: Record<StaffRole, string> = {
  checker: "Pemeriksa data",
  reviewer: "Peninjau data",
  admin: "Admin",
};

const WEEKDAY = ["", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"];
const WEEKDAY_SHORT = ["", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"];

export function weekdayName(n: unknown): string {
  return typeof n === "number" && n >= 1 && n <= 7 ? WEEKDAY[n] : "";
}

// ------------------------------------------------------------------ formatting
const TZ = "Asia/Jakarta";
const dateFmt = new Intl.DateTimeFormat("id-ID", { day: "numeric", month: "short", year: "numeric", timeZone: TZ });
const dateTimeFmt = new Intl.DateTimeFormat("id-ID", {
  day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit", timeZone: TZ,
});
const rupiah = new Intl.NumberFormat("id-ID", { style: "currency", currency: "IDR", maximumFractionDigits: 0 });

/** "20 Nov 2026". Accepts a timestamp or a plain date (YYYY-MM-DD, read as a Jakarta date). */
export function formatDate(v: string | null | undefined): string {
  if (!v) return "";
  const d = /^\d{4}-\d{2}-\d{2}$/.test(v) ? new Date(`${v}T12:00:00+07:00`) : new Date(v);
  return Number.isNaN(d.getTime()) ? "" : dateFmt.format(d);
}

export function formatDateTime(v: string | null | undefined): string {
  if (!v) return "";
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? "" : dateTimeFmt.format(d);
}

/** "Rp150.000" (id-ID without the space Intl puts after "Rp"). */
export function formatRupiah(n: unknown): string {
  if (typeof n !== "number") return "";
  return rupiah.format(n).replace(/\s/g, "");
}

/** "19.30" from "19:30:00". */
export function formatTime(v: unknown): string {
  return typeof v === "string" && v.length >= 5 ? v.slice(0, 5).replace(":", ".") : "";
}

/** YYYY-MM-DD of a timestamp in Asia/Jakarta, for <input type="date">. */
export function jakartaDate(v: string | null | undefined): string {
  if (!v) return "";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return "";
  return new Intl.DateTimeFormat("en-CA", { timeZone: TZ, year: "numeric", month: "2-digit", day: "2-digit" }).format(d);
}

export function todayJakarta(): string {
  return jakartaDate(new Date().toISOString());
}

export function addDays(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function sourceLabel(url: string | null | undefined): string {
  if (!url) return "";
  try {
    const u = new URL(url);
    return u.host + (u.pathname === "/" ? "" : u.pathname);
  } catch {
    return url;
  }
}

// ------------------------------------------------------------------ titles
/** One-line description of a fact, used in the queue, the detail header and the history. */
export function factTitle(kind: FactKind, d: Record<string, unknown> | null): string {
  if (!d) return KIND_LABEL[kind];
  switch (kind) {
    case "opening_hours":
      return `Jam buka ${weekdayName(d.weekday)}`.trim();
    case "closures":
      return `Hari tutup ${formatDate(d.starts_on as string)}${d.ends_on && d.ends_on !== d.starts_on ? ` sampai ${formatDate(d.ends_on as string)}` : ""}`;
    case "ticket_prices":
      return `Harga tiket ${String(d.label ?? "")}`.trim();
    case "events":
      return `Jadwal ${String(d.title ?? "")}`.trim();
  }
}

/** Short value summary, e.g. "Sel 19.30–21.00" or "Rp60.000". */
export function factSummary(kind: FactKind, d: Record<string, unknown> | null): string {
  if (!d) return "—";
  switch (kind) {
    case "opening_hours":
      return d.is_closed ? `${weekdayName(d.weekday)} tutup` : `${weekdayName(d.weekday)} ${formatTime(d.opens_at)}–${formatTime(d.closes_at)}`;
    case "closures":
      return `${formatDate(d.starts_on as string)}–${formatDate(d.ends_on as string)}${d.reason ? ` (${String(d.reason)})` : ""}`;
    case "ticket_prices":
      return d.price_idr == null ? "Harga belum diketahui" : formatRupiah(d.price_idr);
    case "events": {
      const when =
        d.recurrence === "weekly" ? WEEKDAY_SHORT[Number(d.weekday)] ?? ""
          : d.recurrence === "once" ? formatDate(d.event_date as string)
            : "Setiap hari";
      const end = d.end_time ? `–${formatTime(d.end_time)}` : "";
      return `${when} ${formatTime(d.start_time)}${end}`.trim();
    }
  }
}

// ------------------------------------------------------------------ status
export type QueueStatus = "conflict" | "needs_check" | "awaiting_review" | "published";

export const STATUS_LABEL: Record<QueueStatus, string> = {
  conflict: "Sumber bertentangan",
  needs_check: "Perlu dicek",
  awaiting_review: "Menunggu tinjauan",
  published: "Terbit",
};

export function queueStatus(r: QueueRow): QueueStatus {
  if (r.review_status === "published") return "published";
  if (r.has_conflict) return "conflict";
  if (r.checker_id && r.check_is_current) return "awaiting_review";
  return "needs_check";
}

export type QueueFilter = "semua" | "bertentangan" | "jatuh-tempo" | "tinjauan";
export const FILTERS: { id: QueueFilter; label: string }[] = [
  { id: "semua", label: "Semua" },
  { id: "bertentangan", label: "Bertentangan" },
  { id: "jatuh-tempo", label: "Jatuh tempo" },
  { id: "tinjauan", label: "Tinjauan" },
];

export function isDueSoon(r: QueueRow, today = todayJakarta()): boolean {
  return !!r.due_on && r.due_on <= addDays(today, 7);
}

export function matchesFilter(r: QueueRow, f: QueueFilter): boolean {
  switch (f) {
    case "semua":
      return true;
    case "bertentangan":
      return r.review_status === "pending" && r.has_conflict;
    case "jatuh-tempo":
      return isDueSoon(r);
    case "tinjauan":
      return queueStatus(r) === "awaiting_review";
  }
}

export function matchesSearch(r: QueueRow, q: string): boolean {
  if (!q) return true;
  const hay = `${r.place_name} ${factTitle(r.fact_table, r.data)} ${KIND_LABEL[r.fact_table]}`.toLowerCase();
  return hay.includes(q.toLowerCase());
}

// ------------------------------------------------------------------ editable fields
export type FieldType = "text" | "url" | "number" | "time" | "date" | "weekday" | "checkbox" | "recurrence";

export interface FieldDef {
  name: string;
  label: string;
  type: FieldType;
  /** flex basis hint for the form grid */
  wide?: boolean;
}

/** Fields the panel edits per kind. checked_at is handled separately as "Tanggal periksa". */
export const FIELDS: Record<FactKind, FieldDef[]> = {
  opening_hours: [
    { name: "weekday", label: "Hari", type: "weekday" },
    { name: "opens_at", label: "Buka", type: "time" },
    { name: "closes_at", label: "Tutup", type: "time" },
    { name: "is_closed", label: "Tutup sepanjang hari", type: "checkbox", wide: true },
    { name: "note", label: "Keterangan untuk wisatawan", type: "text", wide: true },
  ],
  closures: [
    { name: "starts_on", label: "Mulai", type: "date" },
    { name: "ends_on", label: "Sampai", type: "date" },
    { name: "reason", label: "Alasan (tampil ke wisatawan)", type: "text", wide: true },
  ],
  ticket_prices: [
    { name: "label", label: "Jenis tiket", type: "text" },
    { name: "price_idr", label: "Harga (Rp)", type: "number" },
    { name: "seller_url", label: "Situs penjual resmi", type: "url", wide: true },
  ],
  events: [
    { name: "title", label: "Judul", type: "text", wide: true },
    { name: "recurrence", label: "Pengulangan", type: "recurrence" },
    { name: "weekday", label: "Hari", type: "weekday" },
    { name: "event_date", label: "Tanggal", type: "date" },
    { name: "start_time", label: "Mulai", type: "time" },
    { name: "end_time", label: "Selesai", type: "time" },
    { name: "price_from_idr", label: "Harga mulai (Rp)", type: "number" },
    { name: "seller_url", label: "Situs penjual resmi", type: "url", wide: true },
  ],
};

export const FIELD_LABEL: Record<string, string> = {
  ...Object.fromEntries(Object.values(FIELDS).flat().map((f) => [f.name, f.label])),
  source_url: "Sumber resmi",
  checked_at: "Tanggal periksa",
  review_status: "Status",
  description: "Deskripsi",
  category: "Kategori",
};

/** Turns one form value into the JSON value the database expects. Empty means null. */
export function parseField(def: FieldDef, raw: FormDataEntryValue | null): unknown {
  if (def.type === "checkbox") return raw === "on";
  const v = typeof raw === "string" ? raw.trim() : "";
  if (v === "") return null;
  if (def.type === "number") {
    const n = Number(v.replace(/\./g, ""));
    return Number.isFinite(n) ? Math.round(n) : null;
  }
  if (def.type === "weekday") return Number(v);
  return v;
}

// ------------------------------------------------------------------ history
function display(key: string, v: unknown): string {
  if (v == null || v === "") return "—";
  if (key === "review_status") return v === "published" ? "Terbit" : "Draf";
  if (key === "weekday") return weekdayName(v) || String(v);
  if (key.endsWith("_idr")) return formatRupiah(v);
  if (key.endsWith("_time") || key === "opens_at" || key === "closes_at") return formatTime(v);
  if (key === "checked_at") return formatDate(String(v));
  if (key === "starts_on" || key === "ends_on" || key === "event_date") return formatDate(String(v));
  if (typeof v === "boolean") return v ? "ya" : "tidak";
  if (Array.isArray(v)) return v.join(", ") || "—";
  return String(v);
}

/** "Mulai: 17.30 → 19.30; Status: Draf → Terbit" */
export function historyChange(h: HistoryRow): string {
  if (h.action === "checked") return "Diperiksa (pemeriksa pertama)";
  if (h.action === "reviewed") return "Ditinjau pemeriksa kedua";
  if (h.action === "insert") return `— → ${factSummary(h.fact_table, h.after)}`;
  if (h.action === "delete") return `${factSummary(h.fact_table, h.before)} → dihapus`;
  const before = h.before ?? {};
  const after = h.after ?? {};
  const keys = Object.keys(after).filter(
    (k) => k !== "id" && JSON.stringify(before[k]) !== JSON.stringify(after[k]),
  );
  if (keys.length === 0) return "—";
  return keys
    .map((k) => `${FIELD_LABEL[k] ?? k}: ${display(k, before[k])} → ${display(k, after[k])}`)
    .join("; ");
}

// ------------------------------------------------------------------ errors from the database
const ERROR_TEXT: Record<string, string> = {
  four_eyes_required: "Terbit wajib ditinjau orang yang berbeda dari pemeriksa.",
  stale_check: "Data berubah setelah diperiksa. Minta pemeriksa memeriksa ulang.",
  reviewer_role_required: "Hanya peninjau atau admin yang bisa menerbitkan.",
  not_staff: "Akunmu tidak punya akses ke data ini.",
  fact_not_pending: "Data ini sudah terbit. Ubah nilainya dulu untuk membuat draf baru.",
  check_needs_source_and_date: "Isi sumber resmi dan tanggal periksa sebelum minta tinjauan.",
  fact_not_found: "Data tidak ditemukan.",
};

export function errorText(code: string | undefined): string | null {
  if (!code) return null;
  return ERROR_TEXT[code] ?? "Perubahan gagal disimpan. Periksa isian lalu coba lagi.";
}

/** Maps a Postgres/PostgREST error message to a known code. */
export function errorCode(message: string | undefined): string {
  if (!message) return "unknown";
  const known = Object.keys(ERROR_TEXT).find((k) => message.startsWith(k));
  return known ?? "unknown";
}

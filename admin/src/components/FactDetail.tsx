import { publish, requestReview, saveDraft } from "@/app/actions";
import type { CurrentStaff } from "@/lib/data";
import {
  FIELDS,
  KIND_LABEL,
  formatDate,
  formatDateTime,
  jakartaDate,
  weekdayName,
  factTitle,
  type FieldDef,
  type QueueRow,
} from "@/lib/facts";

function inputValue(def: FieldDef, v: unknown): string {
  if (v == null) return "";
  if (def.type === "time" && typeof v === "string") return v.slice(0, 5);
  return String(v);
}

function Field({ def, value }: { def: FieldDef; value: unknown }) {
  const id = `f-${def.name}`;
  if (def.type === "checkbox") {
    return (
      <label className="check" htmlFor={id} style={{ flexBasis: "100%" }}>
        <input id={id} name={def.name} type="checkbox" defaultChecked={value === true} />
        {def.label}
      </label>
    );
  }
  let control: React.ReactNode;
  if (def.type === "weekday") {
    control = (
      <select id={id} name={def.name} defaultValue={value == null ? "" : String(value)}>
        <option value="">—</option>
        {[1, 2, 3, 4, 5, 6, 7].map((d) => (
          <option key={d} value={d}>
            {weekdayName(d)}
          </option>
        ))}
      </select>
    );
  } else if (def.type === "recurrence") {
    control = (
      <select id={id} name={def.name} defaultValue={String(value ?? "weekly")}>
        <option value="weekly">Tiap minggu</option>
        <option value="once">Sekali</option>
        <option value="daily">Tiap hari</option>
      </select>
    );
  } else {
    const type = def.type === "number" ? "number" : def.type;
    control = (
      <input
        id={id}
        name={def.name}
        type={type}
        min={def.type === "number" ? 0 : undefined}
        step={def.type === "number" ? 1 : undefined}
        defaultValue={inputValue(def, value)}
      />
    );
  }
  return (
    <label className={def.wide ? "field wide" : "field"} htmlFor={id}>
      <span>{def.label}</span>
      {control}
    </label>
  );
}

export function FactDetail({ row, staff, returnTo }: { row: QueueRow; staff: CurrentStaff; returnTo: string }) {
  const d = row.data;
  const published = row.review_status === "published";
  const checked = !!row.checker_id && row.check_is_current;
  const stale = !!row.checker_id && !row.check_is_current;
  const isChecker = row.checker_id === staff.userId;
  const canPublish = !published && checked && !isChecker && staff.role !== "checker";

  let publishHint = "";
  if (published) publishHint = "Sudah terbit. Mengubah nilai membuat draf baru yang perlu diperiksa ulang.";
  else if (!checked) publishHint = "Belum bisa terbit: data harus ditandai diperiksa lebih dulu.";
  else if (isChecker) publishHint = "Kamu pemeriksa data ini. Terbit wajib ditinjau orang lain.";
  else if (staff.role === "checker") publishHint = "Hanya peninjau atau admin yang bisa menerbitkan.";

  return (
    <section className="panel panel-detail" aria-label="Perbarui data">
      <div className="detail-head">
        <span className="cat" aria-hidden="true">
          {row.place_category.slice(0, 2)}
        </span>
        <div>
          <div className="detail-place">
            {row.place_name} · {KIND_LABEL[row.fact_table]}
          </div>
          <h2>{factTitle(row.fact_table, d)}</h2>
        </div>
      </div>

      <form>
        <input type="hidden" name="kind" value={row.fact_table} />
        <input type="hidden" name="id" value={row.fact_id} />
        <input type="hidden" name="return" value={returnTo} />
        <input type="hidden" name="checked_at_prev" value={row.checked_at ?? ""} />

        <div className="note-box">
          <label htmlFor="f-note">Catatan internal, tidak tampil ke wisatawan.</label>
          <textarea id="f-note" name="internal_note" defaultValue={row.internal_note ?? ""} maxLength={2000} />
          <label className="check" htmlFor="f-conflict" style={{ fontWeight: 500, marginBottom: 0 }}>
            <input id="f-conflict" name="has_conflict" type="checkbox" defaultChecked={row.has_conflict} />
            Sumber bertentangan, perlu konfirmasi ke pengelola
          </label>
        </div>

        <div className="grid">
          {FIELDS[row.fact_table].map((def) => (
            <Field key={def.name} def={def} value={d[def.name]} />
          ))}
        </div>
        <div className="grid">
          <label className="field wide" htmlFor="f-source">
            <span>Sumber resmi</span>
            <input id="f-source" name="source_url" type="url" defaultValue={row.source_url ?? ""} />
          </label>
        </div>
        <div className="grid">
          <label className="field" htmlFor="f-checked">
            <span>Tanggal periksa</span>
            <input id="f-checked" name="checked_date" type="date" defaultValue={jakartaDate(row.checked_at)} />
          </label>
          <label className="field" htmlFor="f-due">
            <span>Jatuh tempo</span>
            <input id="f-due" name="due_on" type="date" defaultValue={row.due_on ?? ""} />
          </label>
          <label className="field wide" htmlFor="f-confirm">
            <span>Konfirmasi pengelola</span>
            <input
              id="f-confirm"
              name="manager_confirmation"
              type="text"
              maxLength={300}
              placeholder="Nama dan kanal, misalnya WhatsApp pengelola"
              defaultValue={row.manager_confirmation ?? ""}
            />
          </label>
        </div>

        {/* PROVISIONAL: evidence upload needs a private bucket and policies; not built in v1. */}
        <div className="upload">
          <span>Unggah tangkapan layar sumber sebagai bukti (belum tersedia)</span>
          <input type="file" aria-label="Unggah bukti" disabled style={{ width: 110, fontSize: 12 }} />
        </div>
        {/* PROVISIONAL: notifying subscribers needs an FCM sender job; not built in v1. */}
        <label className="check disabled" htmlFor="f-kabari">
          <input id="f-kabari" type="checkbox" disabled />
          Kabari pengguna yang menyimpan data ini setelah terbit (belum tersedia).
        </label>

        <div className="steps">
          <div className="step">
            <span className={`dot ${checked || published ? "done" : stale ? "current" : ""}`} aria-hidden="true">
              {checked || published ? "✓" : ""}
            </span>
            <div>
              <div className="step-title">Diperiksa</div>
              <div className="step-sub">
                {row.checker_id
                  ? `Pemeriksa ${row.checker_name ?? "staf lama"} · ${formatDate(row.checker_marked_at)}${stale ? " · data berubah setelah diperiksa, periksa ulang" : ""}`
                  : "Belum diperiksa"}
              </div>
            </div>
          </div>
          <div className="step">
            <span className={`dot ${published && row.reviewer_id ? "done" : checked ? "current" : ""}`} aria-hidden="true">
              {published && row.reviewer_id ? "✓" : ""}
            </span>
            <div>
              <div className="step-title">Ditinjau pemeriksa kedua</div>
              <div className="step-sub">
                {row.reviewer_id
                  ? `Peninjau ${row.reviewer_name ?? "staf lama"} · ${formatDate(row.reviewed_at)}`
                  : "Wajib orang berbeda sebelum terbit"}
              </div>
            </div>
          </div>
          <div className="step">
            <span className={`dot ${published ? "done" : ""}`} aria-hidden="true">
              {published ? "✓" : ""}
            </span>
            <div>
              <div className="step-title">Terbit ke aplikasi</div>
              <div className="step-sub">
                {published && row.checked_at
                  ? `Tampil dengan tanggal periksa ${formatDate(row.checked_at)}`
                  : published
                    ? "Terbit tanpa tanggal periksa: aplikasi menampilkan “sedang dicek”"
                    : "Tanggal periksa ikut tampil di layar wisatawan"}
              </div>
            </div>
          </div>
        </div>

        <div className="actions">
          <button className="btn" type="submit" formAction={saveDraft}>
            Simpan draf
          </button>
          {canPublish ? (
            <button className="btn btn-primary" type="submit" formAction={publish}>
              Setujui dan terbitkan
            </button>
          ) : (
            <button className="btn btn-primary" type="submit" formAction={requestReview}>
              Minta tinjauan
            </button>
          )}
        </div>
        {publishHint ? <p className="hint">{publishHint}</p> : null}
        {row.reviewed_at ? <p className="hint">Terakhir ditinjau {formatDateTime(row.reviewed_at)}.</p> : null}
      </form>
    </section>
  );
}

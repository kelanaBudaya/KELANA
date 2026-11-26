import Link from "next/link";
import { getHistory, getQueue, getSession, getStaffNames, getSummary } from "@/lib/data";
import {
  FILTERS,
  STATUS_LABEL,
  errorText,
  factTitle,
  formatDate,
  isDueSoon,
  matchesFilter,
  matchesSearch,
  queueStatus,
  type FactKind,
  type QueueFilter,
} from "@/lib/facts";
import { FactDetail } from "./FactDetail";
import { HistoryTable } from "./HistoryTable";

export interface QueueSearch {
  q?: string;
  filter?: string;
  data?: string;
  ok?: string;
  galat?: string;
}

const OK_TEXT: Record<string, string> = {
  draf: "Draf tersimpan.",
  diperiksa: "Ditandai diperiksa. Menunggu tinjauan pemeriksa kedua.",
  terbit: "Terbit. Data ini sekarang tampil di aplikasi dengan tanggal periksanya.",
};

function href(base: string, params: Record<string, string | undefined>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v) sp.set(k, v);
  const s = sp.toString();
  return s ? `${base}?${s}` : base;
}

export async function QueuePage({
  basePath,
  kind,
  title,
  lead,
  search,
}: {
  basePath: string;
  kind?: FactKind;
  title: string;
  lead: string;
  search: QueueSearch;
}) {
  const [{ staff }, all, summary, names] = await Promise.all([getSession(), getQueue(), getSummary(), getStaffNames()]);
  if (!staff) return null;

  const filter: QueueFilter = FILTERS.some((f) => f.id === search.filter) ? (search.filter as QueueFilter) : "semua";
  const q = (search.q ?? "").trim();
  const rows = all
    .filter((r) => !kind || r.fact_table === kind)
    .filter((r) => matchesFilter(r, filter) && matchesSearch(r, q));

  const selectedKey = search.data ?? (rows[0] ? `${rows[0].fact_table}:${rows[0].fact_id}` : undefined);
  const selected = all.find((r) => `${r.fact_table}:${r.fact_id}` === selectedKey);
  const history = selected ? await getHistory({ placeId: selected.place_id, limit: 20 }) : [];
  const keep = { q: q || undefined, filter: filter === "semua" ? undefined : filter };
  const returnTo = href(basePath, { ...keep, data: selectedKey });
  const flashOk = search.ok ? OK_TEXT[search.ok] : null;
  const flashError = errorText(search.galat);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>{title}</h1>
          <p className="lead">{lead}</p>
        </div>
        <form className="search" role="search" action={basePath}>
          <label htmlFor="cariq" className="sr-only">
            Cari tempat atau data
          </label>
          <input id="cariq" name="q" type="search" placeholder="Cari tempat atau data" defaultValue={q} />
          {keep.filter ? <input type="hidden" name="filter" value={keep.filter} /> : null}
        </form>
      </div>

      <div className="cards">
        <div className="card">
          <div className="card-label">Jatuh tempo minggu ini</div>
          <div className="card-value">{summary.due_this_week}</div>
          <div className="card-note">Menurut tanggal jatuh tempo yang diisi pemeriksa</div>
        </div>
        <div className="card">
          <div className="card-label">Sumber bertentangan</div>
          <div className="card-value">{summary.conflicts}</div>
          <div className="card-note">Perlu konfirmasi ke pengelola</div>
        </div>
        <div className="card">
          <div className="card-label">Laporan pengguna baru</div>
          <div className="card-value">{summary.reports_7d}</div>
          <div className="card-note">Dari tombol Laporkan, 7 hari terakhir</div>
        </div>
        <div className="card">
          <div className="card-label">Terbit 30 hari terakhir</div>
          <div className="card-value">{summary.published_30d}</div>
          <div className="card-note">Lolos dua pemeriksa</div>
        </div>
      </div>

      {flashOk ? (
        <p className="flash flash-ok" role="status">
          {flashOk}
        </p>
      ) : null}
      {flashError ? (
        <p className="flash flash-error" role="alert">
          {flashError}
        </p>
      ) : null}

      <div className="columns">
        <section className="panel panel-list" aria-label="Daftar antrean">
          <nav className="filters" aria-label="Saring antrean">
            {FILTERS.map((f) => (
              <Link
                key={f.id}
                className="chip"
                href={href(basePath, { q: keep.q, filter: f.id === "semua" ? undefined : f.id })}
                aria-current={f.id === filter ? "page" : undefined}
              >
                {f.label}
              </Link>
            ))}
          </nav>
          {rows.length === 0 ? (
            <p className="empty">Tidak ada data yang cocok dengan saringan ini.</p>
          ) : (
            <div className="table-wrap">
              <table className="queue">
                <thead>
                  <tr>
                    <th scope="col">Tempat &amp; data</th>
                    <th scope="col">Terakhir diperiksa</th>
                    <th scope="col">Jatuh tempo</th>
                    <th scope="col">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => {
                    const key = `${r.fact_table}:${r.fact_id}`;
                    const status = queueStatus(r);
                    const isSelected = key === selectedKey;
                    return (
                      <tr key={key} className={isSelected ? "selected" : undefined}>
                        <td>
                          <Link
                            className="row-link"
                            href={href(basePath, { ...keep, data: key })}
                            aria-current={isSelected ? "true" : undefined}
                          >
                            <span className="cat" aria-hidden="true">
                              {r.place_category.slice(0, 2)}
                            </span>
                            <span>
                              <span className="row-title">{r.place_name}</span>
                              <span className="row-sub">{factTitle(r.fact_table, r.data)}</span>
                            </span>
                          </Link>
                        </td>
                        <td className="nowrap">
                          {r.checked_at ? formatDate(r.checked_at) : <span className="muted">Belum diperiksa</span>}
                        </td>
                        <td className="nowrap">
                          {r.due_on ? (
                            <span style={isDueSoon(r) ? { fontWeight: 700 } : undefined}>{formatDate(r.due_on)}</span>
                          ) : (
                            <span className="muted">Belum diatur</span>
                          )}
                        </td>
                        <td>
                          <span className={`badge badge-${status}`}>{STATUS_LABEL[status]}</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {selected ? (
          <FactDetail row={selected} staff={staff} returnTo={returnTo} />
        ) : (
          <section className="panel panel-detail" aria-label="Perbarui data">
            <p className="muted">Pilih data di antrean untuk melihat detailnya.</p>
          </section>
        )}
      </div>

      {selected ? (
        <section className="panel panel-block" aria-label="Riwayat perubahan">
          <h2 className="block-title">Riwayat perubahan · {selected.place_name}</h2>
          <HistoryTable rows={history} names={names} showPlace={false} />
        </section>
      ) : null}
    </>
  );
}

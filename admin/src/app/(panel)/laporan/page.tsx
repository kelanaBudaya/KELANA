import { createClient } from "@/lib/supabase/server";
import { getPlaceNames } from "@/lib/data";
import { formatDateTime } from "@/lib/facts";

const REASON: Record<string, string> = {
  hours_or_price_changed: "Jam atau harga berubah",
  place_closed: "Tempat tutup",
  ceremony_shown_without_permission: "Upacara ditampilkan tanpa izin",
  inappropriate_media: "Foto atau video tidak pantas",
  other: "Lainnya",
};

interface ReportRow {
  id: string;
  user_id: string | null;
  place_id: string | null;
  postcard_id: string | null;
  video_id: string | null;
  reason: string;
  details: string | null;
  photo_path: string | null;
  created_at: string;
}

export default async function LaporanPage() {
  const supabase = await createClient();
  const [{ data, error }, places] = await Promise.all([
    supabase
      .from("reports")
      .select("id, user_id, place_id, postcard_id, video_id, reason, details, photo_path, created_at")
      .order("created_at", { ascending: false })
      .limit(200),
    getPlaceNames(),
  ]);
  const rows = (data ?? []) as ReportRow[];

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Laporan pengguna</h1>
          <p className="lead">Dari tombol Laporkan di aplikasi. Hanya baca untuk saat ini.</p>
        </div>
      </div>
      <section className="panel panel-list" style={{ marginTop: 20 }} aria-label="Daftar laporan">
        {error ? (
          <p className="empty" role="alert">
            Laporan tidak bisa dimuat. Coba muat ulang halaman.
          </p>
        ) : rows.length === 0 ? (
          <p className="empty">Belum ada laporan.</p>
        ) : (
          <div className="table-wrap">
            <table className="queue">
              <thead>
                <tr>
                  <th scope="col">Waktu</th>
                  <th scope="col">Tentang</th>
                  <th scope="col">Alasan</th>
                  <th scope="col">Keterangan</th>
                  <th scope="col">Pelapor</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap">{formatDateTime(r.created_at)}</td>
                    <td>
                      {r.place_id ? (places.get(r.place_id) ?? "Tempat") : r.postcard_id ? "Postcard" : "Video"}
                      {r.photo_path ? <span className="row-sub">Ada foto (belum bisa dibuka di panel)</span> : null}
                    </td>
                    <td>{REASON[r.reason] ?? r.reason}</td>
                    <td>{r.details ?? <span className="muted">—</span>}</td>
                    <td>{r.user_id ? "Pengguna" : <span className="muted">Akun dihapus</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

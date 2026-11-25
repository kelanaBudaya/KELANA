import { HistoryTable } from "@/components/HistoryTable";
import { getHistory, getPlaceNames, getStaffNames } from "@/lib/data";

export default async function RiwayatPage() {
  const [rows, names, places] = await Promise.all([getHistory({ limit: 100 }), getStaffNames(), getPlaceNames()]);
  return (
    <>
      <div className="page-head">
        <div>
          <h1>Riwayat perubahan</h1>
          <p className="lead">
            Dicatat otomatis oleh database dan tidak bisa diubah atau dihapus. Menampilkan 100 perubahan terakhir.
          </p>
        </div>
      </div>
      <section className="panel panel-block" aria-label="Riwayat perubahan">
        <HistoryTable rows={rows} names={names} placeNames={places} showPlace />
      </section>
    </>
  );
}

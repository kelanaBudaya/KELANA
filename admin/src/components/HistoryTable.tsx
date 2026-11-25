import { factTitle, formatDate, historyChange, sourceLabel, type HistoryRow } from "@/lib/facts";

export function HistoryTable({
  rows,
  names,
  placeNames,
  showPlace,
}: {
  rows: HistoryRow[];
  names: Map<string, string>;
  placeNames?: Map<string, string>;
  showPlace: boolean;
}) {
  if (rows.length === 0) return <p className="muted">Belum ada perubahan tercatat.</p>;
  return (
    <div className="table-wrap">
      <table className="history">
        <thead>
          <tr>
            <th scope="col">Tanggal</th>
            {showPlace ? <th scope="col">Tempat</th> : null}
            <th scope="col">Data</th>
            <th scope="col">Sebelum → sesudah</th>
            <th scope="col">Sumber</th>
            <th scope="col">Oleh</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((h) => (
            <tr key={h.id}>
              <td className="nowrap">{formatDate(h.changed_at)}</td>
              {showPlace ? <td>{(h.place_id && placeNames?.get(h.place_id)) || "—"}</td> : null}
              <td>{factTitle(h.fact_table, h.after ?? h.before)}</td>
              <td>{historyChange(h)}</td>
              <td>
                {h.source_url ? (
                  <a href={h.source_url} target="_blank" rel="noopener noreferrer">
                    {sourceLabel(h.source_url)}
                  </a>
                ) : (
                  <span className="muted">—</span>
                )}
              </td>
              <td>
                {h.changed_by ? (
                  (names.get(h.changed_by) ?? "Staf lama")
                ) : (
                  <span className="muted">Sistem (pengambil data atau dasbor)</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

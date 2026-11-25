import Link from "next/link";
import { getQueue } from "@/lib/data";
import { queueStatus } from "@/lib/facts";

export default async function TempatPage() {
  const queue = await getQueue();
  const byPlace = new Map<string, { name: string; category: string; pending: number; published: number; firstKey: string }>();
  for (const r of queue) {
    const e = byPlace.get(r.place_id) ?? {
      name: r.place_name,
      category: r.place_category,
      pending: 0,
      published: 0,
      firstKey: `${r.fact_table}:${r.fact_id}`,
    };
    if (queueStatus(r) === "published") e.published += 1;
    else e.pending += 1;
    byPlace.set(r.place_id, e);
  }
  const places = [...byPlace.values()].sort((a, b) => a.name.localeCompare(b.name, "id"));

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Semua tempat</h1>
          <p className="lead">Tempat yang punya data resmi di wilayahmu.</p>
        </div>
      </div>
      <section className="panel panel-list" style={{ marginTop: 20 }} aria-label="Daftar tempat">
        {places.length === 0 ? (
          <p className="empty">Belum ada data resmi.</p>
        ) : (
          <div className="table-wrap">
            <table className="queue">
              <thead>
                <tr>
                  <th scope="col">Tempat</th>
                  <th scope="col">Draf</th>
                  <th scope="col">Terbit</th>
                </tr>
              </thead>
              <tbody>
                {places.map((p) => (
                  <tr key={p.firstKey}>
                    <td>
                      <Link className="row-link" href={`/?q=${encodeURIComponent(p.name)}&data=${p.firstKey}`}>
                        <span className="cat" aria-hidden="true">
                          {p.category.slice(0, 2)}
                        </span>
                        <span className="row-title">{p.name}</span>
                      </Link>
                    </td>
                    <td>{p.pending}</td>
                    <td>{p.published}</td>
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

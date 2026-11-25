"use client";

export default function PanelError({ reset }: { error: Error; reset: () => void }) {
  return (
    <section className="panel panel-block" role="alert">
      <h1>Data tidak bisa dimuat</h1>
      <p className="lead">Periksa koneksi, lalu coba lagi. Kalau terus gagal, akunmu mungkin belum terdaftar sebagai staf.</p>
      <div className="actions">
        <button className="btn btn-primary" type="button" onClick={reset}>
          Coba lagi
        </button>
      </div>
    </section>
  );
}

import { signIn } from "@/app/actions";

const ERRORS: Record<string, string> = {
  kosong: "Isi email dan kata sandi.",
  gagal: "Email atau kata sandi salah.",
};

export default async function MasukPage({ searchParams }: { searchParams: Promise<{ galat?: string }> }) {
  const { galat } = await searchParams;
  const error = galat ? ERRORS[galat] : undefined;
  return (
    <main className="login-wrap">
      <form className="login" action={signIn}>
        <div className="brand-name" style={{ color: "var(--primary)" }}>KELANA</div>
        <h1 style={{ marginTop: 12 }}>Masuk ke Panel verifikasi</h1>
        <p className="lead">Khusus tim dan mitra yang sudah didaftarkan sebagai staf.</p>
        {error ? (
          <p className="flash flash-error" role="alert">
            {error}
          </p>
        ) : null}
        <label className="field">
          <span>Email</span>
          <input name="email" type="email" autoComplete="username" required />
        </label>
        <label className="field">
          <span>Kata sandi</span>
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        <button className="btn btn-primary" type="submit">
          Masuk
        </button>
        <p className="hint">Tidak ada pendaftaran di sini. Akun staf dibuat oleh pemilik proyek.</p>
      </form>
    </main>
  );
}

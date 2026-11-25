import { redirect } from "next/navigation";
import { signOut } from "@/app/actions";
import { NavLink } from "@/components/NavLink";
import { getPlaceNames, getSession, getSummary } from "@/lib/data";
import { ROLE_LABEL } from "@/lib/facts";

export const dynamic = "force-dynamic";

export default async function PanelLayout({ children }: { children: React.ReactNode }) {
  const { user, staff } = await getSession();
  if (!user) redirect("/masuk");

  if (!staff) {
    return (
      <main className="login-wrap">
        <div className="login">
          <h1>Akun ini bukan staf</h1>
          <p className="lead">
            Panel verifikasi hanya untuk tim dan mitra. Minta pemilik proyek menambahkan akunmu ({user.email}) ke
            tabel staf.
          </p>
          <form action={signOut}>
            <button className="btn btn-primary" type="submit">
              Keluar
            </button>
          </form>
        </div>
      </main>
    );
  }

  const [summary, places] = await Promise.all([getSummary(), getPlaceNames()]);
  const scope = staff.regionCode ?? "semua wilayah";

  return (
    <div className="shell">
      <aside className="sidebar" aria-label="Navigasi panel">
        <div className="brand">
          <div className="brand-name">KELANA</div>
          <div className="brand-sub">Panel verifikasi · tim &amp; mitra</div>
        </div>
        <nav style={{ display: "contents" }}>
          <NavLink href="/" count={summary.pending}>
            Antrean verifikasi
          </NavLink>
          <NavLink href="/tempat">Semua tempat ({places.size})</NavLink>
          <NavLink href="/agenda">Agenda &amp; jadwal</NavLink>
          <NavLink href="/laporan" count={summary.reports_7d}>
            Laporan pengguna
          </NavLink>
          <NavLink href="/riwayat">Riwayat perubahan</NavLink>
        </nav>
        <span className="spacer" />
        <div className="whoami">
          Masuk sebagai <strong>{staff.displayName}</strong> · {ROLE_LABEL[staff.role]} {scope}
          <form action={signOut}>
            <button className="link-button" type="submit">
              Keluar
            </button>
          </form>
        </div>
      </aside>
      <main className="main">
        <div className="main-inner">{children}</div>
      </main>
    </div>
  );
}

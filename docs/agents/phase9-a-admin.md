# Phase 9 · Agent A: Panel verifikasi (web pengelola)

Worktree `D:\PROJECT\BudayaGo\m\p9-admin` (branch `feat/admin-panel`). Read `docs/agents/phase9-common.md` first.

## Screen
`docs/design/artifact/PanelPengelola.dc.html` ("Panel verifikasi · tim & mitra"), the last artifact screen with no implementation. It is a desktop web page, not Android.

## What to build (v1)
A Next.js (App Router, TypeScript) app in `admin/` (AGENTS.md: "admin CMS in Next.js"), using `@supabase/ssr` with the **anon key + the staff member's own session**. The service role key is never used in the admin app.
- **Antrean verifikasi**: list official facts (`opening_hours`, `closures`, `ticket_prices`, `events` and any other table with `review_status`) with place, data, last `checked_at`, due date, status. Filters: Semua, Bertentangan, Jatuh tempo, Tinjauan. Summary cards from real counts (no `[ISI]`).
- **Detail panel**: values, `source_url`, `checked_at`, internal note (never shown to travellers), "Diperiksa" (checker) and "Ditinjau pemeriksa kedua" (reviewer), "Terbit ke aplikasi".
- **Four-eyes rule, enforced in the database**, not only in the UI: a fact can only become `published` when a reviewer different from the checker approved it. Implement with a trigger or a `security definer` function; a direct update that skips it must fail.
- **Riwayat perubahan**: an append-only history table written by trigger (before → after, source, who, when).
- **Laporan pengguna**: list reports (read-only v1).
- "Unggah tangkapan layar" and "Kabari pengguna" may be PROVISIONAL (button shown disabled) if they need more than a small job; say so.

## Security (binding)
- Staff identity: a NEW `staff` table (user_id, role `checker` | `reviewer` | `admin`, region) and a `public.is_staff()` / role helper. RLS policies let staff read pending facts and reports and write review fields; travellers keep seeing only `published` (do not weaken existing policies). Write pgTAP tests for: non-staff cannot read pending facts or reports, checker cannot publish alone, same person cannot check and review, history is append-only.
- No self-service staff signup: staff rows are inserted by the project owner (document how in `admin/README.md`).
- Never expose the service role key; no secrets in git (`.env.local` ignored, `.env.example` committed).

## Files you may touch
`admin/**` (new), NEW migration(s) and NEW tests in `supabase/`, `supabase/README.md` (an admin section), `.gitignore` (admin entries), `.github/workflows/` only a NEW workflow file for admin lint/build. Nothing in `android/`.

## Done means
`npm run lint` and `npm run build` in `admin/` pass (node is installed? check; if not, say so). `deno`/`supabase test db` only if available (Docker is likely off: then say the SQL tests were written, not run). Report what ran.

# KELANA admin: Panel verifikasi

Next.js (App Router, TypeScript) web panel for the team and partners who check official facts (hours, closures,
ticket prices, events) before they reach the app. Layout follows `docs/design/artifact/PanelPengelola.dc.html`.

## Security model

- The panel uses only the **anon (publishable) key** plus the staff member's own Supabase Auth session
  (`@supabase/ssr`, cookies). Every query runs under RLS as that person. **The service role key is never used here**;
  do not add it to `.env.local` or to the hosting environment.
- Who is staff is decided by the database table `public.staff` (migration `supabase/migrations/20261123090000_admin_panel.sql`).
  A signed-in account without a staff row sees "Akun ini bukan staf" and nothing else.
- The four-eyes rule is enforced in Postgres, not here: a fact becomes `published` only when one staff member checked
  it ("Diperiksa") and a **different** reviewer or admin approved exactly that content. The panel's buttons only call
  the RPCs `staff_save_fact`, `mark_fact_checked` and `publish_fact`; the database refuses anything else.
- Pending facts are read only through `staff_fact_queue()`. Staff get no extra select policy on the fact tables, so a
  staff account used in the Android app still sees published facts only.
- Internal notes, manager confirmations and who checked what live in `public.fact_reviews`, never in the fact rows that
  travellers read. `public.fact_history` is append-only (triggers block update, delete and truncate for every role).

## Roles

| Role | Can |
|---|---|
| `checker` | read the queue, reports and history; edit drafts; "Minta tinjauan" (marks the fact as checked by them) |
| `reviewer` | everything a checker can, plus "Setujui dan terbitkan" for facts checked by someone else |
| `admin` | same as reviewer (no extra powers in v1) |

`region_id` limits a staff member to the places of one region; `null` means every region.

## Adding staff (project owner only, no self-service signup)

1. Create the person's account: Supabase dashboard > Authentication > Users > "Add user" (email + password), or let
   them sign up in the app once. Email sign-up is not verified (see AGENTS.md), so check the email address yourself.
2. In the SQL editor (runs as `postgres`), insert the staff row:

```sql
insert into public.staff (user_id, role, region_id, display_name)
select u.id, 'checker', r.id, 'Nama Pemeriksa'
from auth.users u, public.regions r
where u.email = 'orang@contoh.id' and r.code = 'DIY';
-- role: 'checker' | 'reviewer' | 'admin'. Use region_id = null for all regions.
```

To change a role: `update public.staff set role = 'reviewer' where user_id = '...';`
To remove someone: `delete from public.staff where user_id = '...';` (their account stays; their past checks stay in
the history). There is no insert/update/delete policy on `staff`, so nobody can do this from the panel or the app.

## Run locally

```
cd admin
cp .env.example .env.local      # fill NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_ANON_KEY
npm install
npm run dev                     # http://localhost:3000
npm run lint
npm run build
```

`.env.local` is git-ignored. Only `.env.example` is committed.

## Screens

| Route | Content |
|---|---|
| `/masuk` | email + password sign-in (staff accounts only) |
| `/` | Antrean verifikasi: summary cards, filters (Semua, Bertentangan, Jatuh tempo, Tinjauan), search, detail panel, Riwayat perubahan of the selected place |
| `/agenda` | the same queue, events only |
| `/tempat` | places with their number of drafts and published facts |
| `/laporan` | Laporan pengguna, read-only |
| `/riwayat` | last 100 history rows |

PROVISIONAL (shown disabled): "Unggah tangkapan layar sumber" (needs a private evidence bucket and policies) and
"Kabari pengguna" (needs an FCM sender job). Report photos are listed but cannot be opened from the panel yet
(the `report-photos` bucket is private to the reporter).

Editing a published fact makes it pending again (it disappears from the app until it is checked and reviewed again).
A scraper or the dashboard may still refresh only `checked_at` of a published fact when the value is unchanged.

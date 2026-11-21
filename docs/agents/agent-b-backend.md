# Agent B: backend and data contract

You are building the Supabase backend and the API contract for KELANA. Feature agents will code against your contract, so precision and tested security rules matter more than volume.

Read first: `AGENTS.md` (repo root, binding product rules), `docs/design/FIGMA-FRAMES.md` (screen inventory; the screens tell you which fields the app needs).

## Scope: only these paths

Work only under `supabase/`. (An empty `backend/` folder exists; ignore it, the planner will remove it.) Do not touch `AGENTS.md`, `docs/`, `android/`, `admin/`. Do not run `git commit` or `git push`.

## Environment

- Supabase CLI is installed (`supabase`). Docker Desktop is running. Use `supabase init`, `supabase start`, `supabase db reset`, `supabase test db`.
- Never put real keys in files. Local keys printed by `supabase start` stay out of git.

## Build this

1. **Schema as migrations** in `supabase/migrations/` (SQL, snake_case English identifiers, Indonesian content values). Enable PostGIS. Tables:
   `regions`, `places` (PostGIS `geography(Point,4326)`, `category` enum: museum, candi, tari, topeng, gamelan, kerajinan, lainnya; `cap_code` like `YK-02`; `cap_radius_m` nullable because the design leaves it `[ISI]`), `opening_hours`, `closures`, `ticket_prices`, `events` (recurring shows such as "setiap Jumat 19.30"; decide a simple recurrence model and document it), `profiles` (1:1 with `auth.users`; this is the app's "users" table), `stamps`, `postcards`, `videos` (visitor video stories, which the design has in 07.03 and 02.06), `reports`, `subscriptions` ("Kabari aku", per category or place).
   - Every official fact table (`opening_hours`, `closures`, `ticket_prices`, `events`) has `source_url`, `checked_at`, `review_status` (`pending` | `published`) and `is_sample boolean`. The app only ever sees `published` rows (enforce in RLS). A fact without `checked_at` must be representable (the app shows "Harga sedang dicek").
   - `stamps`: unique `(user_id, place_id)`, `stamped_at`. Clients can never insert or update stamps (only a service-role Edge Function will, later).
   - `reports`: reasons mirror frame 02.07 (hours/price changed, place closed, ceremony shown without permission, inappropriate photo/video) plus free text and optional photo path.
2. **RLS on every table.** Login is required for everything except reading `regions` (needed on the onboarding region picker). Rules that must hold, each covered by a test:
   - A user can insert a postcard or video for a place **only if** a stamp for that same user and place exists, and only as themselves.
   - No client can insert, update or delete a stamp.
   - Postcards and videos are readable by any authenticated user immediately (no moderation gate).
   - Users can edit or delete only their own postcards, videos, reports, subscriptions, profile.
   - `pending` facts are invisible to clients.
   - Anonymous users cannot read anything except `regions`.
3. **Tests** with pgTAP in `supabase/tests/`, run by `supabase test db`. Cover every rule above, including the negative cases. A rule without a failing-case test does not count.
4. **Seed** (`supabase/seed.sql`, all rows `is_sample = true`): the 10 Jogja places from the passport, in this order with cap codes: Keraton Yogyakarta `YK-01`, Museum Sonobudoyo `YK-02`, Tamansari `YK-03`, then Museum Kereta Wahanarata, Museum Benteng Vredeburg, Puro Pakualaman, Museum Batik Yogyakarta, Tugu Yogyakarta, Kotagede, Candi Prambanan (`YK-04` to `YK-10`). Use real approximate coordinates. Regions: DI Yogyakarta available, plus a few others marked `upcoming` and `not_open` (the design says 2 upcoming and 7 not yet open). Opening hours, tickets and events: use the values visible in the Figma overviews where they exist (Sonobudoyo Selasa-Minggu 08.00-21.00, closed Senin, adult Rp10.000 and child Rp5.000; Prambanan 06.30-17.30; Sendratari Roro Jonggrang every Friday 19.30-20.30 from Rp150.000; Sendratari Ramayana 19.30-21.00; Macapat and Fragmen wayang wong at Bangsal Srimanganti 09.00-11.00) and clearly invented-but-plausible values elsewhere. Sample `checked_at = 2026-11-20`. Also add a sample user, a stamp, and two postcards so story screens have data.
   - **Add a check** (a SQL test or a documented query in the README) that fails when any `is_sample = true` row exists, to be used before a public release. Sample data must never be mistaken for verified data.
5. **API contract** in `supabase/contract/`:
   - `kelana.openapi.yaml` (OpenAPI 3.1): the PostgREST resources and RPCs the app will call, and the Edge Function `POST /functions/v1/verify-stamp` (request: `place_id`, `lat`, `lng`, `accuracy_m`, `captured_at`, `integrity_token`; response: success with the stamp, or failure reasons `out_of_area`, `location_denied`, `expired_request`, `integrity_failed`, `already_stamped`). Define it even though you do not implement the function. Document the 30-minute expiry rule from `AGENTS.md`.
   - `types.ts` generated with `supabase gen types typescript --local` and committed.
   - `README.md`: the table list, the RLS rules in plain language, the seed warning, and a table "Android feature module -> endpoints it uses" for jelajah, tempat, cerita, agenda, paspor, postcard, notifikasi, tiket, rencana, peta, akun, onboarding.
6. A `supabase/README.md` with the exact commands to start, reset, test, and regenerate types.

## Do not

- Implement Edge Functions, the admin site, or the scraper (later tasks).
- Add a payments table or anything that stores user location history. Location is never stored; the stamp keeps only the verified result.
- Weaken a policy to make a test pass. If a rule is wrong, fix the rule and say so.
- Hand-write fake `checked_at` dates outside sample data.

## Done means (run and report each, with real output)

```
supabase start
supabase db reset
supabase test db
supabase gen types typescript --local > supabase/contract/types.ts
```

`db reset` must apply all migrations and the seed cleanly, and `test db` must show every test passing. Final report: commands run and results, table list, any deviation from this brief with the reason, open questions, and anything you could not verify. Do not claim a pass you did not see.

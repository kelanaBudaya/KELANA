# KELANA backend (Supabase)

## Commands (run from the repo root)

```
supabase start                                              # first run pulls images (minutes)
supabase db reset                                           # applies migrations/ then seed.sql
supabase test db                                            # pgTAP tests in tests/
supabase gen types typescript --local > supabase/contract/types.ts
# Windows PowerShell 5.1: do NOT use `>` (it writes UTF-16 and git sees a binary file).
# Use Git Bash, or: supabase gen types typescript --linked | Out-File -Encoding utf8 supabase/contract/types.ts
supabase stop
```

Coordinates: `places.lat` and `places.lng` are generated plain `double precision` columns (migration 000004). The Android app reads these and never parses `places.location` (PostGIS WKB); request `select=id,slug,name,category,cap_code,lat,lng` to leave `location` out. They are read-only.

Local keys printed by `supabase start` stay out of git.

## Tables

regions, places, opening_hours, closures, ticket_prices, events, profiles (1:1 with auth.users),
stamps, postcards, videos, reports, subscriptions; admin: staff, fact_reviews, fact_history (see below). RPCs: `place_facts(place_id)` (client),
`stamp_distance(place_id, lat, lng)` (service role only, for verify-stamp), `release_check()`.

Event recurrence: `once` (event_date), `weekly` (ISO weekday 1=Senin..7=Minggu), `daily` (neither).

Event detail facts (migration 000007): `performers text[]` (default empty), `arrival_advice_min` and `arrival_advice_max` (minutes before the show; both null or both set, min <= max), and `tags text[]` (default empty; each element one of `tari`, `wayang`, `ramah_anak`). Guests read them like any other published event column.
Times are Asia/Jakarta wall-clock. Cancelled dates go in `closures`.

## RLS rules (plain language)

- Guests (no login, role `anon`) can browse: read `regions`, `places` (with `lat`/`lng`), published `opening_hours`, `closures`, `ticket_prices`, `events`, and call `place_facts`. Pending facts stay hidden from guests too.
- Guests cannot read postcards, videos, profiles, stamps, reports or subscriptions, and cannot write anything. Those need login.
- Only `published` facts (hours, closures, prices, events) are visible; a fact with `checked_at` null must be shown as "Harga sedang dicek".
- Stamps: clients can only read their own; nobody on the client side can insert, update or delete (a service-role Edge Function will).
- Postcards and videos: any logged-in user can read them immediately; inserting needs `user_id = self` and a stamp for the same place. A postcard with `is_public = false` is visible to its author only (`postcards.rating` 1 to 5 or null, `card_style` `jurnal`/`cap`/`peta`).
- Visitor stories are for logged-in users only; the guest browse set above is the whole guest surface.
- Own rows only for editing/deleting postcards, videos, reports, subscriptions, profile.

## Admin panel: staff, four-eyes, history (migration 20261123090000)

The web panel in `admin/` signs in as the staff member (anon key + session, never the service role). See `admin/README.md` for adding staff.

- `staff` (user_id, role `checker` | `reviewer` | `admin`, region_id null = all, display_name). Rows are inserted by the project owner in the SQL editor; there is no insert/update/delete policy. Staff can read the staff list. Helpers: `is_staff()`, `current_staff_role()`, `is_staff_for_place(place_id)`.
- `fact_reviews` (one row per fact: internal_note, manager_confirmation, has_conflict, due_on, checker, reviewer, content snapshots). Separate from the fact tables so internal data never reaches travellers (`place_facts` returns whole fact rows). Readable by staff of the place's region; written only through the RPCs.
- `fact_history`: append-only (before, after, source_url, changed_by = `auth.uid()` or null for scraper/dashboard, changed_at), written by triggers on every insert/update/delete of a fact and on every check and review. Triggers reject update, delete and truncate for every role, postgres included; API roles have no write privilege at all. No foreign keys, so it outlives deleted places and accounts.
- **Four-eyes rule (database-enforced):** a `BEFORE INSERT OR UPDATE` trigger on `opening_hours`, `closures`, `ticket_prices`, `events` lets a row become or stay `published` with changed content only when `fact_reviews` has a checker and a different reviewer whose snapshots equal the row's current content (all columns except `review_status` and `checked_at`). A guard trigger on `fact_reviews` only lets the caller (`auth.uid()`) set themselves as checker or reviewer, requires reviewer/admin role for the review, and computes the snapshots itself, so service_role and postgres cannot forge a review either. Consequences:
  - Inserting an already `published` fact is refused for every role except the owner roles `postgres`/`supabase_admin` (seed and migrations). The scraper (service role) inserts `pending`.
  - Changing a value of a published fact is refused unless the row goes back to `pending`. Refreshing only `checked_at` of an unchanged published fact is allowed ("an unchanged re-check just refreshes checkedAt").
  - A value change after the check makes the check stale; the reviewer gets `stale_check` and the fact must be checked again.
- RPCs (authenticated, each checks the staff row and region): `staff_fact_queue()`, `staff_summary()`, `staff_save_fact(kind, id, patch, note, confirmation, has_conflict, due_on)` (a content change sends the fact back to pending), `mark_fact_checked(kind, id)` (needs `source_url` and `checked_at`), `publish_fact(kind, id)` (reviewer/admin, not the checker: approves and publishes in one transaction). Errors: `four_eyes_required`, `stale_check`, `reviewer_role_required`, `not_staff`, `fact_not_pending`, `check_needs_source_and_date`.
- Unchanged for travellers: no new policy on the fact tables (staff read pending facts only through `staff_fact_queue()`, so a staff account in the Android app still sees published facts only). `reports` gets one extra select policy for staff of the reported place's region (read-only). Report photos stay private to the reporter.
- Tests: `tests/admin.test.sql`.

## Storage

Migration `20261121000006_storage.sql` creates two private buckets (5 MB limit; image/jpeg, image/png, image/webp). There is no video bucket: video lives in Cloudflare Stream/Mux.

| Bucket | Path | Insert | Read | Update / delete |
|---|---|---|---|---|
| `postcards` | `<user_id>/<place_id>/<filename>` | logged in, own folder, and only for a place the user holds a stamp for | any logged-in user | own folder only (update keeps the stamp rule) |
| `report-photos` | `<user_id>/<filename>` | logged in, own folder | own folder only (nobody else) | delete own only, no update |

Guests (anon) get nothing in Storage. Save the object path in `postcards.photo_path` / `reports.photo_path`. Policies are on `storage.objects`, each scoped by `bucket_id`.

The migration is safe on top of buckets created by hand in the dashboard: it upserts the buckets (`on conflict (id) do update set public = false, file_size_limit = ..., allowed_mime_types = ...`) so they end up private with the intended limits, and it runs `drop policy if exists` before every `create policy`, so a same-named dashboard policy cannot break `db push`. Any other hand-made policy with a different name stays and is OR-ed with ours: delete it in the dashboard if it is more permissive.

`dev/storage_stub.sql` is NOT a migration. It stands in for the storage schema only where `supabase start` cannot run (see Windows note); never apply it on a working stack.

## Windows note

On this machine `supabase start` fails with exit 139 (the CLI's Realtime init container segfaults under Docker/WSL2). Fallback used for verification: run `public.ecr.aws/supabase/postgres:17.11.0.002` directly, run `auth migrate` from the gotrue image, apply `migrations/*.sql` then `seed.sql` with psql, then apply `dev/storage_stub.sql` as supabase_admin BEFORE the migrations (storage-api also segfaults here, so the storage schema is a stub), then `supabase test db --db-url postgresql://postgres:postgres@127.0.0.1:54322/postgres` (needs a docker network named `supabase_network_kelana` with the db aliased `db`). On a machine where `supabase start` works, use the normal commands.

## Seed warning

`seed.sql` is SAMPLE data (`is_sample = true`), not verified. Before a public release run
`select public.release_check();` (e.g. `supabase db query` or psql). It raises an error while any sample row remains.
The same function is covered by `tests/rls.test.sql`.

## Edge Function verify-stamp

`functions/verify-stamp/` (Deno). `POST /functions/v1/verify-stamp`, `verify_jwt = true` in `config.toml`; the user id comes from the JWT. Contract in `contract/kelana.openapi.yaml`. Pure logic in `verify.ts`, HTTP in `handler.ts`, Play Integrity in `integrity.ts`, wiring (Supabase client, env) in `index.ts`.

Check order: body shape (400 `bad_request`), `isMock`, accuracy, time window, Play Integrity, existing stamp (`already_stamped`), place and radius via `stamp_distance`, insert. Every decision is HTTP 200 with `status` (`stamped` | `already_stamped` | `rejected`) and an optional `reason`; `distanceM` only with `too_far`. A null `cap_radius_m` gives `radius_not_set` and never a stamp. Idempotent through the unique `(user_id, place_id)`.

PROVISIONAL constants (all in `verify.ts`, change only there):

| Constant | Value | Meaning |
|---|---|---|
| `MAX_CAPTURE_AGE_MS` | 30 min | older fix is `expired` (exactly 30 min is accepted) |
| `MAX_FUTURE_SKEW_MS` | 2 min | later capture time is `future_timestamp` |
| `MAX_ACCURACY_M` | 100 | worse accuracy, or negative/non-finite, is `low_accuracy` |
| `EXPECTED_PACKAGE_NAME` | `id.kelana.app` | package the integrity token must belong to |

The cap radius is not a constant: it is `places.cap_radius_m`, still to be decided per place.

Nonce the Android client must send to Play Integrity (`setNonce`):
`nonce = base64url_no_padding(sha256_utf8(canonical))`, `canonical = placeId|lat|lng|accuracyM|capturedAt` where
`placeId` is the lowercase UUID, `lat` and `lng` are fixed 6 decimals (`-7.805000`), `accuracyM` is fixed 1 decimal (`20.0`), and `capturedAt` is the exact string sent in the body: UTC with exactly 3 millisecond digits (`2026-11-21T10:15:30.123Z`). Use `.` as the decimal separator regardless of locale, and send `lat`/`lng` already rounded to 6 decimals so client and server formatting agree. A negative zero prints as `0.000000`. The nonce is 43 characters (`A-Z a-z 0-9 - _`).

Play Integrity: `IntegrityVerifier` interface. The real verifier calls Google `decodeIntegrityToken` and checks nonce, package name, `PLAY_RECOGNIZED` and `MEETS_DEVICE_INTEGRITY`. It is NOT exercised without a Google Cloud setup. Without `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON` the function fails closed (`integrity_failed`), unless `KELANA_ENV=dev`, where a stub accepts only the token `dev-token` (never set `KELANA_ENV=dev` in production).

Env (never commit values): `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` (both provided by the platform), `PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON`, `PLAY_INTEGRITY_PACKAGE_NAME` (optional), `KELANA_ENV`.

Privacy: lat, lng and accuracy are never logged or stored. Logs hold only `status` and `reason`. Only the existing `stamps` row is written.

Tests (no network, injected fakes). Deno is portable on this machine; keep its cache on D:.

```
cd supabase/functions/verify-stamp
DENO_DIR=D:/deno-cache D:/Devtools/deno/deno.exe test verify_test.ts
DENO_DIR=D:/deno-cache D:/Devtools/deno/deno.exe check index.ts
```

Deploy (needs the user): `supabase functions deploy verify-stamp`, then `supabase secrets set ...`.

## Edge Function delete-account (Hapus akun)

`functions/delete-account/` (Deno). `POST /functions/v1/delete-account` with body `{ "confirm": "HAPUS" }`, `verify_jwt = true` in `config.toml`. The account deleted is always the JWT's user; any id in the body is ignored. Pure logic in `account.ts`, HTTP in `handler.ts`, wiring in `index.ts`.

What it does: removes the caller's postcard photos (bucket `postcards`, folder `<user_id>/`) through the Storage API, then hard-deletes the auth user with the service role. The database cascades profiles, stamps, postcards, videos and subscriptions, and keeps the caller's reports with `user_id` set to null (migration `20261123080000_account_deletion.sql`); report photos stay with them. Any failing step answers 500 and the user is not deleted, so the client keeps its session and may retry. Answers: 200 `{ "status": "deleted" }`, 400 `bad_request`, 401 `unauthorized`, 405, 500 `server_error`. Logs never carry the user id.

Env (provided by the platform): `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`. The service role key stays on the server.

## Edge Function export-account (Unduh dataku dulu)

`functions/export-account/` (Deno). `POST /functions/v1/export-account` (body ignored), `verify_jwt = true` in `config.toml`. Pure logic in `export.ts`, HTTP in `handler.ts`, wiring in `index.ts`.

What it does: returns one JSON document (`format: "kelana-account-export"`, `version: 1`) with the caller's own data: account (id, email, created), profile (display name, home region), stamps (place and time), postcards (message, rating, card style, visibility, photo link), videos (caption, duration, visibility), reports they sent, and "Kabari aku" subscriptions. No location (none is stored), no other user's rows, no internal columns (`is_sample`, Storage paths, stream ids). Photo links are signed URLs that expire after 10 minutes (`PHOTO_URL_TTL_S`), made only for objects in the caller's own folder.

Who reads what: the user id comes from the verified JWT only. The function reads with a client that carries the caller's JWT and the anon key, so RLS applies, and every query also filters on that id; `export.ts` drops any row of another owner again and copies fields by allowlist. No service role is used. Answers: 200 the document (`cache-control: no-store`), 401 `unauthorized`, 405, 500 `server_error` (a failed read fails the whole export; a photo that cannot be signed only loses its link). The Android app saves the file through the Storage Access Framework as `kelana-dataku-<yyyy-MM-dd>.json`.

Env (provided by the platform): `SUPABASE_URL`, `SUPABASE_ANON_KEY`.

Tests and deploy for both functions (tests use injected fakes, no network):

```
cd supabase/functions/delete-account && DENO_DIR=D:/deno-cache D:/Devtools/deno/deno.exe test delete_account_test.ts
cd supabase/functions/export-account && DENO_DIR=D:/deno-cache D:/Devtools/deno/deno.exe test export_account_test.ts
supabase functions deploy delete-account
supabase functions deploy export-account
```

Deploy needs the user (linked project). Keep `verify_jwt = true`; never deploy either with `--no-verify-jwt`.

## Android feature module -> endpoints

| Module | Endpoints (guest = no login needed) |
|---|---|
| onboarding | GET regions (guest), PATCH profiles (home_region_id, login) |
| akun | Supabase Auth (Google / phone OTP), GET/PATCH/DELETE profiles (login), POST functions/v1/delete-account and functions/v1/export-account (login) |
| jelajah | GET places (guest), GET events (guest), GET stamps (login, header card; hide for guests) |
| tempat | GET places (guest), RPC place_facts (guest), GET postcards/videos by place (login), POST reports (login) |
| cerita | login only: GET/POST/PATCH/DELETE postcards and videos, Realtime on both tables, POST reports |
| agenda | GET events (guest), POST subscriptions (login) |
| paspor | login only: GET stamps, POST functions/v1/verify-stamp; GET places (guest) |
| postcard | login only: POST postcards, Storage upload to `postcards/<uid>/<place_id>/<file>` (needs stamp) |
| notifikasi | login only: GET/POST/DELETE subscriptions |
| tiket | GET ticket_prices (guest), events.seller_url (opened in Custom Tab) |
| rencana | GET places, opening_hours, closures, events (all guest; planner Edge Function later) |
| peta | GET places (guest) |

Full request/response shapes: `contract/kelana.openapi.yaml`.

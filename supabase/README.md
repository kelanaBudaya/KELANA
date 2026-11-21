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
stamps, postcards, videos, reports, subscriptions. RPCs: `place_facts(place_id)` (client),
`stamp_distance(place_id, lat, lng)` (service role only, for verify-stamp), `release_check()`.

Event recurrence: `once` (event_date), `weekly` (ISO weekday 1=Senin..7=Minggu), `daily` (neither).
Times are Asia/Jakarta wall-clock. Cancelled dates go in `closures`.

## RLS rules (plain language)

- Guests (no login, role `anon`) can browse: read `regions`, `places` (with `lat`/`lng`), published `opening_hours`, `closures`, `ticket_prices`, `events`, and call `place_facts`. Pending facts stay hidden from guests too.
- Guests cannot read postcards, videos, profiles, stamps, reports or subscriptions, and cannot write anything. Those need login.
- Only `published` facts (hours, closures, prices, events) are visible; a fact with `checked_at` null must be shown as "Harga sedang dicek".
- Stamps: clients can only read their own; nobody on the client side can insert, update or delete (a service-role Edge Function will).
- Postcards and videos: any logged-in user can read them immediately; inserting needs `user_id = self` and a stamp for the same place.
- Visitor stories are for logged-in users only; the guest browse set above is the whole guest surface.
- Own rows only for editing/deleting postcards, videos, reports, subscriptions, profile.

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

## Android feature module -> endpoints

| Module | Endpoints (guest = no login needed) |
|---|---|
| onboarding | GET regions (guest), PATCH profiles (home_region_id, login) |
| akun | Supabase Auth (Google / phone OTP), GET/PATCH/DELETE profiles (login) |
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

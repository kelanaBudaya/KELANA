# KELANA backend (Supabase)

## Commands (run from the repo root)

```
supabase start                                              # first run pulls images (minutes)
supabase db reset                                           # applies migrations/ then seed.sql
supabase test db                                            # pgTAP tests in tests/
supabase gen types typescript --local > supabase/contract/types.ts
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

- Login is required for everything except reading `regions`.
- Only `published` facts (hours, closures, prices, events) are visible; a fact with `checked_at` null must be shown as "Harga sedang dicek".
- Stamps: clients can only read their own; nobody on the client side can insert, update or delete (a service-role Edge Function will).
- Postcards and videos: any logged-in user can read them immediately; inserting needs `user_id = self` and a stamp for the same place.
- Own rows only for editing/deleting postcards, videos, reports, subscriptions, profile.

## Windows note

On this machine `supabase start` fails with exit 139 (the CLI's Realtime init container segfaults under Docker/WSL2). Fallback used for verification: run `public.ecr.aws/supabase/postgres:17.11.0.002` directly, run `auth migrate` from the gotrue image, apply `migrations/*.sql` then `seed.sql` with psql, then `supabase test db --db-url postgresql://postgres:postgres@127.0.0.1:54322/postgres` (needs a docker network named `supabase_network_kelana` with the db aliased `db`). On a machine where `supabase start` works, use the normal commands.

## Seed warning

`seed.sql` is SAMPLE data (`is_sample = true`), not verified. Before a public release run
`select public.release_check();` (e.g. `supabase db query` or psql). It raises an error while any sample row remains.
The same function is covered by `tests/rls.test.sql`.

## Android feature module -> endpoints

| Module | Endpoints |
|---|---|
| onboarding | GET regions, PATCH profiles (home_region_id) |
| akun | Supabase Auth (Google / phone OTP), GET/PATCH/DELETE profiles |
| jelajah | GET places, GET events, GET stamps (header card) |
| tempat | GET places, RPC place_facts, GET postcards/videos (by place), POST reports |
| cerita | GET/POST/PATCH/DELETE postcards and videos, Realtime on both tables, POST reports |
| agenda | GET events, POST subscriptions |
| paspor | GET stamps, GET places, POST functions/v1/verify-stamp |
| postcard | POST postcards, Storage upload (photo_path) |
| notifikasi | GET/POST/DELETE subscriptions |
| tiket | GET ticket_prices, events.seller_url (opened in Custom Tab) |
| rencana | GET places, opening_hours, closures, events (planner Edge Function later) |
| peta | GET places |

Full request/response shapes: `contract/kelana.openapi.yaml`.

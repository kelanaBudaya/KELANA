# Phase 4: Edge Function `verify-stamp`

Worktree `D:\PROJECT\BudayaGo\m\stamp` (branch `feat/verify-stamp`). Do not commit or push. Edit only `supabase/` (function, tests, contract, README, config.toml entry). Read first: `AGENTS.md` (product rules on stamps, location, 30-minute expiry, Play Integrity), `supabase/README.md`, migrations 000001 (stamps, places.cap_radius_m), 000003 (`stamp_distance` RPC, service_role only), 000002 (RLS), `supabase/contract/kelana.openapi.yaml`.

## Goal
`supabase/functions/verify-stamp/` (Deno, TypeScript). The server, never the client, decides whether a stamp is earned. Verify then insert into `public.stamps` with the service role.

## Contract
`POST /functions/v1/verify-stamp`, JWT required (`verify_jwt = true`); the user id comes from the JWT, never from the body.
Body: `{ placeId: uuid, lat, lng, accuracyM, capturedAt: ISO-8601, isMock: boolean, integrityToken: string }`.
Response 200: `{ status: "stamped" | "already_stamped" | "rejected", reason?: ... }` with reason one of `too_far`, `expired`, `future_timestamp`, `low_accuracy`, `mock_location`, `integrity_failed`, `place_unknown`, `radius_not_set`, `bad_request`. Use 401 for missing/invalid JWT, 400 for malformed body (`bad_request`), 500 only for server faults. Return `distanceM` only on `too_far`.

## Decisions (PROVISIONAL, put each in one named constant, listed in the README)
- Capture window: reject when `now - capturedAt > 30 min` (`expired`), or `capturedAt > now + 2 min` (`future_timestamp`).
- Accuracy: reject `accuracyM > 100` (`low_accuracy`); also reject non-finite or negative.
- Radius: use `places.cap_radius_m` via the `stamp_distance` RPC. If null, reject `radius_not_set` (do NOT invent a default and never stamp). `within_radius` from the RPC decides.
- `isMock === true` is rejected (`mock_location`); it is defense in depth only, the integrity check is the real gate.
- Play Integrity: put verification behind an interface `IntegrityVerifier`. The nonce must equal `base64url(sha256(canonical string))` where canonical = `placeId|lat|lng|accuracyM|capturedAt` (exact format documented in README so the Android side can match; lat/lng/accuracy formatting must be unambiguous, e.g. fixed 6 / 1 decimals, stated). Real implementation calls Google `decodeIntegrityToken` (needs a service account secret) and checks nonce match, package name `id.kelana.app`, and device/app verdicts. If the secret is not set, the function fails closed (`integrity_failed`) unless env `KELANA_ENV=dev`, where a documented stub accepts the token `dev-token`. State clearly that the real verifier is not exercised without a Google Cloud setup.
- Idempotent: insert with unique (user_id, place_id); an existing stamp returns `already_stamped`.
- Privacy: never log or store lat, lng or accuracy. Store only the verified result (the existing `stamps` row). Logs contain status and reason only.
- Config via env only (`SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `PLAY_INTEGRITY_*`, `KELANA_ENV`). Never print or commit secrets.

## Structure and tests
Pure logic in `verify.ts` (validation, window, accuracy, nonce canonicalization, decision), I/O in `index.ts`. Deno tests for: every reject reason, boundaries (exactly 30 min, exactly 100 m accuracy, radius edge), nonce canonicalization and tamper (changing any field changes the nonce), fail-closed integrity, idempotency, no location in logs (spy on console). Use injected fakes for the DB and verifier; no network in tests.
`deno` is not installed. C: is full, so install a portable Deno into `D:\Devtools\deno` (zip from the official GitHub releases) and keep its cache on D: (`DENO_DIR=D:\deno-cache`). If you cannot install it, stop and report; do not claim tests ran.
Also add an SQL/pgTAP check if useful that `stamp_distance` is not callable by anon/authenticated (already covered? check before adding; do not duplicate). Update OpenAPI (Redocly lint must pass) and `supabase/README.md`.

## Done means
Deno tests pass and OpenAPI lints; final report with commands and real results, constants chosen, and what is unverified (nothing deployed, nothing run against Supabase or Google). Put "Needs from user" for: deploying (`supabase functions deploy verify-stamp`), setting secrets, Google Cloud Play Integrity setup, deciding real `cap_radius_m` per place.

# Phase 5 core: stamp model, repository, queue, Play Integrity (`:core:*` only, one PR)

Worktree `D:\PROJECT\BudayaGo\m\phase5` (branch `feat/phase5-planner-core`). Do not commit or push. Run Gradle only via `bash /d/tmp-kelana/gradle.sh /d/PROJECT/BudayaGo/m/phase5 <tasks>`. Never print local.properties values, never delete /d/tmp-kelana/gradle.lock. Read first: `AGENTS.md` (stamp rules: read location once, queue with 30-min expiry, server decides, Play Integrity token at SEND time with nonce bound to the payload hash and never stored), `supabase/README.md` (verify-stamp contract, nonce format, constants), `supabase/functions/verify-stamp/verify.ts` (exact request/response and canonicalization), `docs/agents/phase2-common.md`.

## Scope: `:core:model`, `:core:navigation`, `:core:data`, `:core:database`, version catalog. NO feature module code (`:feature:paspor` comes next).

### `:core:model`
`Stamp` (placeId, stampedAt), `StampResult` (Stamped / AlreadyStamped / Rejected(reason, distanceM?) with the exact server reasons: too_far, expired, future_timestamp, low_accuracy, mock_location, integrity_failed, place_unknown, radius_not_set, bad_request) plus client-side outcomes (NeedsLogin, NetworkError, NotConfigured, Expired locally), `PendingStamp` (placeId, capturedAt, no extra fields beyond what the queue must hold). `StampRepository`: `stamps(): Flow<List<Stamp>>` (own stamps), `requestStamp(request)` taking an already-captured fix (lat, lng, accuracyM, isMock, capturedAt) so the feature does the single location read and this layer never reads location, `pending(): Flow<PendingStamp?>`, `cancelPending()`.

### `:core:navigation`
`AmbilCapRoute(placeId)`, `CapBerhasilRoute(placeId)`; keep `PasporRoute`.

### `:core:database` + `:core:data`
- Room: table for the single pending stamp request (placeId, lat, lng, accuracyM, isMock, capturedAt epoch millis). It holds the fix only until the server answers: deleted on ANY server answer (stamped, already_stamped, rejected) and when older than 30 minutes (client drops it, result Expired). Own stamps cached in Room for offline Paspor (cache refreshed from the server `stamps` table, own rows only via RLS).
- `SupabaseStampRepository`: calls `POST /functions/v1/verify-stamp` with the user's JWT (supabase-kt functions or a plain authenticated HTTP call; check what is on the classpath and prefer the existing client; if the functions module is needed add it to the catalog). `capturedAt` must be formatted exactly `yyyy-MM-ddTHH:mm:ss.SSSZ` UTC; lat/lng rounded to 6 decimals; format numbers with Locale.ROOT.
- Nonce: a pure, tested function producing `base64url_no_padding(sha256_utf8(placeId|lat6|lng6|accuracy1|capturedAt))`, identical to the server's `canonicalString`/`computeNonce` in verify.ts. Tests with fixed vectors: negative coordinates (`-7.805000`), accuracy `20.0`, `2026-11-21T10:15:30.123Z`, negative zero prints `0.000000`, decimal separator is `.` even under an id-ID default locale. Cross-check the expected nonce values by running the Deno `computeNonce` on the same inputs (deno is at D:/Devtools/deno/deno.exe, set DENO_DIR=D:/deno-cache) and hard-code those vectors in the Kotlin test.
- Play Integrity: an `IntegrityTokenProvider` interface in `:core:data` with an implementation using `com.google.android.play:integrity` (StandardIntegrityManager or classic IntegrityManager; pick the simpler supported one and say why) that requests the token at SEND time with the nonce. The token is never stored in Room. A debug/dev implementation returning `dev-token` selectable by BuildConfig/flag for use with the server's KELANA_ENV=dev stub; production default is the real provider. Cloud project number comes from config (local.properties key `google.cloudProjectNumber`, optional, empty means NotConfigured); never commit values.
- WorkManager job (`androidx.work`, Hilt worker) that sends the pending request when the network is available (constraint CONNECTED), with backoff; it requests the Integrity token at send time, calls the repository, deletes the row on any server answer, drops it when older than 30 minutes. `requestStamp` enqueues the job when offline; when online it may send directly. Never keep a fix after an answer.
- Version catalog: `play-services-location` (only if a data-layer class needs it; otherwise just add it for the next phase), `com.google.android.play:integrity`, `androidx.work` runtime + hilt-work. Keep versions consistent with existing BOMs; verify they resolve offline-safe from the Gradle cache or download (C: is nearly full, all caches are on D:).
- Bind in Hilt (`RepositoryModule`/`DataModule`). A Fake implementation for previews/tests is fine.
- Privacy: never log or persist lat/lng/accuracy outside the pending row; no location in analytics, no logging of request bodies.

### Backend sample data (planner-owned, tiny)
In `supabase/seed.sql` only, set `cap_radius_m` for the sample places to a placeholder dev value (e.g. 150) for `is_sample = true` rows ONLY. Also write the same change as a standalone `UPDATE` in `supabase/dev/sample_cap_radius.sql` (NOT a migration) so the owner can run it on the cloud project. Real radii remain a team decision; say so in the file header.

### Tests (JUnit + Turbine)
Nonce vectors; 30-minute expiry boundary (exactly 30 min allowed, older dropped); pending row deleted after each kind of server answer; no stamp appears in `stamps()` before the server answers `stamped`; NeedsLogin for a guest; guest/NotConfigured paths; request body formatting (capturedAt, rounding, Locale.ROOT). Use fakes for network and integrity; no live calls.

## Done means
`assembleDebug lintDebug testDebugUnitTest :core:model:test` pass. Final report: commands and real results, files added, library versions chosen, how the Integrity provider and WorkManager were wired, what is unverified (no device, no real Play Integrity, verify-stamp not deployed, nothing run against Supabase), and Needs from user/planner.

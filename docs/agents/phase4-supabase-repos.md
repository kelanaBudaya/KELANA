# Phase 4: real Supabase repositories (replace the fakes)

Worktree `D:\PROJECT\BudayaGo\m\data` (branch `feat/supabase-repos`). Do not commit or push. Run Gradle only via `bash /d/tmp-kelana/gradle.sh /d/PROJECT/BudayaGo/m/data <tasks>`. Read first: `AGENTS.md`, `supabase/README.md`, `supabase/contract/kelana.openapi.yaml`, `supabase/contract/types.ts`, `supabase/seed.sql`, `docs/agents/phase2-common.md`.

## Goal
Implement `PlaceRepository`, `EventRepository`, `RegionRepository`, `ReportRepository` (interfaces in `:core:model`, unchanged) against the live Supabase project, with a Room cache so browsing works offline. Bind them in `RepositoryModule` (the single binding point). Keep the Fake repositories and their tests for previews/tests, but production binds the Supabase ones.

## Rules
- Edit only `android/core/data`, `android/core/database`, `android/core/network`, `android/gradle/libs.versions.toml` (add supabase-postgrest if needed, matching the existing supabase BOM). No feature module changes, no model interface changes. If a model change is needed, list it under "Needs from planner".
- Guests (anon key, no login) must work: read regions, places (use generated `lat`/`lng`, never `location`), published `opening_hours`, `closures`, `ticket_prices`, `events`. Use `place_facts(place_id)` RPC only if simpler than table reads.
- Official facts: map to `OfficialFact(value, sourceUrl, checkedAt)`. A fact with `checked_at` null becomes the Checking state (never a fabricated date). Only published facts are returned by RLS; do not filter client-side in a way that hides the rule.
- Events: handle recurrence `once` / `weekly` (ISO weekday 1=Senin) / `daily`; times are Asia/Jakarta wall-clock; cancelled dates come from `closures`. Reuse the existing fake's expansion logic for `eventsOn`/`eventsBetween` rather than reimplementing differently; extract shared logic if needed.
- Cache: Room entities for regions, places (+facts), events. Flow emits cached rows first, then refreshes from network; on network failure keep cache and do not touch `checkedAt`. The cache never stores user location or any visitor content.
- Reports: `ReportRepository.submit` inserts into `reports` as the logged-in user (supabase-kt session); a guest gets `ReportResult.NeedsLogin`. Photo upload to bucket `report-photos` at `<user_id>/<filename>` only if `ReportDraft` already carries a photo; otherwise skip and say so.
- Region selection: persist `selectedRegionId` in DataStore (replace the in-memory PROVISIONAL), only available regions selectable.
- Secrets: never print or commit `local.properties` values. If SUPABASE_URL is empty, bind-time behavior must not crash: repositories emit empty lists plus an error-free "not configured" state or fall back to the Fake (state which you chose).
- Tests: JUnit + Turbine. Test mappers (DTO to model, null `checked_at`, weekday mapping, recurrence), cache-then-network ordering, and network-failure fallback, using a fake remote data source interface (no real network in unit tests).
- Do NOT call the live network from tests. A separate live smoke test is optional: a clearly separated, skipped-by-default test is fine.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Final report: commands and real results, files added, how fallbacks behave, what is unverified (nothing run against live Supabase or on a device unless you actually did), and "Needs from planner".

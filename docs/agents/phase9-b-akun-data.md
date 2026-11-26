# Phase 9 · Agent B: account data follow-ups (Unduh dataku, clean-up, deploy config)

Worktree `D:\PROJECT\BudayaGo\m\p9-akundata` (branch `feat/akun-data`). Read `docs/agents/phase9-common.md` first.

Follow-ups from PR #27 (Hapus akun).

## 1. "Unduh dataku dulu" (data export)
The link on the Hapus akun sheet (`HapusAkunScreen.kt`) is inert. Make it work:
- NEW Edge Function `supabase/functions/export-account/` (same structure as `delete-account/`): user id **only from the verified JWT**, returns a JSON document of that user's own data (profile, stamps with place and time, postcards and videos metadata with captions and visibility, reports they sent, subscriptions). No other user's data, no internal fields of official data, no signed URLs that live longer than needed (if you include photo links, short-lived signed URLs). Deno tests like `delete-account`.
- Android: call it through a repository (NEW interface in `:core:model`, NEW implementation in `:core:data` + NEW Hilt module), then let the user save the file with `ActivityResultContracts.CreateDocument("application/json")` (Storage Access Framework, no storage permission). Filename `kelana-dataku-<yyyy-MM-dd>.json`. States: idle, exporting, saved, error with retry. The device's location is never part of the export (none is stored).

## 2. Local clean-up on account deletion
Today deletion does not clear the notification centre (notifikasi DataStore `notification_center_json`) or the in-memory reminders in `PlanCache`. Add one NEW interface in `:core:model`, e.g. `UserDataCleaner { suspend fun clear() }`, with a Hilt multibinding (`@IntoSet`) so each owner registers its own cleaner: notifikasi (its DataStore and scheduled reminders via its existing `ReminderScheduler`), rencana (`PlanCache` reminders if any), and wire the existing paspor/plan clean-up through it too if that is a small change. `HapusAkunViewModel` calls all cleaners after a successful delete (still before clearing the session, still not cancellable). Extend its unit test.

## 3. Deploy config
`supabase/config.toml`: add `[functions.delete-account]` and `[functions.export-account]` with `verify_jwt = true`. `supabase/README.md`: a section for both functions (what they do, deploy command).

## Files you may touch
`:feature:akun` (only `HapusAkunScreen.kt`, `HapusAkunViewModel.kt`, their tests, strings), NEW files in `:core:model` / `:core:data` (+ new Hilt modules), the data/di packages of `:feature:notifikasi` and `:feature:rencana` (NEW cleaner classes + their Hilt binding; do not change UI files there), `supabase/functions/export-account/**`, `supabase/config.toml`, `supabase/README.md`, `supabase/contract/kelana.openapi.yaml`. Do NOT change colour constants in `HapusAkunScreen.kt` (Agent C replaces them).

## Done means
Full Gradle check passes; `deno test` for both functions passes (deno is at `/d/Devtools/deno/deno.exe`). ViewModel tests cover export success / error and that every cleaner runs on successful delete and none on failure.

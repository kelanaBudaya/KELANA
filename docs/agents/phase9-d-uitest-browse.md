# Phase 9 · Agent D: Compose UI tests, browse flows

Worktree `D:\PROJECT\BudayaGo\m\p9-uibrowse` (branch `test/ui-browse`). Read `docs/agents/phase9-common.md` first.

## Why
The Definition of Done asks for "relevant instrumented/Compose tests"; none exist except the smoke test in `:core:designsystem`. The planner runs them on the emulator during UAT before the merge.

## What to write
Compose UI tests in `src/androidTest/kotlin/...` of each module below, testing the **stateless content composables** with fixed UI states (use the existing preview/sample data or build states by hand). No Hilt, no network. Wrap in `KelanaTheme`. Prefer `onNodeWithText` with strings from resources (`InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string...)`) so tests follow `strings.xml`.

| Module | Screens and what to assert |
|---|---|
| `:feature:onboarding` | Mulai → Kenalan pager advances; Pilih daerah: DI Yogyakarta selectable, a "segera hadir" region is not |
| `:feature:jelajah` | Home shows category tiles and Paspor card; Cari empty/no-result state; Kategori Topeng empty state (9.3) |
| `:feature:tempat` | Detail tempat: a fact with `checkedAt` shows "Diperiksa <date>"; a fact without shows "sedang dicek" and no value; Laporkan: send disabled until a reason is chosen; Laporan terkirim |
| `:feature:agenda` | Day tiles switch; detail pentas "Beli tiket resmi" calls back with the official URL |
| `:feature:tiket` | Price with date vs "Harga sedang dicek"; empty and error states with retry |
| `:feature:peta` | Filter chips; place card opens on pin select (callback) |
| `:feature:rencana` | Rencana kosong CTA; Susun step 1→2 next enabled only after a date; Tanpa internet state shows cached plan readable |

If a screen's content composable is private and cannot be tested without changing it, make the smallest visibility change (`private` → `internal`) and list it; do not refactor.

## Files you may touch
`src/androidTest/**` of the modules above; visibility-only changes in their `src/main`. Nothing else.

## Done means
`assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest` pass (the tests compile; the planner runs them). List every test with what it asserts.

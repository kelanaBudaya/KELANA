# Phase 8 · Agent A: Tiket resmi (9.5)

Worktree `D:\PROJECT\BudayaGo\m\p8-tiket` (branch `feat/tiket`). Read `docs/agents/phase8-common.md` first; it is binding.

## Screen
| No. | Screen | Artifact file | Module |
|---|---|---|---|
| 9.5 | Tiket resmi | `KatTiket.dc.html` | `:feature:tiket` |

`TiketScreen.kt` is still a `PlaceholderScreen`. It is reached from the Jelajah home tile (`TiketRoute`). Replace it with the real screen.

## What it is
A list of places and shows with their official ticket price, each price shown only as an `OfficialFact` with its check date, or "Harga sedang dicek" when there is no `checkedAt`. No payments: the action opens the official seller through the existing `OfficialSiteRoute` confirm sheet ("Ke situs tiket resmi", 3.4), which opens a Custom Tab. Look at how `:feature:tempat` and `:feature:agenda` already read prices and navigate to `OfficialSiteRoute`, and reuse the same repositories (`PlaceRepository`, `EventRepository` in `:core:model`); do not add a new repository unless the data truly is not there (then put it under "Needs from planner"). Include empty, loading, error and offline states.

## Files you may touch
`:feature:tiket` only (code, strings id + en, drawables prefixed `tiket_`, previews, tests, its `build.gradle.kts` for dependencies already in the version catalog). One-line edit in `android/app/.../KelanaNavHost.kt` only if the `tiketScreen(...)` signature must change (e.g. to pass `navigate`).

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. ViewModel test covers: price with check date, price without check date ("sedang dicek"), empty list, error.

# Phase 7 · Agent D: match audit of merged screens (after A, B and C are merged)

Worktree `D:\PROJECT\BudayaGo\m\audit` (branch `match/phase7-audit`, from `main` after the Phase 7 merges). Read `docs/agents/phase7-common.md` and `docs/agents/phase3-match-common.md` first; both are binding.

## Why
Rencana (4.x), the route map (5.2) and Paspor (7.4) were merged without a separate check against the artifact. Make each screen match, the same way Phase 3 did for Jelajah and Tempat.

## Screens
| No. | Screen | Artifact file | Module |
|---|---|---|---|
| 4.1 | Rencana kosong | `RencanaKosongIlustratif.dc.html` | rencana |
| 4.2 to 4.7 | Susun 1/6 to 6/6 | `SusunTanggal`, `SusunRencanaIlustratif`, `SusunMinat`, `SusunWaktu`, `SusunKendaraan`, `SusunKebutuhan` | rencana |
| 4.8 | Menyusun | `Menyusun.dc.html` | rencana |
| 4.10 | Rencana hari ini | `RencanaHariIniIlustratif.dc.html` | rencana |
| 5.2 | Peta rute | `PetaRuteIlustratif.dc.html` | rencana |
| 7.4 | Paspor budaya | `PasporIlustratif.dc.html` | paspor |

All files are in `docs/design/artifact/`.

## Files you may touch
`:feature:rencana` and `:feature:paspor` only (UI, strings, drawables, previews, tests). Do not change ViewModel behavior, the plan builder, or anything in the stamp flow (location, queue, integrity). A real behavior gap goes under "Needs from planner".

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Per screen, the table of differences fixed and not fixed, as in `phase3-match-common.md`.

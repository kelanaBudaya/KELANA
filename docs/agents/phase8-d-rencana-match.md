# Phase 8 · Agent D: match audit of Rencana (4.x, 5.x)

Worktree `D:\PROJECT\BudayaGo\m\p8-rencana` (branch `match/rencana`). Read `docs/agents/phase8-common.md` and `docs/agents/phase3-match-common.md` first; both are binding. This is the Rencana half of the Phase 7 Agent D brief (`phase7-d-audit.md`), which was never run.

## Why
Rencana and its live re-plan screens were merged without an element-by-element check against the artifact. Make each screen match.

## Screens
| No. | Screen | Artifact file |
|---|---|---|
| 4.1 | Rencana kosong | `RencanaKosongIlustratif.dc.html` |
| 4.2 to 4.7 | Susun 1/6 to 6/6 | `SusunTanggal`, `SusunRencanaIlustratif`, `SusunMinat`, `SusunWaktu`, `SusunKendaraan`, `SusunKebutuhan` |
| 4.8 | Menyusun | `Menyusun.dc.html` |
| 4.10 | Rencana hari ini | `RencanaHariIniIlustratif.dc.html` |
| 3.3 | Atur pengingat | `Pengingat.dc.html` |
| 5.2 | Peta rute | `PetaRuteIlustratif.dc.html` |
| 5.3 | Tempat tutup mendadak | `TempatTutupIlustratif.dc.html` |
| 5.4 | Tanpa internet | `TanpaInternetIlustratif.dc.html` |

## Files you may touch
`:feature:rencana` only (UI, strings, drawables, previews, tests). Do not change ViewModel behaviour, `PlanBuilder`, or the repository. A real behaviour gap goes under "Needs from planner". Do not touch Pengingat scheduling (notifikasi owns the scheduler).

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Per screen, the table of differences fixed and not fixed, as in `phase3-match-common.md`.

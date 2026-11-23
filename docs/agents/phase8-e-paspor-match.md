# Phase 8 · Agent E: match audit of Paspor and Ambil cap (6.x, 7.4)

Worktree `D:\PROJECT\BudayaGo\m\p8-paspor` (branch `match/paspor`). Read `docs/agents/phase8-common.md` and `docs/agents/phase3-match-common.md` first; both are binding. This is the Paspor half of `phase7-d-audit.md`, extended to the Ambil cap flow.

## Screens
| No. | Screen | Artifact file |
|---|---|---|
| 6.1 | Izin lokasi | `IzinLokasiIlustratif.dc.html` |
| 6.2 | Ambil cap | `AmbilCapIlustratif.dc.html` |
| 6.3 | Cap berhasil | `CapBerhasilIlustratif.dc.html` |
| 6.4 | Gagal · di luar area | `DiLuarAreaIlustratif.dc.html` |
| 6.5 | Gagal · lokasi ditolak | `LokasiDitolakIlustratif.dc.html` (adapt "lewat browser" copy to Android app settings) |
| 7.4 | Paspor budaya | `PasporIlustratif.dc.html` |

The "menunggu sinyal" state is PROVISIONAL (not in the artifact): keep it consistent with 6.2 / 6.4, do not remove it.

## Files you may touch
`:feature:paspor` only (UI, strings, drawables, previews, tests). Do NOT change anything in the stamp flow's behaviour: location reading, the pending-stamp queue, its 30-minute expiry, Play Integrity, mock-location blocking, or ViewModel logic. A real behaviour gap goes under "Needs from planner".

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Per screen, the table of differences fixed and not fixed, as in `phase3-match-common.md`.

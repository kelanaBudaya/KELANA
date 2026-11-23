# Phase 7 · Agent C: Peta tempat (9.6)

Worktree `D:\PROJECT\BudayaGo\m\peta` (branch `feat/peta`). Read `docs/agents/phase7-common.md` first; it is binding.

## Screen
| No. | Screen | Artifact file |
|---|---|---|
| 9.6 | Peta tempat | `docs/design/artifact/KatPeta.dc.html` |
| ref | The planner's route map (same drawing style) | `docs/design/artifact/PetaRuteIlustratif.dc.html` |

## Decision already made: an illustrated map, not MapLibre
The design says "Peta ilustrasi, tidak sesuai skala." Build it as an illustration (vector drawable converted from the artifact's inline SVG) with tappable pins drawn in Compose. Do not add MapLibre, Google Maps, or any map SDK or network tiles. `:feature:rencana` has its own route illustration; features never import each other, so convert your own drawables (`peta_*`).

## Files you may touch outside `:feature:peta`
- `android/app/.../navigation/KelanaNavHost.kt`: only the `petaScreen(...)` call (add `navigate` if needed).
Nothing else. `PetaRoute` stays a data object. If a filter argument seems needed, put it under "Needs from planner" instead.

## Behavior
- Pins for every place from `PlaceRepository.places()`, never a hardcoded list. Position: project `Place.lat` / `lng` linearly into the illustration's bounds (one PROVISIONAL bounding box constant), clamp to the drawing, and nudge pins that overlap (document how). Pin labels as in the design.
- Filter chips: "Semua", "Buka hari ini" (from opening hours that carry a check date; a place whose hours are unchecked is not "open"), "Ada cap" (the place has a non-blank `capCode`; mark this interpretation PROVISIONAL). The "open today" logic exists privately in `:feature:jelajah` (`PlaceFilters.kt`) and `:feature:tempat` (`PlaceFacts.kt`); copy the small helper into your module with a note, do not import those features.
- Tapping a pin selects it (tap again to clear) and shows the bottom card: name, today's hours and price through `OfficialFact` (checked value with its date, or "Jam buka sedang dicek" / "Harga sedang dicek"), and at most one status badge backed by data: "Pentas malam ini" when `EventRepository.eventsOn(today)` has a show at that place, otherwise "Buka hari ini" when it is. Badges without data in this app ("Ada kelas membatik", "Bagian Sumbu Filosofi", "Macapat ...") are left out. "Lihat detail" opens `DetailTempatRoute(place.id)`.
- Hint "Ketuk titik untuk melihat info tempat." and footnote "Peta ilustrasi, tidak sesuai skala." as in the design. Back button. Guests can use the screen.
- Optional, only if simple: "Buka di aplikasi peta" for the selected place as a `geo:` intent with the place's own coordinates (no user location).
- Loading, empty (no places) and error states.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Tests cover: the projection (corners, clamping, overlap nudge), the filters (unchecked hours are not "open"), the selection toggle, and the badge choice. Report as in the common brief.

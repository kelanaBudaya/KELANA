# Agent C: `:feature:onboarding`

Common rules: `docs/agents/phase2-common.md`. Module: `android/feature/onboarding/` only.

## Screens and reference frames
| Screen | Frame PNG |
|---|---|
| Mulai (splash with "Ayo berkelana!" and "Sudah punya akun? Masuk") | `docs/design/frames/01.01-mulai.png` |
| Intro 1, 2, 3 (info resmi, jadwal pentas, cap di lokasi), "Lewati" on each, 3-step indicator | `01.02-kenalan-1.png`, `01.03-kenalan-2.png`, `01.04-kenalan-3.png` |
| Pilih daerah: map with region pins (Tersedia / Menyusul / Belum dibuka legend), tap a pin for a callout with "Berkelana di sini", list "Sudah bisa dijelajahi" with a "Berkelana di DI Yogyakarta" button, footer "2 wilayah menyusul dan 7 wilayah belum dibuka" | `01.05-pilih-daerah.png` (+ overview slide conv 2 for context) |

## Acceptance criteria
1. Flow: Mulai -> Intro 1 -> 2 -> 3 -> Pilih daerah -> Jelajah, using the route objects and callbacks the foundation registered. "Lewati" on an intro jumps to Pilih daerah. "Sudah punya akun? Masuk" goes to Masuk through the existing route. Picking a region marks onboarding complete (use the existing onboarding persistence from the foundation/AppViewModel; do not create a second flag) and goes to Jelajah. Back navigation behaves sensibly (no loops).
2. Pilih daerah loads regions from `RegionRepository`. Region status (available / upcoming / not open) drives pin color and the callout. Only an available region can be chosen ("Berkelana di sini"); the others show their status text and no choose button.
3. The map is an **illustration, not a live map** for now: a simple drawn Indonesia/archipelago backdrop (PROVISIONAL placeholder shapes are fine) with pins positioned from region data. Do not add MapLibre or any map SDK; MapLibre comes later. Pin tap shows the callout card. Legend as in the frame. "Lihat seluruh Indonesia" chip and zoom buttons may be visual-only; mark that.
4. The pager/indicator and button states match the frames. Text content exactly as in the frames.
5. ViewModel tests for: the intro page state, "Lewati", region selection (available vs not available), and completing onboarding.
6. Everything in `strings.xml` (id + en), previews for each screen, accessibility labels on pins.

## Out of scope
The Masuk screen, real map, location permission, anything in other modules.

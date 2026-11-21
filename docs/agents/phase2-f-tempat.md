# Agent F: `:feature:tempat`

Common rules: `docs/agents/phase2-common.md`. Module: `android/feature/tempat/` only.

## Screens and reference frames
| Screen | Frame PNG |
|---|---|
| Detail tempat (e.g. Museum Sonobudoyo): illustrated header with back and save buttons and "Lihat 4 foto resmi" chip, category + cap code chips, name, address, description, orange "Cap YK-02 menunggumu" banner, "Info resmi" card with "Diperiksa <tanggal>" badge (jam buka, tiket masuk, hari tutup, source note), visitor stories entry, report link | no PNG yet; overview slide conv 3 (left phone), mark PROVISIONAL |
| Foto resmi (official photos gallery) | no PNG yet; derive from detail + conv 3, mark PROVISIONAL, keep it simple |
| Laporkan: question "Apa yang perlu kami periksa?", 4 radio options (Jam buka atau harga berubah, Tempat tutup, Upacara adat tampil tanpa izin, Foto atau video tidak pantas), free-text "Ceritakan singkat", optional "Tambah foto papan info", submit button | no PNG yet; overview slide conv 3 (right phone), mark PROVISIONAL |

## Acceptance criteria
1. Detail loads one place and its facts from `PlaceRepository` by `placeId` (route argument). Loading, not-found and error states exist. Hours, ticket prices and closed days render through `OfficialFactRow`/`OfficialFact`: with `checkedAt` shows "Diperiksa <tanggal>", without shows "sedang dicek" (see AGENTS.md; never show an official fact without one of the two). Opening hours show today's row emphasized and the closed day.
2. The cap banner shows the place's cap code. "Ambil cap" is Phase 3: the button shows a "segera hadir" snackbar for now but sends a guest through the login hook first. Save (bookmark) button is visual-only, mark it.
3. Visitor stories section and photo/video grids are the Cerita feature (Phase 3): show only an entry row or a placeholder "Cerita pengunjung" with no data, marked PROVISIONAL.
4. Foto resmi opens from the header chip and shows the place's official photos (placeholders from the cover key for now).
5. Laporkan: radio selection required, text optional, photo picker button visual-only (mark). Submitting calls `ReportRepository.submit`: a guest gets the login hook (route to Masuk), a signed-in user gets success feedback and back navigation; failure shows an error. The submit button is disabled until a reason is chosen.
6. Back navigation and the system back work; the bookmark/back buttons have content descriptions.
7. ViewModel tests: detail (found, not found, checkedAt null vs set), report (reason required, guest -> needs login, success, failure).
8. Strings (id + en), previews of each state, accessibility.

## Out of scope
Map, stamping, stories feed, video, planner, notifications.

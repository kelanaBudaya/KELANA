# Agent D: `:feature:jelajah`

Common rules: `docs/agents/phase2-common.md`. Module: `android/feature/jelajah/` only.

## Screens and reference frames
| Screen | Frame PNG |
|---|---|
| Jelajah home: search bar with notification bell and avatar, region + date line, headline "Mau berkelana ke mana hari ini?", Paspor card with 4 quick actions (Ambil cap, Rencana, Agenda, Postcard), 8 category tiles (Museum, Candi, Tari, Topeng, Gamelan, Tiket resmi, Peta, Semua), "Malam ini di Prambanan" highlight banner, "Pilihan buat keluargamu" horizontal list with "Lihat semua", weekly challenge card ("Tantangan pekan ini") | `docs/design/frames/01.06-jelajah.png` |
| Cari (search) | `02.01-cari.png` |
| Kategori list (one generic screen for Museum, Candi, Tari, Gamelan: header illustration, category name, list of places with status chips) | no PNG yet; use overview slide conv 9 and conv 1, mark PROVISIONAL |
| Semua tempat (all places list with filters) | no PNG yet; overview slide conv 9, mark PROVISIONAL |

Bottom bar belongs to `:app`, not to you.

## Acceptance criteria
1. Home reads places and today's events from the repositories (fakes). The "Malam ini di ..." banner shows the next event of the day from `EventRepository`; if none, the banner is hidden. "Pilihan buat keluargamu" shows places from `PlaceRepository`. The Paspor card shows a static "x cap dari 10" using a placeholder count (real stamp data is Phase 3; mark it PROVISIONAL).
2. Category tiles navigate by route objects: Museum, Candi, Tari, Topeng, Gamelan -> `KategoriRoute(category)`; Semua -> `SemuaTempatRoute`; Peta and Tiket resmi go to their routes only if the foundation registered them, otherwise show a "segera hadir" snackbar. Quick actions: Agenda -> `AgendaRoute`; Ambil cap / Rencana / Postcard are Phase 3, show a "segera hadir" snackbar (PROVISIONAL) but route a guest through the login hook where the action will need an account.
3. Tapping a place goes to `DetailTempatRoute(placeId)`. Search bar opens `CariRoute`. Bell and avatar: avatar goes to Masuk for a guest (login hook) and is otherwise a no-op for now; bell is visual-only for now.
4. Cari: text search over places (`PlaceRepository.search`), results list, empty state "Tidak ada hasil" using `EmptyState`.
5. Kategori and Semua tempat: list with `PlaceCard`, category filtering, "Buka hari ini" style chip filter if the foundation data supports it, and the empty state (for example Topeng has no schedule yet: use the empty state with a "Kabari aku" button that goes through the login hook and does nothing else yet).
6. Place cards show the official-info badge "Diperiksa <tanggal>" where the data has one, via `OfficialFact` (rule in AGENTS.md).
7. ViewModel tests for home, search, category, all-places (success, empty, filter), with the fake repositories and Turbine.
8. Strings (id + en), previews, accessibility labels.

## Out of scope
Detail tempat (Agent F), Agenda (Agent E), map, notifications screen, stamps, planner.

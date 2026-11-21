# Agent E: `:feature:agenda`

Common rules: `docs/agents/phase2-common.md`. Module: `android/feature/agenda/` only.

## Screens and reference frames
| Screen | Frame PNG |
|---|---|
| Agenda budaya: title with region and month, horizontal date strip (selected date highlighted, dot under days with events), filter chips (Semua, Tari, Wayang, Ramah anak, ...), "Malam ini" highlight card with "Lihat detail", "Juga hari ini" list, next day sections ("Sabtu, 21 November"), each event row with place and "Diperiksa <tanggal>" badge | no PNG yet; overview slide conv 4 (left phone), mark PROVISIONAL |
| Detail pentas (e.g. Sendratari Roro Jonggrang): illustrated header, type chip, "Diperiksa" badge, title (Fraunces), place and duration, rows Jadwal / Harga / Penampil, source note, "Kisahnya" text, advice to arrive 30 to 45 minutes early | overview slide conv 4 (middle phone), mark PROVISIONAL |
| Ke situs tiket resmi: sheet "Kamu menuju situs tiket resmi", explains KELANA takes no payments, "Batal" and "Buka situs" | overview slide conv 4 (right phone), mark PROVISIONAL |

"Atur pengingat" (frame 03.03) is Phase 3 with notifications; do not build it.

## Acceptance criteria
1. Agenda reads from `EventRepository`: events for the selected date, plus the date strip showing a dot on dates that have events (use the range query). Default selected date is today; the sample data is around 2026-11-20 and later, so make "today" injectable (a `Clock` in the ViewModel) for tests and previews.
2. Filter chips filter by event type; "Semua" clears. Empty state for a date with no events using `EmptyState`.
3. The "Malam ini" card shows the first evening event of the selected date and opens the detail.
4. Detail pentas loads by `eventId`: schedule is rendered from the recurrence data (for example "Setiap Jumat, 19.30 sampai 20.30"), price from ticket data, source note with `sourceUrl` host and "dibuka <tanggal>". Every official fact goes through `OfficialFact` (Diperiksa date or "sedang dicek"). The arrival advice appears when the data has it.
5. "Beli tiket" / ticket button opens the "Ke situs tiket resmi" sheet. "Buka situs" opens the official URL in a **Custom Tab** (`androidx.browser`; if the dependency is not available in your module and you cannot add it, list it under "Needs from planner" and use an `ACTION_VIEW` intent meanwhile). The text states that KELANA accepts no payments. **No payment SDK, no checkout, ever.** Only open `https` URLs from the data.
6. ViewModel tests with a fixed `Clock`: date selection, filter, empty date, detail found / not found, `checkedAt` null.
7. Strings (id + en), previews, accessibility.

## Out of scope
Reminders and notifications, planner, map, stamps.

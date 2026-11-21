# Phase 2.0 foundation (one agent, runs before the four feature agents)

Goal: put every shared thing the Phase 2 feature agents need in place, once, so they can later work only inside their own `:feature:*` module without touching shared files or each other.

Read first: `AGENTS.md` (binding rules, including module and navigation rules), `docs/design/FIGMA-FRAMES.md`, `supabase/seed.sql` (the sample data), `supabase/contract/kelana.openapi.yaml` and `supabase/contract/types.ts` (the data contract), and the existing code under `android/` (skeleton, design system, `SessionRepository`, Masuk flow). Match existing conventions.

Scope: only `android/`. Do not commit. Build env: same as before (`bash /d/tmp-kelana/run.sh <tasks>` from `D:\PROJECT\BudayaGo\KELANA`, keep every cache on D:, use the `D:\PROJECT\BudayaGo\KELANA` path casing).

## Build

1. **Domain models** in `:core:model` (pure Kotlin, no Android), mirroring the contract and the sample data:
   `PlaceCategory` (museum, candi, tari, topeng, gamelan, kerajinan, lainnya), `Place` (id, slug, name, category, capCode, lat, lng, address, description, coverKey), `OpeningHours`, `Closure`, `TicketPrice`, `Event` (recurrence + next occurrences), `Region` (exists, extend if needed), `ReportReason`.
   Enforce the product rule in the type system: every official fact is wrapped in `OfficialFact<T>` carrying `sourceUrl` and `checkedAt: LocalDate?`; a fact with `checkedAt == null` is representable and must render as "sedang dicek" (see AGENTS.md). No official fact reaches a screen without this wrapper.
2. **Repository interfaces** in `:core:model` (or the place you already put `SessionRepository`): `PlaceRepository` (all places, by id, by category, search by text), `EventRepository` (events for a date and for a date range, by id), `RegionRepository`, `ReportRepository` (submit a report; returns Success | NeedsLogin | Failed). Use `Flow` where data can change, suspend for one-shots.
3. **Fake implementations** in `:core:data`, seeded from `supabase/seed.sql` (hand-translate the 10 places, hours, tickets, closures, 5 sample events into Kotlin; keep `checkedAt` 2026-11-20 and mark the data as sample in a comment). Bind them in ONE Hilt module (`RepositoryModule`) so swapping to the Supabase-backed implementations later is a one-file change. `ReportRepository` fake returns `NeedsLogin` for a guest. No network calls yet.
4. **Routes** in `:core:navigation` (serializable route objects), for every Phase 2 screen:
   - onboarding: `IntroRoute(page: Int)` (3 pages), `PilihDaerahRoute`
   - jelajah: `JelajahRoute` (home), `CariRoute`, `KategoriRoute(category)`, `SemuaTempatRoute`
   - tempat: `DetailTempatRoute(placeId)`, `FotoResmiRoute(placeId)`, `LaporkanRoute(placeId)`
   - agenda: `AgendaRoute`, `DetailPentasRoute(eventId)`, plus an `OfficialSiteRoute(url, label)` confirm sheet or an external-link helper (frame 03.04: a sheet saying the user goes to the official ticket site; "Buka situs" opens a Custom Tab).
   Keep the existing routes (Masuk, tabs).
5. **Wiring in `:app`**: register every route in the `NavHost`. For each feature module, add the NavGraph extension and a stub screen composable with a stable public signature, for example `fun NavGraphBuilder.jelajahScreens(navigate: (Any) -> Unit)` where `navigate` takes a route object from `:core:navigation`, and screen composables take what they need (a ViewModel from Hilt, plus callbacks). The stubs show a placeholder `Text` from `strings.xml`. After this task, a feature agent edits only inside its own module and never `:app`, `:core:navigation` or other shared modules. Make the tab "Jelajah" start at `JelajahRoute`, and make the first-launch flow go Intro(0) -> Intro(1) -> Intro(2) -> PilihDaerah -> Jelajah (replace the old onboarding placeholder with these stubs; keep "Sudah punya akun? Masuk" and the guest rule).
6. **Shared UI** (small, reusable, each with a `@Preview`) in `:core:designsystem` or `:core:ui`: `KelanaBackTopBar`, `PlaceCard` (cover slot, title, subtitle; two sizes: grid and list), `EventRow`, `InfoBanner` (tones: info, success, warning), `EmptyState` (illustration slot, title, body, optional action), `OfficialFactRow` (label, value, with the "Diperiksa <tanggal>" `VerifiedBadge` or the "sedang dicek" state). Estimate sizes from the reference frames under `docs/design/frames/` if present, mark unknown values PROVISIONAL.
7. **Tests**: unit tests for the fake repositories (10 places, categories, search, events by date, the `checkedAt == null` case) and for `OfficialFact` rendering logic. Existing tests keep passing. All strings in `strings.xml` (id + en).
8. Update `android/README.md`: the module map, the screen-to-module table, the rule "feature agents touch only their module", and how to swap fake for real repositories.

## Do not
- Add Supabase/PostgREST calls, Room tables, Maps, or real screens (feature agents do those).
- Change the login/session code except where routing needs it.
- Let any feature module depend on another feature.

## Done means
`./gradlew assembleDebug lintDebug testDebugUnitTest` passes. Report honestly: commands and results, files and modules touched, anything PROVISIONAL, anything unverified. Do not claim a visual match you did not see.

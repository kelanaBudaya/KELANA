# KELANA Android

Kotlin, Jetpack Compose, Material 3, Hilt (KSP), Room, Retrofit. JDK 17, minSdk 26, applicationId `id.kelana.app`.

## Build

```
cd android
# local.properties must contain sdk.dir=<your Android SDK> (git-ignored)
./gradlew assembleDebug lintDebug testDebugUnitTest
```

The API base URL is a placeholder (`KELANA_API_BASE_URL` in `local.properties` or `-P`). Never commit secrets.

## Modules

| Module | Purpose |
|---|---|
| `:app` | Application, `MainActivity`, `NavHost` (registers every route), bottom bar (Jelajah, Agenda, Rencana, Cerita) |
| `:core:designsystem` | Theme, tokens, fonts, `KelanaButton/Card/Chip/TopBar/BackTopBar`, `VerifiedBadge`, `PlaceCard`, `EventRow`, `InfoBanner`, `EmptyState`, `OfficialFactRow` |
| `:core:ui` | `PlaceholderScreen`, `OfficialFactRow(label, OfficialFact, format)`, `OfficialSiteSheet` + `openCustomTab`, category labels, `formatRupiah`/`formatTimeRange`, `rememberRequireLogin` |
| `:core:model` | Pure Kotlin: `Place`, `Event`, `Region`, `OfficialFact<T>`, repository interfaces |
| `:core:navigation` | Type-safe routes for every screen |
| `:core:data` | Repository implementations: fakes seeded from `supabase/seed.sql` (`RepositoryModule`), session, onboarding flag |
| `:core:network` | OkHttp, Retrofit, kotlinx.serialization |
| `:core:database` | Room, DataStore |
| `:feature:*` | onboarding, akun, jelajah, tempat, cerita, agenda, rencana, peta, paspor, postcard, notifikasi, tiket |

## Screen to module

| Screen (route) | Module | Entry point called by `:app` |
|---|---|---|
| Intro 1 to 3 (`IntroRoute(page)`), Pilih daerah (`PilihDaerahRoute`) | `:feature:onboarding` | `onboardingScreens(navigate, onFinished)` |
| Masuk (`MasukRoute`), Akun (`AkunRoute`) | `:feature:akun` | `akunScreen(onSignedIn, onGuest)` |
| Jelajah (`JelajahRoute`), Cari (`CariRoute`), Kategori (`KategoriRoute(category)`), Semua tempat (`SemuaTempatRoute`) | `:feature:jelajah` | `jelajahScreens(navigate, onBack)` |
| Detail tempat (`DetailTempatRoute(placeId)`), Foto resmi (`FotoResmiRoute`), Laporkan (`LaporkanRoute`) | `:feature:tempat` | `tempatScreens(navigate, onBack)` |
| Agenda (`AgendaRoute`), Detail pentas (`DetailPentasRoute(eventId)`) | `:feature:agenda` | `agendaScreens(navigate, onBack)` |
| Ke situs tiket resmi (`OfficialSiteRoute(url, label)`) | `:app` dialog, content in `:core:ui` | features just `navigate(OfficialSiteRoute(...))` |
| Rencana, Cerita, Peta, Paspor, Postcard, Notifikasi, Tiket | their own `:feature:*` | still placeholders |

`navigate` takes a route object from `:core:navigation`; `onBack` pops the back stack. These signatures are stable.

## Rule for feature agents: touch only your module

A feature agent edits only inside its own `:feature:<name>` module (code, `strings.xml` id + en, tests). It does not edit `:app`,
`:core:*` or another feature. If a screen needs something shared (a model field, a route, a repository method, a shared composable),
ask the planner to add it to `:core:*`; do not work around it. Replace the stub composables in your module; keep the
`NavGraphBuilder` extension signature. Show official facts only through `OfficialFact` (a fact without `checkedAt` renders as "sedang dicek").

## Swapping fake repositories for the real ones

Every content repository (`PlaceRepository`, `EventRepository`, `RegionRepository`, `ReportRepository`) is bound in one file:
`core/data/src/main/kotlin/id/kelana/core/data/di/RepositoryModule.kt`. Today each `@Binds` points at a `Fake*Repository` in
`core/data/.../repository/FakeRepositories.kt` (sample data in `core/data/.../sample/SampleData.kt`, hand-translated from `supabase/seed.sql`).
To go live: write `Supabase*Repository` classes implementing the same interfaces in `:core:data`, then change the parameter type of
each `@Binds` function in `RepositoryModule`. No feature code changes.

Convention plugins live in `build-logic`: `kelana.android.application`, `.library`, `.compose`, `.hilt`, `.feature`.

## Dependency rules (enforced in the root `build.gradle.kts`, the build fails on a violation)

- `:feature:*` depends on `:core:*` only. Features never depend on each other.
- `:core:*` never depends on a feature or on `:app`.
- Only `:app` depends on features. Features navigate by route objects from `:core:navigation`.

All UI text goes in `strings.xml` (Indonesian in `values/`, English in `values-en/`).

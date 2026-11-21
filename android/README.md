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
| `:app` | Application, `MainActivity`, `NavHost`, bottom bar (Jelajah, Agenda, Rencana, Cerita) |
| `:core:designsystem` | Theme, tokens, Inter font, `KelanaButton/Card/Chip/TopBar`, `VerifiedBadge` |
| `:core:ui` | Shared composables (`PlaceholderScreen`) |
| `:core:model` | Pure Kotlin models |
| `:core:navigation` | Type-safe routes |
| `:core:data` | Repository implementations (empty) |
| `:core:network` | OkHttp, Retrofit, kotlinx.serialization |
| `:core:database` | Room, DataStore |
| `:feature:*` | onboarding, akun, jelajah, tempat, cerita, agenda, rencana, peta, paspor, postcard, notifikasi, tiket |

Convention plugins live in `build-logic`: `kelana.android.application`, `.library`, `.compose`, `.hilt`, `.feature`.

## Dependency rules (enforced in the root `build.gradle.kts`, the build fails on a violation)

- `:feature:*` depends on `:core:*` only. Features never depend on each other.
- `:core:*` never depends on a feature or on `:app`.
- Only `:app` depends on features. Features navigate by route objects from `:core:navigation`.

All UI text goes in `strings.xml` (Indonesian in `values/`, English in `values-en/`).

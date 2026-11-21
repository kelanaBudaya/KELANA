# Agent A: Android skeleton

You are building the Android project skeleton for KELANA. Every feature agent later depends on your output, so correctness and a green build matter more than breadth.

Read first, in order: `AGENTS.md` (repo root, binding rules), `docs/design/FIGMA-FRAMES.md` (tokens, tab order, frame map). You cannot call Figma. Use only the tokens in that file.

## Scope: only these paths

You may create and edit files only under `android/` and `.github/workflows/`. Do not touch `AGENTS.md`, `docs/`, `supabase/`, `admin/`, or the old `assets/` and `index.html` files. Do not run `git commit` or `git push`.

## Environment (Windows)

- JDK 17: look for `C:\Program Files\Eclipse Adoptium\jdk-17*`. Set `JAVA_HOME` to it for your commands.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (platform `android-36.1`, build-tools 36.1.0 and 37.0.0). Write `android/local.properties` with `sdk.dir=...` (must be git-ignored).
- If `JAVA_HOME` or the SDK is missing, stop and report. Do not install anything system-wide.

## Build this

1. **Project**: Kotlin + Jetpack Compose + Material 3, Gradle Kotlin DSL, `gradle/libs.versions.toml` version catalog, Gradle wrapper (generate it; if no `gradle` binary exists, report that instead of faking it), `minSdk 26`, `compileSdk 36`, `targetSdk 36`, JDK 17 toolchain, applicationId and namespace root `id.kelana.app`. Pick current stable versions of AGP, Kotlin, Compose BOM, Hilt, etc. that you have verified resolve and build together. Use KSP, not kapt.
2. **Convention plugins** in `android/build-logic` (`kelana.android.library`, `kelana.android.compose`, `kelana.android.feature`, `kelana.android.hilt`) so module build files stay tiny. Avoid copy-pasted config.
3. **Modules** (register all in `settings.gradle.kts`):
   - `:app` (Application `KelanaApp` with `@HiltAndroidApp`, single `MainActivity` with `@AndroidEntryPoint`, `NavHost`, bottom bar)
   - `:core:designsystem`, `:core:ui`, `:core:model`, `:core:navigation`, `:core:data`, `:core:network`, `:core:database`
   - Empty feature modules: `:feature:onboarding`, `:feature:akun`, `:feature:jelajah`, `:feature:tempat`, `:feature:cerita`, `:feature:agenda`, `:feature:rencana`, `:feature:peta`, `:feature:paspor`, `:feature:postcard`, `:feature:notifikasi`, `:feature:tiket`. Each has only a placeholder screen composable (see 5) and the feature convention plugin. No business logic.
   - Dependency rules (enforce them in Gradle): `:feature:*` may depend on `:core:*` only. No feature depends on another feature. `:core:*` never depends on a feature. Only `:app` depends on features.
4. **Core wiring (minimal, compiles, no real data yet)**:
   - `:core:network`: Hilt module providing `OkHttpClient`, `Retrofit` with kotlinx.serialization converter, base URL from `BuildConfig` (placeholder value, read from `local.properties`/Gradle property, never hardcoded secrets).
   - `:core:database`: Room database with DataStore dependency available. Room needs at least one entity: add one clearly named placeholder entity and mark it `// PLACEHOLDER until the data contract lands`.
   - `:core:data`: empty repository package, Hilt module stub.
   - `:core:model`: pure Kotlin/Android-free models stub (one `Region` class is enough).
5. **Navigation shell**: type-safe routes (kotlinx.serialization) in `:core:navigation`. Bottom bar with exactly four tabs in this order: **Jelajah, Agenda, Rencana, Cerita**, 84 dp tall, active label bold `navy`, inactive regular `muted`, per `FIGMA-FRAMES.md`. Each tab opens its feature's placeholder screen (a centered `Text` from `strings.xml`). `:app` registers every destination; features expose only a `NavGraphBuilder` extension or screen composable, and navigate by route objects only. Non-tab destinations (onboarding, akun) get routes and placeholders but no bar.
6. **Design system** in `:core:designsystem` using Material 3 theme wrappers:
   - `KelanaColors` (confirmed tokens from the file; PROVISIONAL ones marked with a `// PROVISIONAL: confirm against Figma` comment), `KelanaTypography` (Inter Regular 400 and Bold 700; bundle the OFL Inter font files in `res/font`, or if you cannot download them, fall back to `FontFamily.SansSerif` and leave a `TODO`), spacing and shape tokens (16 dp gutter, card radius from your best estimate marked PROVISIONAL).
   - Reusable components, each with a `@Preview`: `KelanaButton` (primary, secondary, outline, text variants + disabled), `KelanaCard`, `KelanaChip` (selected and unselected), `KelanaTopBar`, and `VerifiedBadge` showing "Diperiksa 20 Nov 2026" style text from a `LocalDate` formatted for `id-ID`.
7. **Strings**: every user-facing string in `strings.xml`. Indonesian in `values/`, English in `values-en/`, both complete. Zero hardcoded UI text in Kotlin.
8. **Tests**: at least one real unit test (for example the `VerifiedBadge` date formatting and the route serialization), runnable with `./gradlew test`. No fake or empty tests.
9. **CI**: `.github/workflows/android.yml` triggered on `pull_request`, JDK 17, `gradle/actions/setup-gradle`, working directory `android`, running `./gradlew assembleDebug lintDebug testDebugUnitTest`. Cache Gradle. Do not add release or signing steps.
10. `android/.gitignore` (build outputs, `local.properties`, `.gradle`, IDE files, keystores) and a short `android/README.md` (how to build, module map, the dependency rules).

## Do not

- Add the Google Maps SDK (the map is MapLibre later), any payment SDK, or any location code.
- Add dependencies you do not use. No unrelated libraries "for later" except the ones named above.
- Invent colors, sizes or copy that the design file does not support without marking them PROVISIONAL.

## Done means (run and report each, with the real result)

```
cd android
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
```

All three must pass. In your final report list: the exact commands you ran and whether each passed, the module list, the versions you chose, anything PROVISIONAL, and anything you could not do or verify. If a command failed and you could not fix it, say so plainly. Do not claim a pass you did not see.

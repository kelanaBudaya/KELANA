# Phase 2 feature agents: common rules

Applies to Agent C (onboarding), D (jelajah), F (tempat), E (agenda). Your own brief lists your module, screens and acceptance criteria.

## Read first
`AGENTS.md` (binding), `docs/design/FIGMA-FRAMES.md` (tokens, frame map), `docs/design/frames/README.md` (which frame PNGs exist and the fallback), `android/README.md` (module map, the rule "feature agents touch only their module"), and the code of the foundation: models and repository interfaces in `:core:model`, fakes in `:core:data`, routes in `:core:navigation`, shared components in `:core:designsystem` / `:core:ui`, and your module's stub screen already registered in `:app`.

## Where you work
- You have your own git worktree on your own branch. Work only there. Never touch the main checkout at `D:\PROJECT\BudayaGo\KELANA`.
- Edit **only your own `:feature:<name>` module** (`android/feature/<name>/`). Do not edit `:app`, `:core:*`, `gradle/libs.versions.toml`, CI, `supabase/` or other features. If you truly need a change there (a new route, a shared component, a dependency), stop and list it in your final report under "Needs from planner" and work around it inside your module in the meantime (for example a private composable). Duplicating a tiny helper inside your module is better than editing a shared file.
- Do not commit or push. The planner reviews your diff and commits.
- No network calls: use the repository interfaces from `:core:model`; the Hilt bindings give you the fake implementations. Never hardcode data in screens.

## Build and verify (RAM is limited, drive C: is nearly full)
Run Gradle only through the locked runner, from anywhere:
`bash /d/tmp-kelana/gradle.sh <your worktree root> <tasks>` for example
`bash /d/tmp-kelana/gradle.sh /d/PROJECT/BudayaGo/wt/jelajah :feature:jelajah:testDebugUnitTest`
It queues behind other agents' builds, so builds can wait several minutes. Prefer module-scoped tasks while iterating and run the full `assembleDebug lintDebug testDebugUnitTest` once at the end. Use the exact path casing of your worktree.

## Quality bar (every screen)
- Matches the referenced frame PNG: layout, copy (Indonesian text exactly as in the frame), colors from `FIGMA-FRAMES.md`, typography (Inter UI, Fraunces headlines). All states the design shows (empty, loading, error where drawn).
- All user-facing strings in `strings.xml` (`values/` Indonesian, `values-en/` English). No hardcoded text.
- Official facts always rendered through the `OfficialFact` wrapper with its "Diperiksa <tanggal>" badge or the "sedang dicek" state. Guests browse freely; actions that need an account go through the login hook (`rememberRequireLogin`), never by importing `:feature:akun`.
- Every ViewModel has a unit test (JUnit + Turbine) using the fake repositories. UI state is a single immutable class exposed as `StateFlow`.
- Accessibility: content descriptions on icons and images, touch targets at least 48 dp, text scalable (sp).
- `@Preview` for each screen state. Mark every size or layout value you estimated PROVISIONAL.
- Illustrations: the design uses illustrated covers and icons. Use the SVG-derived vector drawables that exist in `:core:designsystem`; if a needed illustration is missing, use a clearly marked placeholder shape, never a random stock image.

## Final report (required format)
1. Commands run and their real results (paste the BUILD line and test counts).
2. What you built per screen, and per acceptance criterion: done / partly / not done.
3. For each screen: which frame PNG you compared against, and exactly how you compared it. You cannot run an emulator on this machine, so say plainly that you compared code with the PNG and did not see a render, unless you actually rendered something.
4. PROVISIONAL items, "Needs from planner", anything unverified.
Only claim what you saw.

# Phase 4: email + password screens (provisional design)

Worktree `D:\PROJECT\BudayaGo\m\email` (branch `feat/email-auth`) after Agent I has added `EmailAuthRepository` (signUp, signIn only) and the typed failures in `:core:model`/`:core:data`. Do not commit or push. Run Gradle only via `bash /d/tmp-kelana/gradle.sh /d/PROJECT/BudayaGo/m/email <tasks>`.

Read: `AGENTS.md` (auth rule, strings, tokens), `docs/design/email-auth-brief.md` (screens, states, copy rules), `docs/agents/phase2-common.md`, `docs/design/artifact/MasukIlustratif.dc.html` (visual language), the existing `feature/akun` (MasukScreen, MasukViewModel).

## Scope (decided by the owner)
Only **Masuk dengan email**, **Daftar**, and the updated **Masuk**. NO code verification screens, NO forgot password, NO success screen: sign-up creates an active account and signs in immediately.

## Important
There is **no artifact design** for these screens. The owner will design them in Claude Design later and we will then match. Build from existing tokens and components (`KelanaTheme`, `KelanaTypography`, `KelanaShapes`, `KelanaDimens`, `KelanaButton`, existing input components, the Masuk layout). One composable per screen, no hardcoded colors or sizes. Mark the design PROVISIONAL in the report; do not claim a visual match.

## Do
1. `:feature:akun`: add `MasukEmailScreen` and `DaftarScreen` with one ViewModel each (+ Turbine unit tests), and update `MasukScreen`: keep the Google button, add an "atau" divider, an email option, and links to Daftar and "Jelajah dulu tanpa akun".
2. Routes in `:core:navigation` (MasukEmailRoute, DaftarRoute), registered in `:app` `KelanaNavHost`. Features never import each other. After successful sign-in or sign-up, leave the flow exactly as Google sign-in does today (reuse the logic in MasukViewModel/RequireLogin, including returning to where the guest came from).
3. Behavior: field validation with the shared validators; password show/hide; loading and disabled button states; every typed failure from `EmailAuthRepository` mapped to a string (id + en); wrong credentials must not reveal whether the email exists. If sign-up returns the "no session" failure, show a clear message instead of a fake success.
4. Strings: Indonesian in `values/`, English in `values-en/`, nothing hardcoded, no unused strings.
5. Previews for each screen and state (error, loading). Touch targets >= 48 dp, content descriptions on icons.
6. Anything needed outside `:feature:akun`, `:core:navigation` and `:app` navigation goes under "Needs from planner", not edited.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Report real results, files and strings added, what is provisional, and what is unverified (not rendered or run on a device; no real sign-up against Supabase).

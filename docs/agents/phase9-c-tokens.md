# Phase 9 · Agent C: design tokens, no ad-hoc colours in features

Worktree `D:\PROJECT\BudayaGo\m\p9-tokens` (branch `chore/design-tokens`). Read `docs/agents/phase9-common.md` first.

## Why
AGENTS.md: "use design tokens from `:core:designsystem`, no ad-hoc colors, sizes or fonts in feature code". Feature code still has private `Color(0x...)` constants and Phase 8 agents asked for tokens: `#9A3412`, `#B5BBC6`, `#B9C4DA`, `#C9BFA8`, the Hapus akun reds `#B84439` / `#FBE7E5` / `#E5B9B4`, plus others found by `grep -rn "Color(0x" android/feature`.

## What to do
- Add semantic tokens to `KelanaColors` (name by role, e.g. `dangerStrong`, `dangerSurface`, `dangerDisabled`, `warningText`, `routeSpine`, `dashMuted`, `dashWarm`, `galleryPanel` ... pick clear names; reuse an existing token when the value is equal). Document each with the artifact screen it comes from. Same for any ad-hoc `.dp` radius the agents marked "PROVISIONAL: no token" if a matching radius token is missing (add to the shapes/radius tokens).
- Replace every `Color(0x...)` in `android/feature/**/src/main` with tokens. Colours that are part of an illustration drawn in code (e.g. `PostcardCardDrawing.kt`, map art) may stay if they are art, not UI: then move them into a clearly named `internal object ...Art` palette in that file and say which ones in the report.
- Add a unit test in `:core:designsystem` that pins the new token values.
- Visual result must not change: each replacement uses the same hex value.

## Files you may touch
`:core:designsystem`, and in feature modules ONLY the lines that declare or use ad-hoc colours/radii (plus imports). Do not change layout, copy, behaviour or anything else; other agents edit those modules at the same time.

## Done means
Full Gradle check passes. `grep -rn "Color(0x" android/feature/*/src/main` returns only the documented art palettes. Report a table: old constant → token, file.

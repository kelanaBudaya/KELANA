# Phase 8 · Agent C: Hapus akun (account deletion)

Worktree `D:\PROJECT\BudayaGo\m\p8-akun` (branch `feat/hapus-akun`). Read `docs/agents/phase8-common.md` first; it is binding.

## Screen
| Screen | Artifact file | Module |
|---|---|---|
| Hapus akun · konfirmasi (artifact page "awal", allowed: it only exists there) | `HapusAkun.dc.html` | `:feature:akun` + `supabase/` |

Profil (8.2) already shows a destructive "Hapus akun" row (`ProfilScreen.kt`, `R.string.profil_row_hapus`) with no action. Wire it to a new confirmation screen.

## Behaviour (security sensitive: read carefully)
- The screen lists what is deleted and what is kept. Counts (cap, postcard, video) come from real data for the signed-in user; if a count is not available, drop the number rather than invent one. "Ketik HAPUS untuk melanjutkan": the destructive button is enabled only when the field equals `HAPUS` (trim, case-sensitive per the artifact; decide and document). "Batal" goes back.
- "Unduh dataku dulu": if a proper export is more than a small job, keep the button but mark it PROVISIONAL and put the export under "Needs from planner"; do not ship a half export.
- Deletion is done on the server, never by the client deleting rows. Add a Supabase Edge Function `supabase/functions/delete-account/` (Deno, follow `verify-stamp/` for structure, auth and error shape): verify the caller's JWT, then delete that user only (`auth.admin.deleteUser` with the service role, which the function reads from env, never from the client). Data rules from the artifact: stamps, postcards, videos (including public ones), plans and saved needs are deleted; reports the user sent are kept with the user reference set to NULL. Check the existing migrations' foreign keys: add a NEW migration only for what cascade / set-null does not already do, and delete the user's Storage objects (postcard photos). Add tests in `supabase/tests/` in the same style as the existing ones.
- After success: sign out locally (clear the supabase-kt session and any per-user local cache the app keeps: Room / DataStore tables that hold passport, plan, pending stamps), then go to Masuk or Jelajah as a guest. On failure: stay, show the error, nothing is cleared.
- The Android side calls the function through the existing supabase-kt `Functions` client; put the call behind a repository interface (NEW file in `:core:model`), real implementation in a NEW file in `:core:data`, NEW Hilt module file.

## Files you may touch
`:feature:akun`; NEW files in `:core:model`, `:core:data`, `:core:navigation` (route); one-line registration in `android/app/.../KelanaNavHost.kt`; `supabase/functions/delete-account/**`, a NEW migration, NEW or extended tests in `supabase/tests/`, `supabase/contract/` only if it lists functions. If clearing local caches needs a method on an existing DAO or store, add it with the smallest change and list it.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. ViewModel test covers: button disabled until `HAPUS`, success clears session and navigates, failure keeps everything. Report whether the Supabase tests and the function were actually run (e.g. `supabase test db`, `deno test`/`deno check`) or only written; do not claim a run you did not do.

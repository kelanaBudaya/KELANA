# Phase 9 · Agent E: Compose UI tests, account flows

Worktree `D:\PROJECT\BudayaGo\m\p9-uiaccount` (branch `test/ui-account`). Read `docs/agents/phase9-common.md` first.

## Why
Same as Agent D (`phase9-d-uitest-browse.md`): Compose UI tests the planner runs on the emulator during UAT. You cover the flows that need an account.

## What to write
Compose UI tests in `src/androidTest/kotlin/...`, testing stateless content composables with fixed UI states, in `KelanaTheme`, no Hilt, no network, strings read from resources.

| Module | Screens and what to assert |
|---|---|
| `:feature:akun` | Masuk: Google and email options, "Jelajah dulu tanpa akun"; email sign-up: password rule (min 8, letters and digits) blocks submit; Profil rows; Hapus akun: button disabled until exactly `HAPUS`, enabled after, "Batal" calls back |
| `:feature:paspor` | Ambil cap states 6.1 to 6.5 and "menunggu sinyal": each shows its title and its primary action; Cap berhasil; Paspor grid shows earned vs empty stamps |
| `:feature:postcard` | Tulis postcard 7.1: publish disabled until required fields; "Video" button calls back; Bagikan 7.2 share targets; Unggah video 7.3: upload disabled until a video and all three checks; too-long state; progress; "Ambil cap dulu" state |
| `:feature:cerita` | Feed: guest sees login guard (stories not readable by guests); empty state; filter chips |
| `:feature:notifikasi` | Izin notifikasi 4.9 actions; Notifikasi 5.1 list and empty state; Pengaturan toggles |

If a content composable is private, make the smallest visibility change (`private` → `internal`) and list it; do not refactor. Do not touch `HapusAkunViewModel.kt` or anything outside `src/androidTest` except those visibility changes (Agents B and C edit the same modules).

## Files you may touch
`src/androidTest/**` of the modules above; visibility-only changes in their `src/main`.

## Done means
`assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest` pass. List every test with what it asserts.

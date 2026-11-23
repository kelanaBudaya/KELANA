# Phase 7 · Agent B: Notifikasi (4.9, 5.1, Pengaturan notifikasi, local reminders)

Worktree `D:\PROJECT\BudayaGo\m\notifikasi` (branch `feat/notifikasi`). Read `docs/agents/phase7-common.md` first; it is binding.

## Screens
| No. | Screen | Artifact file |
|---|---|---|
| 4.9 | Izin notifikasi | `docs/design/artifact/IzinNotifIlustratif.dc.html` |
| 5.1 | Notifikasi (pusat notifikasi) | `docs/design/artifact/NotifikasiIlustratif.dc.html` |
| (awal) | Pengaturan notifikasi | `docs/design/artifact/PengaturanNotif.dc.html` |
| ref | How a system notification reads | `docs/design/artifact/LayarKunci.dc.html` |

## Scope: local notifications only
No FCM, no Firebase, no server push in this phase. Everything is produced on the device.

## Files you may touch outside `:feature:notifikasi`
- NEW `android/core/navigation/.../NotifikasiRoutes.kt` (`IzinNotifRoute`, `PengaturanNotifRoute`, anything else you need).
- `android/app/.../navigation/KelanaNavHost.kt`: only the `notifikasiScreen(...)` call.
- `android/feature/jelajah/.../JelajahScreen.kt`: the bell's `onBell` navigates to `NotifikasiRoute` (one line), plus the existing comment in `HomeContent.kt` that says the bell is visual-only.
- `android/feature/akun/.../ProfilScreen.kt` (and `AkunScreen.kt` if a callback is needed): make the "Notifikasi" row open `PengaturanNotifRoute`. Leave the other inert rows as they are.
- `android/feature/rencana/.../PengingatViewModel.kt` (+ its test, + `PengingatScreen.kt` only if needed for the permission step): after saving the choice, call `ReminderScheduler.scheduleShowReminder`. When `addToCalendar` is on, open the calendar insert intent (`Intent.ACTION_INSERT`, `CalendarContract.Events`, no calendar permission).
- `android/core/data/.../stamp/StampSender.kt` (+ its tests): when a send ends in `StampResult.Stamped`, call `NotificationRepository.post(NotificationEvent.StampConfirmed(placeId))`. Change nothing else in the stamp flow; the location and privacy rules there are strict. Put a `FakeNotificationRepository` (not bound in Hilt) under `:core:data` test sources or as a new main file for other modules' tests.

## Build
- `NotificationRepository` implementation inside your module, persisted across restarts (DataStore with kotlinx.serialization JSON is enough; keep at most 100 entries). Bind it with a Hilt module in your module (precedent: `PasporModule`). It resolves each `NotificationEvent` into title and body from your strings (place and event names from `PlaceRepository` / `EventRepository`), stores the entry, and shows a system notification only when permission is granted, the category's setting is on, and it is not inside quiet hours. When the app is in the foreground, store the entry but skip the system notification.
- Notification channels: Rencana, Paspor, Info. `POST_NOTIFICATIONS` declared in your module's manifest.
- `ReminderScheduler` implementation with WorkManager: one unique work per event (`REPLACE`), delayed until the show start minus the chosen offset (one hour, 30 minutes, or "waktunya berangkat"). "Waktunya berangkat" uses a fixed PROVISIONAL lead time in one named constant; it never uses location or live travel time. The worker posts `NotificationEvent.ShowReminderDue`. Use a Hilt entry point inside the worker, like `StampSendWorker`, so `:app` needs no `Configuration.Provider`. A reminder in the past is not scheduled.
- 4.9 Izin notifikasi: shown before the first reminder is saved when the permission is missing on API 33+. "Ingatkan aku" requests the system permission; "Nanti saja" closes and the reminder is still stored (the center keeps it even without system notifications). Below API 33 the screen is skipped.
- 5.1 Notifikasi: chips Semua / Rencana / Paspor / Cerita, grouped "HARI INI", "KEMARIN", then dates in id-ID. The design's "[ISI] jam" is the real time of the entry. An empty state when there are no entries (not in the design: PROVISIONAL, built from the design's tokens). Opening the screen marks all as read. A tap opens the related place or event (`DetailTempatRoute` / `DetailPentasRoute`) when it has one. The design's sample rows (Keraton tutup, laporan diperiksa) have no producer yet; never show them as fake entries.
- Pengaturan notifikasi: the design comes from the web era. Show only the switches that have a real producer in this build ("Waktunya berangkat" / pengingat pentas, "Ambil cap" for stamps confirmed, and the quiet hours 21.00 to 06.00). Leave out "Notifikasi browser", "Email" and the "Paling banyak [ISI] per hari" cap. List the rows you left out in the report. Settings persist in DataStore.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. Tests cover: the repository (posting each event, ordering, unread count, mark all read, the 100-entry cap, quiet hours, a disabled category), the reminder delay calculation (each timing, a past show, replacement), the center ViewModel (chips, grouping by day), the settings ViewModel, `PengingatViewModel` calling the scheduler, and `StampSender` posting only on `Stamped`. Report as in the common brief. Say plainly that no notification was seen on a device.

package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/*
 * Routes of the notifikasi feature (the notification center itself is [NotifikasiRoute] in Routes.kt).
 */

/**
 * "Mau diingatkan sebelum pentas mulai?" (4.9), a sheet shown before the first reminder is saved when the
 * POST_NOTIFICATIONS permission is missing (API 33+). When it closes, whatever the user chose, it sets
 * [IZIN_NOTIF_RESULT_KEY] to true in the previous back stack entry's SavedStateHandle so the caller can go on.
 */
@Serializable data object IzinNotifRoute

/** Pengaturan notifikasi: switches for the notifications this build really produces, and the quiet hours. */
@Serializable data object PengaturanNotifRoute

/** SavedStateHandle key (set to true on the previous entry) when [IzinNotifRoute] has closed. */
const val IZIN_NOTIF_RESULT_KEY = "izin_notif_done"

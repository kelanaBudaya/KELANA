package id.kelana.feature.notifikasi.data

import id.kelana.core.model.UserDataCleaner

/**
 * Clears what the notification centre keeps for the user after "Hapus akun": the entries in the DataStore
 * (`notification_center_json`), every scheduled show reminder (WorkManager) and the notifications already shown in
 * the system tray. Each step runs even if an earlier one fails; the first failure is rethrown at the end.
 * Notification settings (switches, quiet hours) are device preferences and stay.
 */
class NotifikasiUserDataCleaner(
    private val storage: NotificationStorage,
    private val cancelScheduledReminders: () -> Unit,
    private val dismissShown: () -> Unit,
) : UserDataCleaner {

    override suspend fun clear() {
        var failure: Throwable? = null
        runCatching { cancelScheduledReminders() }.onFailure { failure = failure ?: it }
        runCatching { dismissShown() }.onFailure { failure = failure ?: it }
        runCatching { storage.update { emptyList() } }.onFailure { failure = failure ?: it }
        failure?.let { throw it }
    }
}

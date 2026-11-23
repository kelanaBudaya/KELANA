package id.kelana.feature.notifikasi.domain

import id.kelana.core.model.NotificationCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Switches of Pengaturan notifikasi. Only notifications this build really produces have a switch:
 * [showReminders] (the reminder chosen on the Pengingat sheet, "Waktunya berangkat") and [stampConfirmations]
 * ("Ambil cap": the server confirmed a stamp). [quietHours] holds back system notifications from 21.00 to 06.00.
 * A switch off never deletes entries: they still appear in the notification center.
 */
data class NotificationSettings(
    val showReminders: Boolean = true,
    val stampConfirmations: Boolean = true,
    val quietHours: Boolean = true,
) {
    /** Whether a system notification of [category] may be shown. Categories without a producer are never blocked. */
    fun allows(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.Rencana -> showReminders
        NotificationCategory.Paspor -> stampConfirmations
        NotificationCategory.Cerita, NotificationCategory.Info -> true
    }
}

/** The fixed quiet hours of the design ("Jangan kirim pukul 21.00 sampai 06.00"). */
object QuietHours {
    val START: LocalTime = LocalTime.of(21, 0)
    val END: LocalTime = LocalTime.of(6, 0)

    /** True from [START] (inclusive) to [END] (exclusive), across midnight. */
    fun contains(time: LocalTime): Boolean = !time.isBefore(START) || time.isBefore(END)
}

interface NotificationSettingsRepository {
    val settings: Flow<NotificationSettings>

    suspend fun update(transform: (NotificationSettings) -> NotificationSettings)
}

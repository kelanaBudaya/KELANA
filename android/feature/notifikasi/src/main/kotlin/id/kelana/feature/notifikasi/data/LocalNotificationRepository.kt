package id.kelana.feature.notifikasi.data

import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.NotificationRepository
import id.kelana.feature.notifikasi.domain.NotificationContentResolver
import id.kelana.feature.notifikasi.domain.NotificationSettingsRepository
import id.kelana.feature.notifikasi.domain.QuietHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** Most entries the center keeps; the oldest ones are dropped first. */
const val MAX_NOTIFICATIONS = 100

/**
 * The notification center. [post] always stores the entry, so the center keeps a reminder even when system
 * notifications are off. A system notification is shown only when all of these hold: the OS allows it
 * ([SystemNotifier.canShow]), the switch of the entry's category is on, it is not inside quiet hours (when that
 * switch is on), and the app is not in the foreground. Nothing here reads location.
 *
 * [zone] gives the time zone used for quiet hours; the device zone by default (the user's own night).
 */
class LocalNotificationRepository(
    private val storage: NotificationStorage,
    private val settings: NotificationSettingsRepository,
    private val resolver: NotificationContentResolver,
    private val notifier: SystemNotifier,
    private val foreground: AppForeground,
    private val clock: Clock,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : NotificationRepository {

    override fun notifications(): Flow<List<AppNotification>> =
        storage.entries.map { list -> list.sortedByDescending { it.createdAt } }

    override fun unreadCount(): Flow<Int> = storage.entries.map { list -> list.count { !it.isRead } }

    override suspend fun post(event: NotificationEvent) {
        val resolved = resolver.resolve(event)
        val now = clock.instant()
        val entry = AppNotification(
            id = newId(),
            category = resolved.category,
            title = resolved.title,
            body = resolved.body,
            createdAt = now,
            isRead = false,
            placeId = resolved.placeId,
            eventId = resolved.eventId,
        )
        storage.update { current ->
            // The new entry goes first so it also leads when two entries share one instant.
            (listOf(entry) + current).sortedByDescending { it.createdAt }.take(MAX_NOTIFICATIONS)
        }

        val s = settings.settings.first()
        val quiet = s.quietHours && QuietHours.contains(now.atZone(zone()).toLocalTime())
        val show = s.allows(entry.category) && !quiet && !foreground.isForeground() && notifier.canShow()
        if (show) notifier.show(entry)
    }

    override suspend fun markAllRead() {
        storage.update { list -> if (list.none { !it.isRead }) list else list.map { it.copy(isRead = true) } }
    }
}

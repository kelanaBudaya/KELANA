package id.kelana.core.model

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Filter chips on Notifikasi (5.1): Semua shows every category. */
enum class NotificationCategory { Rencana, Paspor, Cerita, Info }

/**
 * One entry in the notification center. [title] and [body] were resolved from the notifikasi feature's
 * strings when the entry was created. [placeId] / [eventId] say where a tap leads; both may be null.
 */
data class AppNotification(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val isRead: Boolean,
    val placeId: String? = null,
    val eventId: String? = null,
)

/**
 * What producers report. Producers never write user-facing text: the notifikasi feature turns each event
 * into its title and body (its own strings, id + en) and decides whether a system notification is shown.
 */
sealed interface NotificationEvent {
    /** The server confirmed a stamp (for example a queued "menunggu sinyal" request sent in the background). */
    data class StampConfirmed(val placeId: String) : NotificationEvent

    /** A reminder chosen on the Pengingat sheet (3.3) is due. */
    data class ShowReminderDue(val eventId: String, val timing: ReminderTiming) : NotificationEvent
}

/**
 * Local notification center. [post] stores an entry and shows a system notification when the user allowed
 * notifications and the category is switched on. Nothing here reads or stores location.
 */
interface NotificationRepository {
    /** Newest first. */
    fun notifications(): Flow<List<AppNotification>>

    fun unreadCount(): Flow<Int>

    suspend fun post(event: NotificationEvent)

    suspend fun markAllRead()
}

/** Schedules the local reminder chosen on the Pengingat sheet (3.3). Replaces an earlier one for the same event. */
interface ReminderScheduler {
    suspend fun scheduleShowReminder(eventId: String, choice: ReminderChoice)

    suspend fun cancelShowReminder(eventId: String)
}

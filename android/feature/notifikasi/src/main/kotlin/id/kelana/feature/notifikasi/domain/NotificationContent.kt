package id.kelana.feature.notifikasi.domain

import androidx.annotation.StringRes
import id.kelana.core.model.EventRepository
import id.kelana.core.model.NotificationCategory
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.notifikasi.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reads a string of this module's resources (id or en). A seam so tests need no Android. */
fun interface TextSource {
    fun get(@StringRes id: Int, vararg args: Any): String
}

/** An event turned into what the center stores. */
data class ResolvedNotification(
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val placeId: String? = null,
    val eventId: String? = null,
)

/**
 * Turns producer events into title and body (producers never write user-facing text). Names come from
 * [PlaceRepository] / [EventRepository]. A show time is only written when its official fact has a check date,
 * and then the date is written next to it; otherwise the text leaves the time out.
 */
class NotificationContentResolver(
    private val places: PlaceRepository,
    private val events: EventRepository,
    private val text: TextSource,
) {
    suspend fun resolve(event: NotificationEvent): ResolvedNotification = when (event) {
        is NotificationEvent.StampConfirmed -> stamp(event)
        is NotificationEvent.ShowReminderDue -> reminder(event)
    }

    private suspend fun stamp(event: NotificationEvent.StampConfirmed): ResolvedNotification {
        val place = places.place(event.placeId).first()
        return if (place == null) {
            ResolvedNotification(
                NotificationCategory.Paspor,
                text.get(R.string.notif_stamp_title_unknown),
                text.get(R.string.notif_stamp_body_unknown),
                placeId = event.placeId,
            )
        } else {
            ResolvedNotification(
                NotificationCategory.Paspor,
                text.get(R.string.notif_stamp_title, place.name),
                text.get(R.string.notif_stamp_body, place.name),
                placeId = place.id,
            )
        }
    }

    private suspend fun reminder(event: NotificationEvent.ShowReminderDue): ResolvedNotification {
        val show = events.event(event.eventId).first()
            ?: return ResolvedNotification(
                NotificationCategory.Rencana,
                text.get(R.string.notif_reminder_title_unknown),
                text.get(R.string.notif_reminder_body_unknown),
                eventId = event.eventId,
            )
        val fact = show.schedule
        val checked = fact.checkedAt
        val title = when (event.timing) {
            ReminderTiming.WhenToLeave -> text.get(R.string.notif_reminder_title_leave, show.placeName)
            ReminderTiming.OneHourBefore -> text.get(R.string.notif_reminder_title_hour, show.title)
            ReminderTiming.ThirtyMinutesBefore -> text.get(R.string.notif_reminder_title_half, show.title)
        }
        val body = if (checked == null) {
            text.get(R.string.notif_reminder_body_checking, show.title, show.placeName)
        } else {
            text.get(
                R.string.notif_reminder_body,
                show.title,
                formatTime(fact.value.start),
                show.placeName,
            )
        }
        return ResolvedNotification(NotificationCategory.Rencana, title, body, placeId = show.placeId, eventId = show.id)
    }

    private fun formatTime(time: LocalTime): String = TIME_FORMAT.format(time)

    private companion object {
        val ID: Locale = Locale.forLanguageTag("id-ID")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH.mm", ID)
    }
}

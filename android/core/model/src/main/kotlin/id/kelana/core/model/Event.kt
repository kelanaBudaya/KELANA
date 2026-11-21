package id.kelana.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** How often an event takes place. Mirrors the Postgres enum `recurrence_kind`. */
sealed interface Recurrence {
    data class Once(val date: LocalDate) : Recurrence
    data class Weekly(val weekday: DayOfWeek) : Recurrence
    data object Daily : Recurrence

    fun occursOn(date: LocalDate): Boolean = when (this) {
        is Once -> this.date == date
        is Weekly -> weekday == date.dayOfWeek
        Daily -> true
    }
}

/** When an event runs. An official fact, see [Event.schedule]. */
data class EventSchedule(
    val recurrence: Recurrence,
    val start: LocalTime,
    val end: LocalTime?,
)

/** Ticket info of an event. [priceFromIdr] null means free or not known; [sellerUrl] is the official seller. */
data class EventTicket(
    val priceFromIdr: Long?,
    val sellerUrl: String?,
)

/** Filter tag of an event. Mirrors the allowed values of `events.tags` (`tari`, `wayang`, `ramah_anak`). */
enum class EventTag(val key: String) {
    Tari("tari"),
    Wayang("wayang"),
    RamahAnak("ramah_anak"),
    ;

    companion object {
        fun fromKey(key: String): EventTag? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A performance or recurring show at a place.
 * [placeName] is denormalised for list rows. [performers] is free text ("Lebih dari 200 penari dan pemusik lokal").
 * [arrivalAdviceMin] and [arrivalAdviceMax] are minutes before the show; both null or both set.
 */
data class Event(
    val id: String,
    val placeId: String,
    val placeName: String,
    val title: String,
    val description: String,
    val category: PlaceCategory,
    val schedule: OfficialFact<EventSchedule>,
    val ticket: OfficialFact<EventTicket>,
    val performers: List<String> = emptyList(),
    val arrivalAdviceMin: Int? = null,
    val arrivalAdviceMax: Int? = null,
    val tags: Set<EventTag> = emptySet(),
) {
    fun occursOn(date: LocalDate): Boolean = schedule.value.recurrence.occursOn(date)

    /** Dates in `[from, to]` (inclusive) on which this event takes place. */
    fun occurrencesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .filter(::occursOn)
            .toList()
    }

    /** The next [count] dates on or after [from], looking at most one year ahead. */
    fun nextOccurrences(from: LocalDate, count: Int): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .take(366)
            .filter(::occursOn)
            .take(count)
            .toList()
}

/** One event on one date, the unit of the agenda list. */
data class EventOccurrence(val event: Event, val date: LocalDate)

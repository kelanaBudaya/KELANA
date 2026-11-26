package id.kelana.core.data.repository

import id.kelana.core.model.Closure
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.Place
import java.time.LocalDate

/**
 * Expands events into dated occurrences in `[from, to]`, ordered by date, start time, title. Used by the fake and
 * the Supabase repositories so both behave the same. [cancelledDays] maps a place id to closures; an occurrence
 * that falls inside a closure of its place is dropped (cancelled dates come from `closures`).
 */
internal fun expandOccurrences(
    events: List<Event>,
    from: LocalDate,
    to: LocalDate,
    cancelledDays: Map<String, List<Closure>> = emptyMap(),
): List<EventOccurrence> =
    events.flatMap { event ->
        val closures = cancelledDays[event.placeId].orEmpty()
        event.occurrencesBetween(from, to)
            .filter { date -> closures.none { date in it } }
            .map { EventOccurrence(event, it) }
    }.sortedWith(compareBy({ it.date }, { it.event.schedule.value.start }, { it.event.title }))

/**
 * The one scoping rule: with no selected region everything is returned (onboarding, Pilih daerah); with one, only
 * items whose [regionOf] equals it. An item with an unknown region (null) is not shown in a region.
 */
internal fun <T> List<T>.inRegion(selected: String?, regionOf: (T) -> String?): List<T> =
    if (selected == null) this else filter { regionOf(it) == selected }

/** Case-insensitive match on name, address and description. A blank query matches nothing. */
internal fun List<Place>.searchPlaces(query: String): List<Place> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return filter {
        it.name.contains(q, ignoreCase = true) ||
            it.address.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
    }
}

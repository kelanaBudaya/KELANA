package id.kelana.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * A place. Plain fields are KELANA content. Hours, ticket prices and closures are official facts and are
 * always wrapped in [OfficialFact].
 *
 * [id] is a slug in the sample data; with the real backend it is the row uuid. Treat it as opaque.
 * [coverKey] names the cover illustration or image; the UI maps it to a drawable or URL.
 */
data class Place(
    val id: String,
    val slug: String,
    val name: String,
    val category: PlaceCategory,
    val capCode: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val description: String,
    val coverKey: String,
    val hours: List<OfficialFact<OpeningHours>> = emptyList(),
    val tickets: List<OfficialFact<TicketPrice>> = emptyList(),
    val closures: List<OfficialFact<Closure>> = emptyList(),
    /** Code of the region the place belongs to (e.g. "DIY"); null when unknown. Repositories scope lists by it. */
    val regionId: String? = null,
)

/** Opening hours for one weekday. When [isClosed] is true, [opens] and [closes] are null. */
data class OpeningHours(
    val weekday: DayOfWeek,
    val isClosed: Boolean,
    val opens: LocalTime?,
    val closes: LocalTime?,
    val note: String? = null,
)

/** A closed period (both dates inclusive). */
data class Closure(
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val reason: String?,
) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(startsOn) && !date.isAfter(endsOn)
}

/** One ticket tier. [priceIdr] null means the price is not known yet. [sellerUrl] is the official seller. */
data class TicketPrice(
    val label: String,
    val priceIdr: Long?,
    val sellerUrl: String?,
)

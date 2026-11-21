package id.kelana.feature.tempat.domain

import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import java.net.URI
import java.time.DayOfWeek

/** PROVISIONAL: the number of official photos is fixed until the photo source exists (the frame says 4). */
const val OFFICIAL_PHOTO_COUNT = 4

/** The opening-hours fact of [today], or null when the place has none for that weekday. */
fun todayHours(hours: List<OfficialFact<OpeningHours>>, today: DayOfWeek): OfficialFact<OpeningHours>? =
    hours.firstOrNull { it.value.weekday == today }

/**
 * Closed weekdays as one official fact. It has a check date only when every weekly hours fact has one (the
 * oldest date is used), so a closed day is never shown without a date. Null when there are no hours at all.
 */
fun closedDaysFact(hours: List<OfficialFact<OpeningHours>>): OfficialFact<List<DayOfWeek>>? {
    if (hours.isEmpty()) return null
    val closed = hours.filter { it.value.isClosed }.map { it.value.weekday }.sorted()
    val checkedAt = if (hours.any { it.checkedAt == null }) null else hours.mapNotNull { it.checkedAt }.minOrNull()
    return OfficialFact(value = closed, sourceUrl = hours.firstNotNullOfOrNull { it.sourceUrl }, checkedAt = checkedAt)
}

/** Host of the first checked source of the place's official facts, for the "Sumber" note. Null when none. */
fun sourceHost(place: Place): String? {
    val urls = place.hours.checkedUrls() + place.tickets.checkedUrls() + place.closures.checkedUrls()
    return urls.firstNotNullOfOrNull { url -> runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") }
}

private fun <T> List<OfficialFact<T>>.checkedUrls(): List<String> =
    filter { it.checkedAt != null }.mapNotNull { it.sourceUrl }

/** First ticket seller URL that belongs to a checked ticket fact. */
fun officialSellerUrl(place: Place): String? =
    place.tickets.filter { it.checkedAt != null }.firstNotNullOfOrNull { it.value.sellerUrl }

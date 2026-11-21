package id.kelana.feature.tempat.domain

import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import java.net.URI
import java.time.DayOfWeek
import java.time.LocalTime

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

/** A run of consecutive open weekdays that share the same hours, for example Tuesday to Sunday 08.00 to 21.00. */
data class HoursSummary(
    val firstDay: DayOfWeek,
    val lastDay: DayOfWeek,
    val opens: LocalTime,
    val closes: LocalTime?,
)

/**
 * Weekly hours as one official fact, when the open days form one run of consecutive weekdays (Monday first, no
 * wrap) with identical hours. The check date is the oldest one and is null when any hours fact has none, so the
 * summary never shows without a date. Null when the hours do not fit that pattern: the caller falls back to today's hours.
 */
fun hoursSummary(hours: List<OfficialFact<OpeningHours>>): OfficialFact<HoursSummary>? {
    val open = hours.filter { !it.value.isClosed && it.value.opens != null }.sortedBy { it.value.weekday }
    if (open.isEmpty() || open.size != open.map { it.value.weekday }.distinct().size) return null
    val first = open.first().value
    val sameHours = open.all { it.value.opens == first.opens && it.value.closes == first.closes }
    val consecutive = open.zipWithNext().all { (a, b) -> b.value.weekday.value == a.value.weekday.value + 1 }
    if (!sameHours || !consecutive) return null
    val checkedAt = if (hours.any { it.checkedAt == null }) null else hours.mapNotNull { it.checkedAt }.minOrNull()
    return OfficialFact(
        value = HoursSummary(first.weekday, open.last().value.weekday, first.opens!!, first.closes),
        sourceUrl = hours.firstNotNullOfOrNull { it.sourceUrl },
        checkedAt = checkedAt,
    )
}

/** Oldest check date among the place's checked official facts (hours, tickets, closures); null when none is checked. */
fun infoCheckedAt(place: Place): java.time.LocalDate? =
    (place.hours.map { it.checkedAt } + place.tickets.map { it.checkedAt } + place.closures.map { it.checkedAt })
        .filterNotNull()
        .minOrNull()

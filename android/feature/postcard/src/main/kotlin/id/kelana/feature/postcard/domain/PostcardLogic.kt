package id.kelana.feature.postcard.domain

import id.kelana.core.model.POSTCARD_MESSAGE_MAX
import id.kelana.core.model.Place
import id.kelana.core.model.Stamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A place the user holds a confirmed stamp for. */
data class StampedPlace(val place: Place, val stampedAt: Instant)

/** The user's stamped places, in passport order. A place with several stamps keeps the earliest. Unknown places are dropped. */
fun stampedPlaces(places: List<Place>, stamps: List<Stamp>): List<StampedPlace> {
    val byPlace = stamps.groupBy { it.placeId.lowercase() }
    return places.mapNotNull { place ->
        byPlace[place.id.lowercase()]?.minOfOrNull { it.stampedAt }?.let { StampedPlace(place, it) }
    }
}

/** True when the message is 1 to [POSTCARD_MESSAGE_MAX] characters after trimming. */
fun isMessageValid(message: String): Boolean = message.trim().length in 1..POSTCARD_MESSAGE_MAX

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "20 NOV 2026" for the "DIKUNJUNGI" badge: the stamp date, never today's date. */
fun formatVisitedDate(stampedAt: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", IdLocale).withZone(zone).format(stampedAt)
        .uppercase(IdLocale)
        .replace(".", "")

/** "20.11.26" for the round cap on the card. */
fun formatStampDate(stampedAt: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd.MM.yy", IdLocale).withZone(zone).format(stampedAt)

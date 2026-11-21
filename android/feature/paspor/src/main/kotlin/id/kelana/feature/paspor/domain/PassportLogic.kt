package id.kelana.feature.paspor.domain

import id.kelana.core.model.Place
import id.kelana.core.model.Stamp
import java.time.Instant

/** One place of the passport, in passport order. [stampedAt] is null until the server confirmed the stamp. */
data class PassportItem(val place: Place, val stampedAt: Instant?) {
    val isCollected: Boolean get() = stampedAt != null
}

/** Joins the places (passport order) with the user's own stamps. Stamps for unknown places are ignored. */
fun buildPassport(places: List<Place>, stamps: List<Stamp>): List<PassportItem> {
    val byPlace = stamps.groupBy { it.placeId.lowercase() }
    return places.map { place ->
        PassportItem(place, byPlace[place.id.lowercase()]?.minOfOrNull { it.stampedAt })
    }
}

/** Number of collected places. Real count: 0 when there is no stamp. */
fun List<PassportItem>.collectedCount(): Int = count { it.isCollected }

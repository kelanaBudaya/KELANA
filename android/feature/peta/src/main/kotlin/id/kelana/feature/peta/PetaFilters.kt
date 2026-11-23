package id.kelana.feature.peta

import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.checkedOnly
import java.time.LocalDate
import java.time.LocalTime

/** Filter chips of the map. */
internal enum class PetaFilter { All, OpenToday, HasCap }

/**
 * True only when the checked opening hours say the place is open on [date] and no checked closure covers it.
 * Unchecked facts are never relied on (AGENTS.md). Copied from `:feature:jelajah` PlaceFilters.kt because features
 * never import each other.
 */
internal fun Place.isOpenOn(date: LocalDate): Boolean {
    val day = hours.firstOrNull { it.value.weekday == date.dayOfWeek }?.display() as? FactDisplay.Checked
        ?: return false
    if (day.value.isClosed) return false
    return closures.none { fact ->
        val shown = fact.display()
        shown is FactDisplay.Checked && date in shown.value
    }
}

/** PROVISIONAL interpretation of "Ada cap": the place has a stamp code (a non-blank `capCode`). */
internal fun Place.hasCap(): Boolean = capCode.isNotBlank()

/** A place plus the facts the map needs about it today. */
internal data class PetaPlace(
    val place: Place,
    val openToday: Boolean,
    val showToday: Boolean,
)

internal fun PetaPlace.matches(filter: PetaFilter): Boolean = when (filter) {
    PetaFilter.All -> true
    PetaFilter.OpenToday -> openToday
    PetaFilter.HasCap -> place.hasCap()
}

/** The one status badge a place card may show. Only data-backed badges exist. */
internal enum class PetaBadge { ShowTonight, OpenToday }

/** "Pentas malam ini" wins over "Buka hari ini"; no data, no badge. */
internal fun badgeFor(showToday: Boolean, openToday: Boolean): PetaBadge? = when {
    showToday -> PetaBadge.ShowTonight
    openToday -> PetaBadge.OpenToday
    else -> null
}

/** Ids of places that have a show with a checked schedule in [occurrences] (the day's events). */
internal fun placeIdsWithShow(occurrences: List<EventOccurrence>): Set<String> =
    occurrences.filter { it.event.schedule.checkedAt != null }.map { it.event.placeId }.toSet()

/** Today's hours of a place for the card. */
internal sealed interface HoursLine {
    data class Open(val opens: LocalTime, val closes: LocalTime?) : HoursLine
    data object Closed : HoursLine
    data object Checking : HoursLine
}

/** Price of a place for the card. */
internal sealed interface PriceLine {
    data object Free : PriceLine
    data class Paid(val fromIdr: Long, val isFrom: Boolean) : PriceLine
    data object Checking : PriceLine
}

/** Facts for the card. [checkedAt] is the oldest check date among the facts that are shown with a value. */
internal data class CardFacts(val hours: HoursLine, val price: PriceLine, val checkedAt: LocalDate?)

internal fun cardFacts(place: Place, today: LocalDate): CardFacts {
    val hoursFact = place.hours.firstOrNull { it.value.weekday == today.dayOfWeek }
    val shownHours = hoursFact?.display() as? FactDisplay.Checked
    val opens = shownHours?.value?.opens
    val hours = when {
        shownHours == null -> HoursLine.Checking
        shownHours.value.isClosed -> HoursLine.Closed
        opens == null -> HoursLine.Checking
        else -> HoursLine.Open(opens, shownHours.value.closes)
    }
    val checkedTickets = place.tickets.checkedOnly().filter { it.value.priceIdr != null }
    val prices = checkedTickets.mapNotNull { it.value.priceIdr }
    val price = when {
        prices.isEmpty() -> PriceLine.Checking
        prices.all { it == 0L } -> PriceLine.Free
        else -> PriceLine.Paid(prices.min(), isFrom = prices.distinct().size > 1)
    }
    val dates = buildList {
        if (hours !is HoursLine.Checking) shownHours?.let { add(it.checkedAt) }
        if (price !is PriceLine.Checking) checkedTickets.mapNotNullTo(this) { it.checkedAt }
    }
    return CardFacts(hours, price, dates.minOrNull())
}

/** Marker glyph family of a category. Categories without their own glyph share the building glyph. */
internal enum class PinGlyph { Candi, Museum, Batik, Building }

internal fun PlaceCategory.glyph(): PinGlyph = when (this) {
    PlaceCategory.Candi -> PinGlyph.Candi
    PlaceCategory.Museum -> PinGlyph.Museum
    PlaceCategory.Kerajinan -> PinGlyph.Batik
    else -> PinGlyph.Building
}

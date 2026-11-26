package id.kelana.feature.jelajah

import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Latest check date among the place's hours; null when no hour fact has been checked (then show "sedang dicek"). */
internal fun Place.latestCheckedAt(): LocalDate? =
    hours.mapNotNull { (it.display() as? FactDisplay.Checked)?.checkedAt }.maxOrNull()

/**
 * True only when the checked opening hours say the place is open on [date] and no checked closure covers it.
 * Unchecked facts are never used (AGENTS.md: a fact without `checkedAt` is not rendered or relied on).
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

/** Region time zone: "today" is the Jakarta calendar day, not the device's. */
internal val JakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")

/** Today's date in Asia/Jakarta; tests pass a fixed [clock]. */
internal fun todayInJakarta(clock: Clock = Clock.system(JakartaZone)): LocalDate =
    LocalDate.now(clock.withZone(JakartaZone))

/** The "Kunjungi Hari Ini" badge: checked hours say open on [today]. A null [today] (not known yet) shows nothing. */
internal fun isVisitToday(place: Place, today: LocalDate?): Boolean = today != null && place.isOpenOn(today)

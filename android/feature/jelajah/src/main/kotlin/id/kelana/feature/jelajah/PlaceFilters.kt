package id.kelana.feature.jelajah

import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import java.time.LocalDate

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

package id.kelana.core.model

import java.time.LocalDate

/**
 * Product rule (AGENTS.md): every official fact (hours, price, closed day, show time) carries its source and the
 * date it was last checked. A fact with [checkedAt] == null is representable (the value is not trusted yet) and
 * must be shown as "sedang dicek", never as a value. Screens read facts only through [display].
 */
data class OfficialFact<out T>(
    val value: T,
    val sourceUrl: String?,
    val checkedAt: LocalDate?,
) {
    /** What a screen may render for this fact. */
    fun display(): FactDisplay<T> =
        if (checkedAt == null) FactDisplay.Checking else FactDisplay.Checked(value, checkedAt, sourceUrl)
}

/** Render state of an [OfficialFact]. */
sealed interface FactDisplay<out T> {
    /** Show the value together with "Diperiksa <checkedAt>". */
    data class Checked<out T>(val value: T, val checkedAt: LocalDate, val sourceUrl: String?) : FactDisplay<T>

    /** No check date: show "sedang dicek" and no value. */
    data object Checking : FactDisplay<Nothing>
}

/** Keeps only the facts that may be rendered with a value. */
fun <T> List<OfficialFact<T>>.checkedOnly(): List<OfficialFact<T>> = filter { it.checkedAt != null }

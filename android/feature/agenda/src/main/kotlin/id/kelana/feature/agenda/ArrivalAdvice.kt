package id.kelana.feature.agenda

/** How early to arrive, in minutes before the show. */
internal sealed interface ArrivalAdvice {
    data class Range(val min: Int, val max: Int) : ArrivalAdvice

    data class About(val minutes: Int) : ArrivalAdvice
}

/**
 * Both bounds and different: a range. Equal bounds, or only one bound known: "about N". Nothing known (or a
 * non-positive value): null, so the note is not shown.
 */
internal fun arrivalAdvice(min: Int?, max: Int?): ArrivalAdvice? {
    val lo = min?.takeIf { it > 0 }
    val hi = max?.takeIf { it > 0 }
    return when {
        lo != null && hi != null && lo != hi -> ArrivalAdvice.Range(minOf(lo, hi), maxOf(lo, hi))
        lo != null -> ArrivalAdvice.About(lo)
        hi != null -> ArrivalAdvice.About(hi)
        else -> null
    }
}

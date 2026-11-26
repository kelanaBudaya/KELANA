package id.kelana.core.ui.map

/*
 * Pure layout for map pins whose boxes overlap on screen. No Android or Compose imports, so it is unit tested as plain
 * Kotlin. Keep the algorithm deterministic: the same input always gives the same output.
 */

/** How OsmMap resolves pins whose boxes overlap on screen. */
enum class MarkerOverlap {
    /** Fan overlapping pins out side by side (few pins, all must stay visible, e.g. Peta rute). */
    Spread,

    /** Merge overlapping pins into one count bubble (many pins, e.g. the Peta tab). */
    Cluster,
}

/** A marker's projected tip position, in px. */
internal data class ScreenPoint(val key: String, val x: Float, val y: Float)

internal sealed interface PlacedItem {
    /** One pin whose tip is drawn at (x, y). (anchorX, anchorY) is the true spot; it differs only for a spread pin. */
    data class Single(
        val key: String,
        val x: Float,
        val y: Float,
        val anchorX: Float = x,
        val anchorY: Float = y,
    ) : PlacedItem {
        val displaced: Boolean get() = x != anchorX || y != anchorY
    }

    /** A cluster bubble centred at (x, y) = centroid of the members; keys are in input order, size >= 2. */
    data class Group(val keys: List<String>, val x: Float, val y: Float) : PlacedItem
}

/** Zoom from which a cluster tap opens the member list instead of zooming further. */
internal const val CLUSTER_LIST_ZOOM = 17.5

/**
 * Lays out [points] for [mode].
 *
 * Cluster: pins whose boxes overlap merge into one [PlacedItem.Group]. The selected pin is never clustered and is
 * returned last so it is drawn on top. Spread: overlapping pins fan out side by side, centred on the seed pin, and
 * keep their true spot in the anchor fields; the selected pin takes part like any other.
 *
 * Overlap is compared against the group's seed only (not every member), so the result is order-dependent: shuffled
 * input may give a different grouping.
 */
internal fun layoutMarkers(
    points: List<ScreenPoint>,
    selectedKey: String?,
    mode: MarkerOverlap,
    boxWidthPx: Float,   // caller passes PinWidth (76 dp) in px
    boxHeightPx: Float,  // caller passes PinCollisionHeight (72 dp) in px
    spreadStepPx: Float, // caller passes PinWidth (76 dp) in px
): List<PlacedItem> {
    val sel: ScreenPoint?
    val pool: List<ScreenPoint>
    if (mode == MarkerOverlap.Cluster) {
        val selIndex = points.indexOfFirst { it.key == selectedKey }
        sel = points.getOrNull(selIndex)
        pool = points.filterIndexed { i, _ -> i != selIndex }
    } else {
        sel = null
        pool = points
    }

    // Strict <: two boxes exactly one box-width apart do not collide.
    fun collides(a: ScreenPoint, b: ScreenPoint): Boolean =
        kotlin.math.abs(a.x - b.x) < boxWidthPx && kotlin.math.abs(a.y - b.y) < boxHeightPx

    val assigned = BooleanArray(pool.size)
    val out = mutableListOf<PlacedItem>()
    for (i in pool.indices) {
        if (assigned[i]) continue
        val seed = pool[i]
        val group = mutableListOf(seed)
        assigned[i] = true
        for (j in i + 1 until pool.size) {
            if (!assigned[j] && collides(seed, pool[j])) {
                group += pool[j]
                assigned[j] = true
            }
        }

        if (group.size == 1) {
            out += PlacedItem.Single(seed.key, seed.x, seed.y)
        } else if (mode == MarkerOverlap.Cluster) {
            val n = group.size
            out += PlacedItem.Group(
                keys = group.map { it.key },
                x = (group.sumOf { it.x.toDouble() } / n).toFloat(),
                y = (group.sumOf { it.y.toDouble() } / n).toFloat(),
            )
        } else {
            // Spread: fan out 76 dp apart, centred on the seed; the anchor keeps the true spot.
            val n = group.size
            for ((k, p) in group.withIndex()) {
                val dx = (k - (n - 1) / 2f) * spreadStepPx
                out += PlacedItem.Single(p.key, x = seed.x + dx, y = seed.y, anchorX = p.x, anchorY = p.y)
            }
        }
    }

    if (sel != null) out += PlacedItem.Single(sel.key, sel.x, sel.y) // last, so it is drawn on top
    return out
}

/** True when a cluster tap should open the member list instead of zooming. */
internal fun shouldListCluster(members: List<Pair<Double, Double>>, zoom: Double): Boolean {
    val bounds = boundsOf(members) ?: return false
    return zoom >= CLUSTER_LIST_ZOOM || bounds.isPoint() // isPoint() default epsilon 1e-4 deg (about 11 m)
}

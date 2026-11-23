package id.kelana.feature.peta

import kotlin.math.abs

/*
 * Illustrated map projection. The drawing is not to scale, so a place is placed by projecting its own lat/lng
 * linearly into a bounding box and then into a "safe area" of the drawing (so no pin hides under the back button and
 * filter chips at the top or the hint bar at the bottom). Everything here is plain Kotlin so it can be unit tested.
 */

/** A lat/lng bounding box. */
internal data class GeoBounds(val north: Double, val south: Double, val west: Double, val east: Double)

/** A rectangle in unit space (0..1 of the drawing's width and height). */
internal data class UnitRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * PROVISIONAL: bounding box of the launch region (DI Yogyakarta city plus Kotagede and Prambanan). It is one
 * constant, not derived from the data, so the drawing does not shift when a filter hides a place. A place outside
 * it is clamped to the nearest edge of [PIN_SAFE_AREA]. Tune once the real place list is known.
 */
internal val PETA_BOUNDS = GeoBounds(north = -7.74, south = -7.84, west = 110.34, east = 110.50)

/**
 * PROVISIONAL: part of the drawing that pins may use. Top 14% leaves room for the back button and chips, the
 * bottom 28% for the place card and hint bar; 10% sides keep the pin label inside the screen.
 */
internal val PIN_SAFE_AREA = UnitRect(left = 0.10f, top = 0.14f, right = 0.90f, bottom = 0.72f)

/** PROVISIONAL: space one pin needs (the 44 dp drop is 44 wide, its label up to about 80), in dp. */
internal const val PIN_MIN_DX_DP = 80f
internal const val PIN_MIN_DY_DP = 72f

/** A point in unit space of the drawing. */
internal data class UnitPoint(val x: Float, val y: Float)

/**
 * Projects [lat], [lng] linearly into [safe] (north is up). A value outside [bounds] is clamped to the edge, so the
 * result is always inside [safe].
 */
internal fun projectToUnit(
    lat: Double,
    lng: Double,
    bounds: GeoBounds = PETA_BOUNDS,
    safe: UnitRect = PIN_SAFE_AREA,
): UnitPoint {
    val tx = ((lng - bounds.west) / (bounds.east - bounds.west)).coerceIn(0.0, 1.0).toFloat()
    val ty = ((bounds.north - lat) / (bounds.north - bounds.south)).coerceIn(0.0, 1.0).toFloat()
    return UnitPoint(safe.left + tx * safe.width, safe.top + ty * safe.height)
}

/** A pin to place: [key] identifies it, ([x], [y]) is its projected centre in unit space. */
internal data class PinSeed(val key: String, val x: Float, val y: Float)

/** A pin position in dp inside the drawing. */
internal data class PinPosition(val key: String, val xDp: Float, val yDp: Float)

/**
 * Turns projected pins into dp positions and separates overlapping ones.
 *
 * Nudge rule (deterministic, order of [seeds] breaks ties): two pins overlap when they are closer than
 * [minDx] horizontally and [minDy] vertically. The pair is pushed apart along the axis with the smaller overlap, half
 * each; pins at the exact same point are split vertically (the later one goes down). This repeats until no pair
 * overlaps or [maxIterations] is reached, and after every push the pin is clamped back into the safe area, so a
 * pin never leaves the drawing even when the safe area is crowded (then pins may still touch).
 */
internal fun layoutPins(
    seeds: List<PinSeed>,
    widthDp: Float,
    heightDp: Float,
    safe: UnitRect = PIN_SAFE_AREA,
    minDx: Float = PIN_MIN_DX_DP,
    minDy: Float = PIN_MIN_DY_DP,
    maxIterations: Int = 200,
): List<PinPosition> {
    val minX = safe.left * widthDp
    val maxX = safe.right * widthDp
    val minY = safe.top * heightDp
    val maxY = safe.bottom * heightDp
    val xs = FloatArray(seeds.size) { (seeds[it].x * widthDp).coerceIn(minX, maxX) }
    val ys = FloatArray(seeds.size) { (seeds[it].y * heightDp).coerceIn(minY, maxY) }
    repeat(maxIterations) {
        var moved = false
        for (i in seeds.indices) for (j in i + 1 until seeds.size) {
            var dx = xs[j] - xs[i]
            var dy = ys[j] - ys[i]
            if (abs(dx) >= minDx || abs(dy) >= minDy) continue
            if (dx == 0f && dy == 0f) dy = 1f
            val overlapX = minDx - abs(dx)
            val overlapY = minDy - abs(dy)
            if (overlapY <= overlapX) {
                val push = overlapY / 2f * if (dy >= 0f) 1f else -1f
                ys[i] = (ys[i] - push).coerceIn(minY, maxY)
                ys[j] = (ys[j] + push).coerceIn(minY, maxY)
            } else {
                val push = overlapX / 2f * if (dx >= 0f) 1f else -1f
                xs[i] = (xs[i] - push).coerceIn(minX, maxX)
                xs[j] = (xs[j] + push).coerceIn(minX, maxX)
            }
            moved = true
        }
        if (!moved) return seeds.indices.map { PinPosition(seeds[it].key, xs[it], ys[it]) }
    }
    return seeds.indices.map { PinPosition(seeds[it].key, xs[it], ys[it]) }
}

/**
 * Short label under a pin. PROVISIONAL rule (the design hand-wrote its labels): drop a trailing " Yogyakarta" and a
 * leading "Museum Benteng ", "Museum ", "Candi " or "Puro ", e.g. "Museum Benteng Vredeburg" becomes "Vredeburg".
 * Falls back to the full name when nothing is left.
 */
internal fun pinLabel(name: String): String {
    var label = name.trim().removeSuffix(" Yogyakarta")
    for (prefix in listOf("Museum Benteng ", "Museum ", "Candi ", "Puro ")) {
        if (label.startsWith(prefix)) {
            label = label.removePrefix(prefix)
            break
        }
    }
    return label.ifBlank { name.trim() }
}

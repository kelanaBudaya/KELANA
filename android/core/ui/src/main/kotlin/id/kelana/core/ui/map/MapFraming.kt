package id.kelana.core.ui.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/*
 * Camera framing for the OpenStreetMap view. The library projects places onto the screen, so only the initial camera
 * is computed here, as plain Kotlin that can be unit tested.
 */

/** A lat/lng bounding box. */
internal data class GeoBounds(val north: Double, val south: Double, val west: Double, val east: Double) {
    val centerLat: Double get() = (north + south) / 2
    val centerLng: Double get() = (west + east) / 2
}

internal const val SINGLE_PLACE_ZOOM = 14.0

/** Bounds of all [places] (lat, lng pairs); null when empty. */
internal fun boundsOf(places: List<Pair<Double, Double>>): GeoBounds? {
    if (places.isEmpty()) return null
    return GeoBounds(
        north = places.maxOf { it.first },
        south = places.minOf { it.first },
        west = places.minOf { it.second },
        east = places.maxOf { it.second },
    )
}

/** True when the box is too small to frame with a bounds fit (zero or one point): use a fixed zoom instead. */
internal fun GeoBounds.isPoint(epsilon: Double = 1e-4): Boolean =
    max(north - south, east - west) < epsilon

/** The map viewport and the room pins need, all in dp (MapLibre zoom maths is in dp: the world is 512 dp wide at zoom 0). */
internal data class FramingViewport(
    val widthDp: Double,
    val heightDp: Double,
    /** Area covered by overlays (chips, hint card): pins must stay clear of it. */
    val padLeftDp: Double,
    val padTopDp: Double,
    val padRightDp: Double,
    val padBottomDp: Double,
    /** Pin box around its tip: half the width, the height above the tip (drop, top inset, scale) and the label below it. */
    val pinHalfWidthDp: Double,
    val pinAboveDp: Double,
    val pinBelowDp: Double,
)

/** A camera position: centre and zoom. */
internal data class CameraFit(val lat: Double, val lng: Double, val zoom: Double)

private const val WORLD_DP_AT_ZOOM_0 = 512.0

private fun mercX(lng: Double) = (lng + 180.0) / 360.0
private fun mercY(lat: Double): Double {
    val clamped = lat.coerceIn(-85.0, 85.0)
    return 0.5 - ln(tan(PI / 4 + Math.toRadians(clamped) / 2)) / (2 * PI)
}
private fun lngOf(x: Double) = x * 360.0 - 180.0
private fun latOf(y: Double) = Math.toDegrees(atan(sinh(PI * (1 - 2 * y))))

/**
 * Camera that shows every point of [places] (lat, lng) with its whole pin inside the free part of [viewport]: pin tips
 * are kept [FramingViewport.pinHalfWidthDp] from the side padding, [FramingViewport.pinAboveDp] below the top padding
 * and [FramingViewport.pinBelowDp] above the bottom padding. Works in Web Mercator (the same projection as the map) at the
 * largest zoom in [minZoom]..[maxZoom] that fits; a single point or points that coincide use [singleZoom]. Null when empty.
 * Zoom is not clamped up to the map's own minimum on purpose: [minZoom] must be low enough for the widest region.
 */
internal fun fitCamera(
    places: List<Pair<Double, Double>>,
    viewport: FramingViewport,
    minZoom: Double,
    maxZoom: Double,
    singleZoom: Double = SINGLE_PLACE_ZOOM,
): CameraFit? {
    val bounds = boundsOf(places) ?: return null
    val left = viewport.padLeftDp + viewport.pinHalfWidthDp
    val right = viewport.widthDp - viewport.padRightDp - viewport.pinHalfWidthDp
    val top = viewport.padTopDp + viewport.pinAboveDp
    val bottom = viewport.heightDp - viewport.padBottomDp - viewport.pinBelowDp
    val availW = max(right - left, 1.0)
    val availH = max(bottom - top, 1.0)

    val x0 = mercX(bounds.west)
    val x1 = mercX(bounds.east)
    val y0 = mercY(bounds.north) // north has the smaller y
    val y1 = mercY(bounds.south)
    val dx = x1 - x0
    val dy = y1 - y0

    val zoom = if (bounds.isPoint()) {
        singleZoom
    } else {
        val zx = if (dx > 0) log2(availW / (dx * WORLD_DP_AT_ZOOM_0)) else Double.MAX_VALUE
        val zy = if (dy > 0) log2(availH / (dy * WORLD_DP_AT_ZOOM_0)) else Double.MAX_VALUE
        min(zx, zy)
    }.coerceIn(minZoom, maxZoom)

    // Put the middle of the bounds in the middle of the free rectangle; the camera centre is the viewport centre.
    val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom)
    val shiftX = (viewport.widthDp / 2 - (left + right) / 2) / world
    val shiftY = (viewport.heightDp / 2 - (top + bottom) / 2) / world
    val cx = (x0 + x1) / 2 + shiftX
    val cy = (y0 + y1) / 2 + shiftY
    return CameraFit(lat = latOf(cy), lng = lngOf(cx), zoom = zoom)
}

/** Screen position (dp, from the top-left of the viewport) of a point under [camera]; the inverse check of [fitCamera]. */
internal fun projectToViewport(lat: Double, lng: Double, camera: CameraFit, viewport: FramingViewport): Pair<Double, Double> {
    val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(camera.zoom)
    val x = viewport.widthDp / 2 + (mercX(lng) - mercX(camera.lng)) * world
    val y = viewport.heightDp / 2 + (mercY(lat) - mercY(camera.lat)) * world
    return x to y
}

/**
 * Short label under a pin. PROVISIONAL rule (the design hand-wrote its labels): drop a trailing " Yogyakarta" and a
 * leading "Museum Benteng ", "Museum ", "Candi " or "Puro ", e.g. "Museum Benteng Vredeburg" becomes "Vredeburg".
 * Falls back to the full name when nothing is left.
 */
fun pinLabel(name: String): String {
    var label = name.trim().removeSuffix(" Yogyakarta")
    for (prefix in listOf("Museum Benteng ", "Museum ", "Candi ", "Puro ")) {
        if (label.startsWith(prefix)) {
            label = label.removePrefix(prefix)
            break
        }
    }
    return label.ifBlank { name.trim() }
}

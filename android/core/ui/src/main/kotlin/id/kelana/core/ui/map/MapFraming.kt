package id.kelana.core.ui.map

import kotlin.math.max

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

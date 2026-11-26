package id.kelana.core.data.route

import id.kelana.core.model.RoutePoint

/** Decodes an encoded polyline with [precision] decimals (6 for Valhalla, 5 for the classic Google format). */
internal fun decodePolyline(encoded: String, precision: Int = 6): List<RoutePoint> {
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val out = ArrayList<RoutePoint>()
    var index = 0
    var lat = 0L
    var lng = 0L

    fun nextValue(): Long? {
        var result = 0L
        var shift = 0
        while (true) {
            if (index >= encoded.length) return null
            val b = encoded[index++].code - 63
            if (b < 0) return null
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
            if (b < 0x20) break
            if (shift > 60) return null
        }
        return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
    }

    while (index < encoded.length) {
        val dLat = nextValue() ?: break
        val dLng = nextValue() ?: break
        lat += dLat
        lng += dLng
        out += RoutePoint(lat / factor, lng / factor)
    }
    return out
}

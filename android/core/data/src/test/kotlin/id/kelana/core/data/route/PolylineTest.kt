package id.kelana.core.data.route

import id.kelana.core.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong

/** Test-only encoder, the inverse of [decodePolyline]. */
internal fun encodePolyline(points: List<RoutePoint>, precision: Int = 6): String {
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val sb = StringBuilder()
    var pLat = 0L
    var pLng = 0L
    fun put(delta: Long) {
        var v = if (delta < 0) (delta shl 1).inv() else delta shl 1
        while (v >= 0x20) {
            sb.append(((0x20 or (v and 0x1f).toInt()) + 63).toChar())
            v = v shr 5
        }
        sb.append((v.toInt() + 63).toChar())
    }
    for (p in points) {
        val lat = (p.lat * factor).roundToLong()
        val lng = (p.lng * factor).roundToLong()
        put(lat - pLat)
        put(lng - pLng)
        pLat = lat
        pLng = lng
    }
    return sb.toString()
}

class PolylineTest {
    @Test
    fun classicPolyline5Example() {
        val pts = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)
        assertEquals(3, pts.size)
        assertEquals(38.5, pts[0].lat, 1e-9)
        assertEquals(-120.2, pts[0].lng, 1e-9)
        assertEquals(40.7, pts[1].lat, 1e-9)
        assertEquals(-120.95, pts[1].lng, 1e-9)
        assertEquals(43.252, pts[2].lat, 1e-9)
        assertEquals(-126.453, pts[2].lng, 1e-9)
    }

    @Test
    fun classicExampleScaledToPolyline6() {
        val expected = listOf(RoutePoint(38.5, -120.2), RoutePoint(40.7, -120.95), RoutePoint(43.252, -126.453))
        val encoded = encodePolyline(expected, precision = 6)
        val decoded = decodePolyline(encoded, precision = 6)
        assertEquals(expected.size, decoded.size)
        expected.zip(decoded).forEach { (a, b) ->
            assertEquals(a.lat, b.lat, 1e-9)
            assertEquals(a.lng, b.lng, 1e-9)
        }
    }

    @Test
    fun roundTripAroundYogyakartaWithNegativeDeltas() {
        val pts = listOf(
            RoutePoint(-7.801234, 110.364567),
            RoutePoint(-7.805678, 110.361234),
            RoutePoint(-7.790001, 110.370002),
            RoutePoint(-7.790001, 110.370002),
        )
        val decoded = decodePolyline(encodePolyline(pts))
        assertEquals(pts.size, decoded.size)
        pts.zip(decoded).forEach { (a, b) ->
            assertEquals(a.lat, b.lat, 1e-9)
            assertEquals(a.lng, b.lng, 1e-9)
        }
    }

    @Test
    fun emptyAndTruncatedInputGiveNoCrash() {
        assertTrue(decodePolyline("").isEmpty())
        // A lone latitude without its longitude is dropped.
        assertTrue(decodePolyline("_p~iF").isEmpty())
    }
}

package id.kelana.core.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Framing with real place coordinates (supabase/data/provinsi_places.sql) on a 393 x 852 dp phone. */
class MapFitTest {
    private val phone = FramingViewport(
        widthDp = 393.0, heightDp = 852.0,
        padLeftDp = 24.0, padTopDp = 100.0, padRightDp = 24.0, padBottomDp = 150.0,
        pinHalfWidthDp = 38.0, pinAboveDp = 70.0, pinBelowDp = 24.0,
    )

    private fun assertAllInside(points: List<Pair<Double, Double>>, v: FramingViewport = phone) {
        val cam = fitCamera(points, v, 5.0, 18.0)
        assertNotNull(cam)
        for ((lat, lng) in points) {
            val (x, y) = projectToViewport(lat, lng, cam!!, v)
            val eps = 0.01
            assertTrue("x=$x left of ${v.padLeftDp + v.pinHalfWidthDp}", x >= v.padLeftDp + v.pinHalfWidthDp - eps)
            assertTrue("x=$x right of ${v.widthDp - v.padRightDp - v.pinHalfWidthDp}", x <= v.widthDp - v.padRightDp - v.pinHalfWidthDp + eps)
            assertTrue("y=$y above ${v.padTopDp + v.pinAboveDp}", y >= v.padTopDp + v.pinAboveDp - eps)
            assertTrue("y=$y below ${v.heightDp - v.padBottomDp - v.pinBelowDp}", y <= v.heightDp - v.padBottomDp - v.pinBelowDp + eps)
        }
    }

    @Test fun jawaTimurFitsAtAZoomBelowTheOldMinimumOfEight() {
        assertAllInside(JAWA_TIMUR)
        assertTrue(fitCamera(JAWA_TIMUR, phone, 5.0, 18.0)!!.zoom < 8.0)
    }

    @Test fun jawaTengahFits() {
        assertAllInside(JAWA_TENGAH)
        assertTrue(fitCamera(JAWA_TENGAH, phone, 5.0, 18.0)!!.zoom < 8.0)
    }

    @Test fun jakartaFits() = assertAllInside(JAKARTA)

    @Test fun bantenIsTallAndWideAndFits() = assertAllInside(BANTEN)

    @Test fun veryTallRegionIsLimitedByHeight() {
        val tall = listOf(-5.0 to 110.0, -9.0 to 110.05)
        assertAllInside(tall)
        val cam = fitCamera(tall, phone, 5.0, 18.0)!!
        val (_, y1) = projectToViewport(-5.0, 110.0, cam, phone)
        val (_, y2) = projectToViewport(-9.0, 110.05, cam, phone)
        assertEquals(phone.heightDp - phone.padBottomDp - phone.pinBelowDp, y2, 0.01)
        assertEquals(phone.padTopDp + phone.pinAboveDp, y1, 0.01)
    }

    @Test fun wideRegionIsLimitedByWidth() {
        val wide = listOf(-7.0 to 105.0, -7.1 to 109.5)
        assertAllInside(wide)
    }

    @Test fun singlePointUsesFixedZoomAndSitsInTheFreeArea() {
        val p = listOf(-7.8014 to 110.3647)
        val cam = fitCamera(p, phone, 5.0, 18.0)!!
        assertEquals(SINGLE_PLACE_ZOOM, cam.zoom, 1e-9)
        assertAllInside(p)
    }

    @Test fun identicalPointsBehaveLikeOne() {
        val p = listOf(-7.8 to 110.4, -7.8 to 110.4, -7.8 to 110.4)
        assertEquals(SINGLE_PLACE_ZOOM, fitCamera(p, phone, 5.0, 18.0)!!.zoom, 1e-9)
        assertAllInside(p)
    }

    @Test fun emptyIsNull() = assertNull(fitCamera(emptyList(), phone, 5.0, 18.0))

    @Test fun zoomIsClampedToTheMaximumForCloseClusters() {
        val close = listOf(-7.80000 to 110.40000, -7.80030 to 110.40030)
        assertEquals(18.0, fitCamera(close, phone, 5.0, 18.0)!!.zoom, 1e-9)
    }

    companion object {
    private val BANTEN = listOf(
        -6.0381641 to 106.1543798,
        -6.0359414 to 106.1541066,
        -6.0438351 to 106.1600022,
        -6.0312048 to 106.1506564,
        -6.0362376 to 106.1558862,
        -6.0301657 to 106.1492426,
        -6.0381864 to 106.1585941,
        -6.0706794 to 105.8852557,
        -6.5945617 to 106.225445,
        -6.3605216 to 106.2472601,
        -6.1786898 to 106.6296439,
        -6.1790564 to 106.6296064,
        -6.1688514 to 106.6378822,
        -6.1786597 to 106.6302705,
        -6.3572 to 105.8292,
        -6.8046678 to 106.4235804,
    )

    private val JAKARTA = listOf(
        -6.1761 to 106.8217,
        -6.172 to 106.819,
        -6.18624 to 106.83665,
        -6.1838167 to 106.8430499,
        -6.1702395 to 106.8310056,
        -6.1692046 to 106.8332373,
        -6.1267614 to 106.808347,
        -6.1215868 to 106.8102943,
        -6.0969265 to 106.9588827,
        -6.1262704 to 106.8044521,
        -6.1351548 to 106.813289,
        -6.1371853 to 106.8129483,
        -6.1342607 to 106.8144771,
        -6.138126 to 106.81779,
        -6.341667 to 106.823889,
        -6.2310834 to 106.8189045,
        -6.3026341 to 106.8952962,
        -6.3006158 to 106.8872775,
    )

    private val JAWA_TENGAH = listOf(
        -7.6064052 to 110.2045392,
        -7.578 to 110.8273,
        -7.56639 to 110.82306,
        -7.568661 to 110.8144172,
        -7.6273152 to 111.1315373,
        -7.595736 to 111.158131,
        -7.205028 to 109.906919,
        -6.983979 to 110.410791,
        -6.96723 to 110.4268983,
        -6.9958722 to 110.3983842,
        -6.9860374 to 110.3839468,
        -7.2063743 to 110.340176,
        -6.8946963 to 110.6373455,
        -6.5885648 to 110.667249,
        -6.878642 to 109.675579,
        -7.7491116 to 109.0172596,
        -7.5992992 to 109.5178918,
        -7.0029657 to 110.42442,
    )

    private val JAWA_TIMUR = listOf(
        -7.5600583 to 112.3805254,
        -7.5716315 to 112.4035216,
        -8.0164156 to 112.2094979,
        -8.0899991 to 112.1716269,
        -7.7464173 to 112.2180351,
        -7.7349613 to 112.1611779,
        -7.8877437 to 112.6639091,
        -8.0057952 to 112.7641342,
        -8.0258439 to 112.7091054,
        -7.6624998 to 112.6677564,
        -7.94039 to 112.62086,
        -7.433982 to 112.719922,
        -7.230813 to 112.734247,
        -7.2317656 to 112.7423278,
        -7.1688945 to 112.6310331,
        -6.8945877 to 112.0633076,
        -8.2208655 to 114.3681617,
        -7.0083434 to 113.8628657,
    )
    }
}

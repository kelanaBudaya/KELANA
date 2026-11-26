package id.kelana.core.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFramingTest {
    @Test fun boundsOfEmptyListIsNull() {
        assertNull(boundsOf(emptyList()))
    }

    @Test fun boundsCoverAllPlaces() {
        val b = boundsOf(listOf(-7.75 to 110.49, -7.83 to 110.36, -7.80 to 110.40))!!
        assertEquals(-7.75, b.north, 1e-9)
        assertEquals(-7.83, b.south, 1e-9)
        assertEquals(110.36, b.west, 1e-9)
        assertEquals(110.49, b.east, 1e-9)
        assertEquals(-7.79, b.centerLat, 1e-9)
        assertEquals(110.425, b.centerLng, 1e-9)
    }

    @Test fun singlePlaceIsAPointAndNeedsAFixedZoom() {
        assertTrue(boundsOf(listOf(-7.8 to 110.4))!!.isPoint())
        assertFalse(boundsOf(listOf(-7.75 to 110.49, -7.83 to 110.36))!!.isPoint())
    }

    @Test fun tileConfigIsOneOsmRasterTemplate() {
        assertTrue(TILE_URL_TEMPLATE.contains("{z}") && TILE_URL_TEMPLATE.contains("{x}") && TILE_URL_TEMPLATE.contains("{y}"))
        assertTrue(osmStyleJson().contains(TILE_URL_TEMPLATE))
        assertTrue(TILE_USER_AGENT.isNotBlank())
    }

    @Test fun pinLabelsAreShortened() {
        assertEquals("Vredeburg", pinLabel("Museum Benteng Vredeburg"))
        assertEquals("Prambanan", pinLabel("Candi Prambanan"))
        assertEquals("Pakualaman", pinLabel("Puro Pakualaman"))
        assertEquals("Keraton", pinLabel("Keraton Yogyakarta"))
        assertEquals("Tamansari", pinLabel("Tamansari"))
    }
}

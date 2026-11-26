package id.kelana.core.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

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

    @Test fun bundledStyleIsValidVectorStyleWithSpriteAndGlyphs() {
        val file = listOf(
            File("src/main/res/raw/map_style_muted.json"),
            File("core/ui/src/main/res/raw/map_style_muted.json"),
            File("android/core/ui/src/main/res/raw/map_style_muted.json"),
        ).firstOrNull { it.isFile }
        assertNotNull("map_style_muted.json not found from ${File(".").absolutePath}", file)
        val style = Json.parseToJsonElement(file!!.readText()).jsonObject
        assertEquals(8, style["version"]!!.jsonPrimitive.int)
        assertTrue(style["sprite"]!!.jsonPrimitive.content.startsWith("https://"))
        assertTrue(style["glyphs"]!!.jsonPrimitive.content.contains("{fontstack}"))
        val sources = style["sources"]!!.jsonObject
        assertTrue(sources.values.any { it.jsonObject["type"]!!.jsonPrimitive.content == "vector" })
        assertTrue(style["layers"]!!.jsonArray.isNotEmpty())
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

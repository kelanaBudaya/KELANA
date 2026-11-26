package id.kelana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceCategoryTest {
    @Test
    fun fromKey_resolvesEveryCurrentKey() {
        PlaceCategory.entries.forEach { assertEquals(it, PlaceCategory.fromKey(it.key)) }
        assertEquals("pentas", PlaceCategory.Pentas.key)
        assertEquals("seni_musik", PlaceCategory.SeniMusik.key)
    }

    @Test
    fun fromKey_mapsLegacyKeysWrittenByOlderAppVersions() {
        assertEquals(PlaceCategory.Pentas, PlaceCategory.fromKey("topeng"))
        assertEquals(PlaceCategory.SeniMusik, PlaceCategory.fromKey("gamelan"))
    }

    @Test
    fun fromKey_unknownKeyIsNull() {
        assertNull(PlaceCategory.fromKey("keraton"))
        assertNull(PlaceCategory.fromKey(""))
    }
}

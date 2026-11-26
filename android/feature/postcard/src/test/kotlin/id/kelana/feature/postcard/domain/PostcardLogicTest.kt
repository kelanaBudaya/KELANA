package id.kelana.feature.postcard.domain

import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Stamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PostcardLogicTest {
    private fun place(id: String) = Place(id, id, "Tempat $id", PlaceCategory.Museum, "YK-0", 0.0, 0.0, "", "", id)

    private val t0 = Instant.parse("2026-11-20T03:00:00Z")

    @Test fun stamped_places_keep_passport_order_and_the_earliest_stamp() {
        val places = listOf(place("a"), place("b"), place("c"))
        val stamps = listOf(Stamp("C", t0), Stamp("a", t0.plusSeconds(100)), Stamp("a", t0), Stamp("zzz", t0))
        val result = stampedPlaces(places, stamps)
        assertEquals(listOf("a", "c"), result.map { it.place.id })
        assertEquals(t0, result[0].stampedAt)
    }

    @Test fun message_rules() {
        assertFalse(isMessageValid(""))
        assertFalse(isMessageValid("  \n "))
        assertTrue(isMessageValid("a"))
        assertTrue(isMessageValid("x".repeat(500)))
        assertFalse(isMessageValid("x".repeat(501)))
        assertTrue(isMessageValid("  " + "x".repeat(500) + "  ")) // counted after trimming
    }

    @Test fun dates_come_from_the_stamp_in_id_format() {
        val wib = ZoneId.of("Asia/Jakarta")
        assertEquals("20 NOV 2026", formatVisitedDate(t0, wib))
        assertEquals("20.11.26", formatStampDate(t0, wib))
        // 23:00 UTC on the 20th is already the 21st in Yogyakarta.
        assertEquals("21 NOV 2026", formatVisitedDate(Instant.parse("2026-11-20T23:00:00Z"), wib))
    }

    private fun card(name: String, code: String, short: String) = CardContent(
        placeName = "Tempat", capCode = "X-01", message = "m", rating = null, style = id.kelana.core.model.PostcardStyle.Jurnal,
        stampedAt = null, photoJpeg = null, regionName = name, regionCode = code, regionShortName = short,
    )

    @Test fun card_region_texts_come_from_the_region() {
        val c = card("Jawa Tengah", "JTG", "Jateng")
        assertEquals("JAWA TENGAH", c.regionLabel)
        assertEquals("JTG", c.regionCode)
        assertEquals("Jateng", c.regionShortName)
        assertEquals("DI YOGYAKARTA", card("DI Yogyakarta", "DIY", "Jogja").regionLabel)
    }

    @Test fun fitting_keeps_the_base_size_when_it_fits_and_shrinks_in_proportion_otherwise() {
        assertEquals(10f, fitFontSize(10f, 80f, 100f, 6f), 0f)
        assertEquals(8f, fitFontSize(10f, 125f, 100f, 6f), 0.001f)
        assertEquals(6f, fitFontSize(10f, 400f, 100f, 6f), 0f) // never below the minimum
    }
}

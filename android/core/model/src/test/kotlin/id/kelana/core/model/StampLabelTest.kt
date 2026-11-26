package id.kelana.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StampLabelTest {
    private fun place(stampName: String?) = Place(
        id = "p", slug = "p", name = "Candi Prambanan", category = PlaceCategory.Candi, capCode = "YK-10",
        lat = 0.0, lng = 0.0, address = "", description = "", coverKey = "p", stampName = stampName,
    )

    @Test
    fun stampLabel_usesTheNameWhenPresent() {
        assertEquals("Penjaga Ramayana", place("Penjaga Ramayana").stampLabel)
    }

    @Test
    fun stampLabel_fallsBackToTheCodeWhenNull() {
        assertEquals("YK-10", place(null).stampLabel)
    }

    @Test
    fun stampLabel_fallsBackToTheCodeWhenBlank() {
        assertEquals("YK-10", place("   ").stampLabel)
        assertEquals("YK-10", place("").stampLabel)
    }

    @Test
    fun stampNameDefaultsToNull() {
        val p = Place("p", "p", "n", PlaceCategory.Museum, "YK-01", 0.0, 0.0, "", "", "p")
        assertEquals(null, p.stampName)
    }

    @Test
    fun planStop_stampLabelFollowsTheSameRule() {
        val stop = PlanStop(
            placeId = "p", placeName = "n", capCode = "YK-10", category = PlaceCategory.Candi, lat = 0.0, lng = 0.0,
            start = java.time.LocalTime.of(9, 0), end = java.time.LocalTime.of(10, 0),
        )
        assertEquals("YK-10", stop.stampLabel)
        assertEquals("Penjaga Ramayana", stop.copy(stampName = "Penjaga Ramayana").stampLabel)
    }
}

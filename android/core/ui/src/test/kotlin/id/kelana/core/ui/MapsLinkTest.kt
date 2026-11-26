package id.kelana.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsLinkTest {
    @Test
    fun `geo uri keeps the southern latitude sign and encodes the label`() {
        assertEquals(
            "geo:-7.801937,110.364425?q=-7.801937,110.364425(Museum%20Negeri%20Sonobudoyo)",
            googleMapsGeoUri(-7.801937, 110.364425, "Museum Negeri Sonobudoyo"),
        )
    }

    @Test
    fun `label with parentheses and ampersand is escaped`() {
        assertEquals(
            "geo:-6.200000,106.800000?q=-6.200000,106.800000(Kota%20Tua%20%26%20Museum%20%28Jakarta%29)",
            googleMapsGeoUri(-6.2, 106.8, "Kota Tua & Museum (Jakarta)"),
        )
    }

    @Test
    fun `web uri uses the documented search url`() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=-7.801937,110.364425",
            googleMapsWebUri(-7.801937, 110.364425),
        )
    }
}

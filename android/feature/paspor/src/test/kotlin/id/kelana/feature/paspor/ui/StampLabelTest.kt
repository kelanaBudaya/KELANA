package id.kelana.feature.paspor.ui

import java.util.Locale
import org.junit.Test
import org.junit.Assert.assertEquals

class StampLabelTest {
    private val id = Locale.forLanguageTag("id-ID")

    @Test
    fun dropsLeadingGenericWord() {
        assertEquals("SONOBUDOYO", stampLabel("Museum Sonobudoyo", id))
        assertEquals("PRAMBANAN", stampLabel("Candi Prambanan", id))
    }

    @Test
    fun keepsOtherNamesWhole() {
        assertEquals("TAMAN SARI", stampLabel("Taman Sari", id))
        assertEquals("KOTAGEDE", stampLabel("Kotagede", id))
        assertEquals("MUSEUM", stampLabel("Museum", id))
    }
}

package id.kelana.core.designsystem.component

import id.kelana.core.designsystem.theme.KelanaColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class OfficialFactRowTest {
    @Test
    fun valueIsHiddenWithoutCheckDate() {
        assertNull(visibleFactValue("Rp10.000", null))
    }

    @Test
    fun valueIsShownWithCheckDate() {
        assertEquals("Rp10.000", visibleFactValue("Rp10.000", LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun missingValueStaysNull() {
        assertNull(visibleFactValue(null, LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun everyBannerToneHasItsOwnBackground() {
        val colors = KelanaColors()
        val backgrounds = BannerTone.entries.map { bannerColors(it, colors).first }
        assertEquals(backgrounds.size, backgrounds.toSet().size)
        assertNotEquals(bannerColors(BannerTone.Info, colors).first, bannerColors(BannerTone.Info, colors).second)
    }
}

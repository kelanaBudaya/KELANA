package id.kelana.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class KelanaColorsTest {
    private val colors = KelanaColors()

    @Test
    fun confirmedFigmaTokensMatchTheDesignNotes() {
        assertEquals(Color(0xFFFFFCF6), colors.background)
        assertEquals(Color(0xFF24458F), colors.primary)
        assertEquals(Color(0xFFC94D1A), colors.accentOrange)
        assertEquals(Color(0xFFF4B52B), colors.stampYellow)
        assertEquals(Color(0xFF8C93A0), colors.iconMuted)
    }

    @Test
    fun artifactTokensMatchTheScreenFiles() {
        assertEquals(Color(0xFFFCE1D2), colors.peach)
        assertEquals(Color(0xFFD5DBE6), colors.divider)
        assertEquals(Color(0xFFE7E0D2), colors.outline)
        assertEquals(Color(0xFF14213D), colors.galleryDark)
        assertEquals(Color(0xFF243047), colors.ink)
        assertEquals(Color(0xFF566072), colors.muted)
        assertEquals(Color(0xFF1B3671), colors.navy)
        assertEquals(Color(0xFFDCE6F7), colors.skyText)
    }

    @Test
    fun markerColoursMatchTheIsiBox() {
        assertEquals(Color(0xFFFFF6DC), colors.markerBackground)
        assertEquals(Color(0xFFC9A14A), colors.markerBorder)
        assertEquals(Color(0xFF8A5A00), colors.markerText)
    }

    @Test
    fun activeTabPillMatchesSkyText() {
        assertEquals(colors.skyText, colors.tabActivePill)
    }
}

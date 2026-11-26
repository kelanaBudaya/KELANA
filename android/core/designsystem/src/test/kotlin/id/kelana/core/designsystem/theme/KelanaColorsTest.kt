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
    fun phase9SemanticTokensPinTheOldAdHocValues() {
        assertEquals(Color(0xFFB84439), colors.dangerStrong)
        assertEquals(Color(0xFFFBE7E5), colors.dangerSurface)
        assertEquals(Color(0xFFE5B9B4), colors.dangerDisabled)
        assertEquals(Color(0xFFFFE6D6), colors.peachSoft)
        assertEquals(Color(0xFFEEF0F3), colors.badgeClosedBackground)
        assertEquals(Color(0xFFB9B09E), colors.dashWarm)
        assertEquals(Color(0xFFB9C4DA), colors.dashMuted)
        assertEquals(Color(0xFF24345A), colors.galleryPanel)
        assertEquals(Color(0xFF3A4A70), colors.galleryTileBorder)
        assertEquals(Color(0xFFDCEAF7), colors.mapBackdrop)
    }

    @Test
    fun activeTabPillMatchesSkyText() {
        assertEquals(colors.skyText, colors.tabActivePill)
    }
}

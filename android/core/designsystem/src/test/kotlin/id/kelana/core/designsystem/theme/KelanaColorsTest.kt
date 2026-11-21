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
    fun activeTabPillMatchesSkyText() {
        assertEquals(colors.skyText, colors.tabActivePill)
    }
}

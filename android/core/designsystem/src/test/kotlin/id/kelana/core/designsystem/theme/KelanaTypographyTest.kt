package id.kelana.core.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class KelanaTypographyTest {
    @Test
    fun titleAndBodyMatchTheArtifact() {
        val t = KelanaTypography
        assertEquals(28.sp, t.headlineLarge.fontSize)
        assertEquals(34.sp, t.headlineLarge.lineHeight)
        assertEquals(FontWeight.ExtraBold, t.headlineLarge.fontWeight)
        assertEquals((-0.4).sp, t.headlineLarge.letterSpacing)
        assertEquals(16.sp, t.bodyLarge.fontSize)
        assertEquals(24.sp, t.bodyLarge.lineHeight)
        assertEquals(16.sp, t.labelLarge.fontSize)
        assertEquals(FontWeight.ExtraBold, t.labelLarge.fontWeight)
        assertEquals(12.sp, Caption.fontSize)
        assertEquals(18.sp, Caption.lineHeight)
    }
}

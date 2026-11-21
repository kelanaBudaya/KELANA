package id.kelana.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class KelanaDimensTest {
    @Test
    fun radiiMatchTheArtifact() {
        assertEquals(4.dp, KelanaRadii.badge)
        assertEquals(12.dp, KelanaRadii.input)
        assertEquals(16.dp, KelanaRadii.card)
        assertEquals(20.dp, KelanaRadii.listCard)
        assertEquals(32.dp, KelanaRadii.imagePanel)
        assertEquals(999.dp, KelanaRadii.pill)
    }

    @Test
    fun heightsMatchTheArtifact() {
        assertEquals(52.dp, KelanaSizes.buttonHeight)
        assertEquals(40.dp, KelanaSizes.chipHeight)
        assertEquals(44.dp, KelanaSizes.inputHeight)
        assertEquals(84.dp, KelanaSizes.bottomBarHeight)
        assertEquals(58.dp, KelanaSizes.tabPillWidth)
        assertEquals(32.dp, KelanaSizes.tabPillHeight)
    }

    @Test
    fun guttersMatchTheArtifact() {
        assertEquals(16.dp, KelanaSpacing.gutter)
        assertEquals(20.dp, KelanaSpacing.gutterWide)
        assertEquals(24.dp, KelanaSpacing.gutterText)
    }
}

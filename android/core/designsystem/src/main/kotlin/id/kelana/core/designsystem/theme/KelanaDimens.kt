package id.kelana.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object KelanaSpacing {
    /** Side gutter of every screen. */
    val gutter: Dp = 16.dp

    // PROVISIONAL: confirm against Figma
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 24.dp
}

object KelanaSizes {
    /** Card width in the 390 dp frames. */
    val cardWidth: Dp = 358.dp
    val bottomBarHeight: Dp = 84.dp

    // PROVISIONAL: confirm against Figma
    val buttonHeight: Dp = 48.dp
    val topBarHeight: Dp = 56.dp
}

object KelanaShapes {
    // PROVISIONAL: confirm against Figma
    val card = RoundedCornerShape(16.dp)

    // PROVISIONAL: confirm against Figma
    val button = RoundedCornerShape(12.dp)

    // PROVISIONAL: confirm against Figma
    val chip = RoundedCornerShape(50)
}

val KelanaMaterialShapes = Shapes(
    small = KelanaShapes.chip,
    medium = KelanaShapes.button,
    large = KelanaShapes.card,
)

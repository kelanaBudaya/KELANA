package id.kelana.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from docs/design/FIGMA-FRAMES.md.
 * All values are read from Figma fills except those marked PROVISIONAL.
 */
@Immutable
data class KelanaColors(
    // Text and base
    val ink: Color = Color(0xFF243047),
    val muted: Color = Color(0xFF566072),
    val navy: Color = Color(0xFF1B3671),
    val skyText: Color = Color(0xFFDCE6F7),
    val white: Color = Color(0xFFFFFFFF),

    // Surfaces
    val surface: Color = Color(0xFFFFFFFF),
    val background: Color = Color(0xFFFFFCF6),
    val backgroundWarm: Color = Color(0xFFF8F3E8),

    // Blues
    val primary: Color = Color(0xFF24458F),
    val primaryDeep: Color = Color(0xFF133476),
    val primaryDeepAlt: Color = Color(0xFF1A3885),
    val primaryBright: Color = Color(0xFF1D499A),
    val primaryBrightAlt: Color = Color(0xFF2D52A9),

    // Orange, stamp and yellow
    val accentOrange: Color = Color(0xFFC94D1A),
    val accentOrangeBright: Color = Color(0xFFF5831B),
    val accentOrangeBrightAlt: Color = Color(0xFFE5591A),
    val stampBrown: Color = Color(0xFFA84E2B),
    val stampYellow: Color = Color(0xFFF4B52B),
    val stampYellowAlt: Color = Color(0xFFFDB42C),
    val creamYellow: Color = Color(0xFFFFF0C8),
    val creamYellowAlt: Color = Color(0xFFFFF6DC),
    val warnText: Color = Color(0xFF8A5A00),
    val warnTextDeep: Color = Color(0xFF5C3D00),

    // Success
    val success: Color = Color(0xFF468A5E),
    val successText: Color = Color(0xFF35745A),
    val successTextDeep: Color = Color(0xFF1F4D3A),
    val successSurface: Color = Color(0xFFE3F1EA),

    // Icons and bottom bar
    val iconMuted: Color = Color(0xFF8C93A0),
    /** Pill behind the active tab icon, about 64 x 32 dp. Approximate (~#DCE6F7), same as [skyText]. */
    val tabActivePill: Color = Color(0xFFDCE6F7),

    // PROVISIONAL: confirm against Figma (danger red is not readable from fills)
    val danger: Color = Color(0xFFC0392B),
    // PROVISIONAL: confirm against Figma (hairline for cards, outlines and the bar divider)
    val outline: Color = Color(0xFFD9DEE8),
)

val LocalKelanaColors = staticCompositionLocalOf { KelanaColors() }

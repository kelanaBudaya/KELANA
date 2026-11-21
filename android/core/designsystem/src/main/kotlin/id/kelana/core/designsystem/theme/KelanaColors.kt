package id.kelana.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens, verified against the Claude Design artifact (docs/design/artifact/README.md and the screen files
 * JelajahIlustratif, Kenalan1, MasukIlustratif, DetailIlustratif, KatMuseum, Galeri). Values not seen in those files
 * are marked PROVISIONAL. Illustration-only colours (primaryDeep, primaryBright, ...) come from the inline SVG art.
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
    /** Peach tile and panel background (KatMuseum, Jelajah category tiles). */
    val peach: Color = Color(0xFFFCE1D2),
    /** Dark gallery background (Galeri.dc.html). */
    val galleryDark: Color = Color(0xFF14213D),
    /** Deep ink used on dark panels (KatMuseum, Jelajah). */
    val inkDeep: Color = Color(0xFF1B2A4A),

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
    /** Pill behind the active tab icon, 58 x 32 dp pill filled `#DCE6F7` (KatMuseum bottom nav), same as [skyText]. */
    val tabActivePill: Color = Color(0xFFDCE6F7),

    // PROVISIONAL: no red appears in the five reference files
    val danger: Color = Color(0xFFC0392B),
    /** Hairline of cards, chips, outline buttons and the bottom bar top border (`#E7E0D2` in every screen file). */
    val outline: Color = Color(0xFFE7E0D2),
    /** Divider and inactive pager dot (`#D5DBE6`, Kenalan1 and Jelajah). */
    val divider: Color = Color(0xFFD5DBE6),

    // [ISI] marker colours: for previews only, never shown in production UI (the app shows "sedang dicek").
    val markerBackground: Color = Color(0xFFFFF6DC),
    val markerBorder: Color = Color(0xFFC9A14A),
    val markerText: Color = Color(0xFF8A5A00),
)

val LocalKelanaColors = staticCompositionLocalOf { KelanaColors() }

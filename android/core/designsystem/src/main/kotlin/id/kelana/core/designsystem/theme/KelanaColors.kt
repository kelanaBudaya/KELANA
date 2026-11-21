package id.kelana.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colour tokens from docs/design/FIGMA-FRAMES.md. */
@Immutable
data class KelanaColors(
    // Confirmed from Figma node styles.
    val ink: Color = Color(0xFF243047),
    val muted: Color = Color(0xFF566072),
    val navy: Color = Color(0xFF1B3671),
    val skyText: Color = Color(0xFFDCE6F7),
    val white: Color = Color(0xFFFFFFFF),
    // PROVISIONAL: confirm against Figma
    val primaryBlue: Color = Color(0xFF23458F),
    // PROVISIONAL: confirm against Figma
    val deepNavy: Color = Color(0xFF1E3A78),
    // PROVISIONAL: confirm against Figma
    val orange: Color = Color(0xFFC4501B),
    // PROVISIONAL: confirm against Figma
    val stampYellow: Color = Color(0xFFF4B528),
    // PROVISIONAL: confirm against Figma
    val cream: Color = Color(0xFFFBF7EE),
    // PROVISIONAL: confirm against Figma
    val success: Color = Color(0xFF2E8B57),
    // PROVISIONAL: confirm against Figma (hex not in the design file yet)
    val warning: Color = Color(0xFFE0A526),
    // PROVISIONAL: confirm against Figma (hex not in the design file yet)
    val danger: Color = Color(0xFFC0392B),
    // PROVISIONAL: confirm against Figma (hairline for cards and outlines)
    val outline: Color = Color(0xFFD9DEE8),
)

val LocalKelanaColors = staticCompositionLocalOf { KelanaColors() }

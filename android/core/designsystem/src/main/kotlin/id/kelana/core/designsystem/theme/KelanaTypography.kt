package id.kelana.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plusjakartasans_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/**
 * Plus Jakarta Sans (OFL), variable font with a wght axis (200..800), used at 400, 500, 600, 700 and 800 for all UI.
 * Licence: core/designsystem/PLUSJAKARTASANS-LICENSE-OFL.txt.
 */
val JakartaFontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

/**
 * Fraunces (OFL), static 72pt optical size: Regular, Bold and Italic.
 * Only for the postcard and highlight headlines, never for UI text.
 * Licence: core/designsystem/FRAUNCES-LICENSE-OFL.txt.
 */
val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_bold, FontWeight.Bold),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun ui(size: Int, line: Int, weight: FontWeight, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Sizes, weights and line heights taken from JelajahIlustratif, Kenalan1, MasukIlustratif, DetailIlustratif and
 * KatMuseum (docs/design/artifact). Mapping to Material roles:
 *
 * - headlineLarge  28/34 w800, letter-spacing -0.4: screen title (Kenalan1 h1, KatMuseum h1 28/32)
 * - headlineMedium 26/32 w800, letter-spacing -0.4: Detail h1, Masuk h1 (26/32)
 * - headlineSmall  22/28 w800: PROVISIONAL weight and line height (22 is only seen in Fraunces)
 * - titleLarge     18/24 w800: section heading (Detail h2 18/800); line height PROVISIONAL
 * - titleMedium    16/21 w800: card title (KatMuseum 16/800/21), also top bar and empty state titles
 * - titleSmall     14/20 w700: small link or label (Lewati 14/700); line height PROVISIONAL
 * - bodyLarge      16/24 w400: body (Kenalan1)
 * - bodyMedium     14/21 w400: 14 text with 1.5 line height
 * - bodySmall      13/18 w400: subtitle and caption under a title (KatMuseum 13/18)
 * - labelLarge     16/20 w800: button label (52 dp button 16/800); line height PROVISIONAL
 * - labelMedium    12/16 w600: badge (12/600/16)
 * - labelSmall     11/13 w700: small tile caption (11/700/13)
 *
 * Extra styles below: [Caption], [ChipLabel], [TabLabelActive], [TabLabelInactive], [SerifHeadline], [SerifItalic].
 */
val KelanaTypography = Typography(
    headlineLarge = ui(28, 34, FontWeight.ExtraBold, -0.4f),
    headlineMedium = ui(26, 32, FontWeight.ExtraBold, -0.4f),
    headlineSmall = ui(22, 28, FontWeight.ExtraBold),
    titleLarge = ui(18, 24, FontWeight.ExtraBold),
    titleMedium = ui(16, 21, FontWeight.ExtraBold),
    titleSmall = ui(14, 20, FontWeight.Bold),
    bodyLarge = ui(16, 24, FontWeight.Normal),
    bodyMedium = ui(14, 21, FontWeight.Normal),
    bodySmall = ui(13, 18, FontWeight.Normal),
    labelLarge = ui(16, 20, FontWeight.ExtraBold),
    labelMedium = ui(12, 16, FontWeight.SemiBold),
    labelSmall = ui(11, 13, FontWeight.Bold),
)

/** Caption 12/18 w400. */
val Caption = ui(12, 18, FontWeight.Normal)

/** Chip label 13/16 w700 (KatMuseum filter chips). Line height PROVISIONAL. */
val ChipLabel = ui(13, 16, FontWeight.Bold)

/** Serif highlight headline, Fraunces 22/25 (Jelajah; the artifact uses w600, the static Bold file is the closest). */
val SerifHeadline = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 25.sp)

/** Italic serif accent line (Fraunces Italic 13/17, DetailIlustratif). */
val SerifItalic = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontStyle = FontStyle.Italic,
    fontSize = 13.sp,
    lineHeight = 17.sp,
)

/** Active bottom bar label, 12 w700 (KatMuseum nav). */
val TabLabelActive = ui(12, 16, FontWeight.Bold)

/** Inactive bottom bar label, 12 w500 (KatMuseum nav). */
val TabLabelInactive = ui(12, 16, FontWeight.Medium)

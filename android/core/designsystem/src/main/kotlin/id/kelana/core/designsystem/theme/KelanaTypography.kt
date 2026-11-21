package id.kelana.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.R

/** Inter (OFL), Regular 400 and Bold 700, for UI. Licence: core/designsystem/INTER-LICENSE-OFL.txt. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Fraunces (OFL), static 72pt optical size: Regular, Bold and Italic.
 * For headlines, titles on dark screens, the postcard and the KELANA wordmark.
 * Licence: core/designsystem/FRAUNCES-LICENSE-OFL.txt.
 */
val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_bold, FontWeight.Bold),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * Text sizes in use in Figma: 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 22, 26, 30.
 * Line heights and which size maps to which role are PROVISIONAL: confirm against Figma.
 */
val KelanaTypography = Typography(
    // Serif headlines (Fraunces)
    headlineLarge = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    // header value, 17 Bold
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    // card title, 14 Bold
    titleSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // body small, caption and tab label, 12
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    // button label, 11
    labelSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Italic serif accent line (Fraunces Italic), for postcard and quote text. */
val SerifItalic = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontStyle = FontStyle.Italic,
    fontSize = 18.sp,
    lineHeight = 26.sp,
)

/** Bold 12 tab label for the active bottom bar item. */
val TabLabelActive = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)

/** Regular 12 tab label for inactive bottom bar items. */
val TabLabelInactive = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)

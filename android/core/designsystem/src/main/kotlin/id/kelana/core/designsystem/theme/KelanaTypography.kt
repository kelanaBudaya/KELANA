package id.kelana.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.R

/** Inter (OFL), Regular 400 and Bold 700. Licence: core/designsystem/INTER-LICENSE-OFL.txt. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Sizes seen in Figma: 9, 11, 12 (caption, tab), 14 (card title), 17 (header value).
 * Line heights are PROVISIONAL: confirm against Figma. The serif headline face is not identified yet.
 */
val KelanaTypography = Typography(
    // header value, 17 Bold
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    // card title, 14 Bold
    titleSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // caption and tab label, 12
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Bold 12 tab label for the active bottom bar item. */
val TabLabelActive = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)

/** Regular 12 tab label for inactive bottom bar items. */
val TabLabelInactive = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)

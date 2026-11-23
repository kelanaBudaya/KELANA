package id.kelana.feature.postcard.ui.card

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.ShareFormat
import id.kelana.feature.postcard.domain.formatStampDate
import id.kelana.feature.postcard.domain.formatVisitedDate

/** Localised, formatted texts of the card. The stamp date is the date of the stamp, never today's date. */
internal fun cardTexts(context: Context, content: CardContent): CardTexts {
    val styleName = context.getString(
        when (content.style) {
            PostcardStyle.Jurnal -> R.string.postcard_card_style_jurnal
            PostcardStyle.Cap -> R.string.postcard_card_style_cap
            PostcardStyle.Peta -> R.string.postcard_card_style_peta
        },
    )
    return CardTexts(
        kicker = context.getString(R.string.postcard_card_kicker, styleName),
        region = context.getString(R.string.postcard_card_region),
        regionCode = context.getString(R.string.postcard_card_region_code),
        visitedBadge = content.stampedAt?.let {
            context.getString(R.string.postcard_card_visited, formatVisitedDate(it))
        },
        stampDate = content.stampedAt?.let { formatStampDate(it) },
        headline = context.getString(R.string.postcard_card_headline),
        quote = context.getString(R.string.postcard_card_quote, content.message.trim()),
        tagline = context.getString(R.string.postcard_card_tagline),
    )
}

/** Text measurer with 1 sp = 1 px, so the card can be drawn in design units and scaled afterwards. */
internal fun cardTextMeasurer(context: Context): TextMeasurer =
    TextMeasurer(createFontFamilyResolver(context), Density(1f, 1f), LayoutDirection.Ltr)

internal fun decodeCardPhoto(jpeg: ByteArray?): ImageBitmap? =
    jpeg?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }

/**
 * The card as the user will share it, drawn by [drawPostcardCard] (the same drawing the export uses).
 * 270 dp wide like the artifact (270 x 480 for Story; the 1:1 format is 270 x 270).
 */
@Composable
internal fun PostcardCardView(content: CardContent, format: ShareFormat, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val measurer = remember(context) { cardTextMeasurer(context) }
    val texts = remember(context, content) { cardTexts(context, content) }
    val photo = remember(content) { decodeCardPhoto(content.photoJpeg) }
    val description = context.getString(R.string.postcard_card_description, content.placeName)
    Canvas(
        modifier = modifier
            .width(270.dp)
            .aspectRatio(CARD_W / format.designHeight())
            .semantics { contentDescription = description },
    ) {
        drawPostcardCardFitWidth(content, texts, photo, measurer, format)
    }
}

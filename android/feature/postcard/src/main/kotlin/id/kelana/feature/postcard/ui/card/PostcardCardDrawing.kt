package id.kelana.feature.postcard.ui.card

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.JakartaFontFamily
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.ShareFormat
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Texts of the card, already localised and formatted (see [cardTexts]). A null date means the stamp date is unknown: the date is hidden. */
internal class CardTexts(
    val kicker: String,
    val region: String,
    val regionCode: String,
    val visitedBadge: String?,
    val stampDate: String?,
    val headline: String,
    val quote: String,
    val tagline: String,
)

/** Card width in design units. The artifact card is 270 x 480 px; the export is this drawing scaled by 4 (1080 x 1920). */
internal const val CARD_W = 270f
internal const val CARD_STORY_H = 480f
internal const val CARD_FEED_H = 270f
private const val FOOTER_H = 46f

internal fun ShareFormat.designHeight(): Float = if (this == ShareFormat.Story) CARD_STORY_H else CARD_FEED_H

private val Colors = KelanaColors()

/** Colours of the code-drawn postcard art (wave band behind the photo). Art, not UI: no semantic token. */
internal object PostcardWaveArt {
    /** Lighter navy wave of the Jurnal style, `#2B4C93`. */
    val waveJurnal = Color(0xFF2B4C93)

    /** Lighter yellow wave of the Cap style, `#F8CB62`. */
    val waveCap = Color(0xFFF8CB62)
}

/** Colours per card style. Jurnal is the artifact (Postcard.dc.html); Cap and Peta have no artifact yet: PROVISIONAL. */
private class Palette(
    val background: Color,
    val wave: Color,
    val kickerLeft: Color,
    val kickerRight: Color,
    val stampInk: Color,
    val stickerFill: Color,
    val stickerText: Color,
    val polaroid: Color,
    val stampScale: Float,
)

private fun paletteOf(style: PostcardStyle): Palette = when (style) {
    PostcardStyle.Jurnal -> Palette(
        background = Colors.navy, wave = PostcardWaveArt.waveJurnal, kickerLeft = Colors.stampYellow, kickerRight = Colors.white,
        stampInk = Colors.stampYellow, stickerFill = Colors.stampYellow, stickerText = Colors.navy,
        polaroid = Colors.backgroundWarm, stampScale = 1f,
    )
    PostcardStyle.Cap -> Palette(
        background = Colors.stampYellow, wave = PostcardWaveArt.waveCap, kickerLeft = Colors.navy, kickerRight = Colors.navy,
        stampInk = Colors.navy, stickerFill = Colors.navy, stickerText = Colors.white,
        polaroid = Colors.backgroundWarm, stampScale = 1.4f,
    )
    PostcardStyle.Peta -> Palette(
        background = Colors.skyText, wave = Colors.white, kickerLeft = Colors.primary, kickerRight = Colors.navy,
        stampInk = Colors.primary, stickerFill = Colors.primary, stickerText = Colors.white,
        polaroid = Colors.white, stampScale = 1f,
    )
}

private fun jakarta(size: Float, weight: FontWeight, color: Color, line: Float? = null, spacing: Float = 0f) = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (line ?: (size * 1.25f)).sp,
    letterSpacing = spacing.sp,
    color = color,
)

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

private fun TextMeasurer.layout(text: String, style: TextStyle, width: Float, maxLines: Int = 1): TextLayoutResult =
    measure(
        text = text,
        style = style,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
        maxLines = maxLines,
        constraints = Constraints(maxWidth = width.roundToInt()),
    )

private fun TextLayoutResult.textWidth(): Float {
    var w = 0f
    for (i in 0 until lineCount) w = max(w, getLineRight(i) - getLineLeft(i))
    return w
}

/**
 * Draws the card in design units (width [CARD_W], height [ShareFormat.designHeight]). Callers scale the scope:
 * the screen preview and the 1080 px export call the same function, so they cannot drift apart.
 * [photo] is cover-cropped into the polaroid; with no photo the panel stays plain sky blue.
 * The 1:1 format reuses the story composition, scaled to fit above a full-width footer (PROVISIONAL: no artifact for it).
 */
internal fun DrawScope.drawPostcardCard(
    content: CardContent,
    texts: CardTexts,
    photo: ImageBitmap?,
    measurer: TextMeasurer,
    format: ShareFormat,
) {
    val height = format.designHeight()
    val palette = paletteOf(content.style)
    drawRect(palette.background, size = Size(CARD_W, height))

    withTransform({ scale(1f, height / CARD_STORY_H, Offset.Zero) }) {
        drawPath(
            path(PostcardArt.WAVE),
            palette.wave,
            style = Stroke(width = 22f, cap = StrokeCap.Round),
        )
    }

    val bodyHeight = CARD_STORY_H - FOOTER_H
    if (format == ShareFormat.Story) {
        drawBody(content, texts, photo, measurer, palette)
    } else {
        val s = (height - FOOTER_H) / bodyHeight
        withTransform({
            translate(left = (CARD_W - CARD_W * s) / 2f, top = 0f)
            scale(s, s, Offset.Zero)
        }) { drawBody(content, texts, photo, measurer, palette) }
    }
    drawFooter(texts, measurer, top = height - FOOTER_H)
}

/** [drawPostcardCard] scaled so the design width fills the scope's width. */
internal fun DrawScope.drawPostcardCardFitWidth(
    content: CardContent,
    texts: CardTexts,
    photo: ImageBitmap?,
    measurer: TextMeasurer,
    format: ShareFormat,
) {
    val s = size.width / CARD_W
    withTransform({ scale(s, s, Offset.Zero) }) { drawPostcardCard(content, texts, photo, measurer, format) }
}

private fun DrawScope.drawBody(
    content: CardContent,
    texts: CardTexts,
    photo: ImageBitmap?,
    measurer: TextMeasurer,
    palette: Palette,
) {
    // Header: "KELANA / JURNAL" left, region right.
    val kicker = measurer.layout(texts.kicker, jakarta(10f, FontWeight.ExtraBold, palette.kickerLeft, spacing = 0.8f), 130f)
    val region = measurer.layout(texts.region, jakarta(10f, FontWeight.ExtraBold, palette.kickerRight, spacing = 0.8f), 100f)
    drawText(kicker, topLeft = Offset(16f, 16f))
    drawText(region, topLeft = Offset(CARD_W - 16f - region.textWidth(), 16f))

    // Polaroid: 214 wide at (26, 60), rotated -3 degrees. Inner width 194.
    val px = 26f
    val py = 60f
    val pw = 214f
    val inner = 194f
    val photoH = 168f
    val headline = measurer.layout(
        texts.headline,
        TextStyle(
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 26.sp,
            color = Colors.stampBrown,
        ),
        inner,
    )
    val starSize = 12f
    val stars = content.rating ?: 0
    val starsWidth = if (stars > 0) stars * starSize + (stars - 1) * 2f + 4f else 0f
    val name = measurer.layout(content.placeName, jakarta(11f, FontWeight.Bold, Colors.ink, line = 14f), inner - starsWidth)
    val quote = measurer.layout(
        texts.quote,
        TextStyle(
            fontFamily = FrauncesFontFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Colors.ink,
        ),
        inner,
        maxLines = 3,
    )
    val ph = 10f + photoH + 10f + 26f + 6f + 14f + 4f + quote.size.height + 12f

    rotate(-3f, Offset(px + pw / 2f, py + ph / 2f)) {
        // Soft shadow approximated with stacked translucent rounded rects (0 4px 10px rgba(0,0,0,.25) in the artifact).
        for (i in 0..3) {
            val grow = i * 1.6f
            drawRoundRect(
                Color.Black.copy(alpha = 0.07f),
                topLeft = Offset(px - grow, py + 3f - grow),
                size = Size(pw + grow * 2f, ph + grow * 2f),
                cornerRadius = CornerRadius(4f + grow),
            )
        }
        drawRoundRect(palette.polaroid, Offset(px, py), Size(pw, ph), CornerRadius(4f))

        val photoTop = Offset(px + 10f, py + 10f)
        val photoRect = Rect(photoTop, Size(inner, photoH))
        clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(photoRect, CornerRadius(2f))) }) {
            drawRect(Colors.skyText, photoRect.topLeft, photoRect.size)
            if (photo != null) drawCover(photo, photoRect)
        }
        if (texts.visitedBadge != null) {
            val badge = measurer.layout(
                texts.visitedBadge,
                jakarta(8f, FontWeight.ExtraBold, Colors.white, line = 10f, spacing = 0.6f),
                inner - 28f,
            )
            val bw = badge.textWidth() + 12f
            drawRoundRect(
                Colors.navy,
                Offset(photoTop.x + 8f, photoTop.y + 8f),
                Size(bw, 14f),
                CornerRadius(3f),
            )
            drawText(badge, topLeft = Offset(photoTop.x + 14f, photoTop.y + 10f))
        }

        var y = py + 10f + photoH + 10f
        drawText(headline, topLeft = Offset(px + 10f, y))
        y += 26f + 6f
        var x = px + 10f
        if (stars > 0) {
            val star = path(PostcardArt.STAR)
            repeat(stars) {
                withTransform({
                    translate(x, y + 1f)
                    scale(starSize / 40.56f, starSize / 40.56f, Offset.Zero)
                }) { drawPath(star, Colors.stampYellowAlt) }
                x += starSize + 2f
            }
            x += 2f
        }
        drawText(name, topLeft = Offset(x, y))
        y += 14f + 4f
        drawText(quote, topLeft = Offset(px + 10f, y))
    }

    // Place sticker: overlaps the polaroid's lower right corner, rotated 10 degrees.
    val stickerW = 84f
    val stickerH = 62f
    val stickerX = CARD_W - 10f - stickerW
    val stickerY = py + ph - 14f
    drawSticker(content, measurer, palette, Offset(stickerX, stickerY), stickerW, stickerH)

    // Round cap (74 px), left, tilted 14 degrees.
    val stampSize = 74f * palette.stampScale
    val stampTop = min(py + ph + 30f, CARD_STORY_H - FOOTER_H - stampSize - 4f)
    drawCap(texts, palette, Offset(14f, stampTop), palette.stampScale)
}

private fun DrawScope.drawCover(image: ImageBitmap, target: Rect) {
    val targetRatio = target.width / target.height
    val imageRatio = image.width.toFloat() / image.height
    val srcW: Int
    val srcH: Int
    if (imageRatio > targetRatio) {
        srcH = image.height
        srcW = (image.height * targetRatio).roundToInt()
    } else {
        srcW = image.width
        srcH = (image.width / targetRatio).roundToInt()
    }
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - srcW) / 2, (image.height - srcH) / 2),
        srcSize = IntSize(srcW, srcH),
        dstOffset = IntOffset(target.left.roundToInt(), target.top.roundToInt()),
        dstSize = IntSize(target.width.roundToInt(), target.height.roundToInt()),
        filterQuality = FilterQuality.High,
    )
}

/** Perforated sticker outline: semicircle bites every ~12 units along each edge, as in the artifact. */
private fun perforatedPath(w: Float, h: Float): Path {
    val nx = max(1, (w / 12f).roundToInt())
    val ny = max(1, (h / 12f).roundToInt())
    val px = w / nx
    val py = h / ny
    val r = 4f
    return Path().apply {
        moveTo(0f, 0f)
        for (i in 0 until nx) { // top edge, left to right: bites point down
            val x = i * px
            val gap = (px - 2 * r) / 2f
            lineTo(x + gap, 0f)
            arcTo(Rect(x + gap, -r, x + gap + 2 * r, r), 180f, -180f, false)
            lineTo(x + px, 0f)
        }
        for (i in 0 until ny) { // right edge, top to bottom: bites point left
            val y = i * py
            val gap = (py - 2 * r) / 2f
            lineTo(w, y + gap)
            arcTo(Rect(w - r, y + gap, w + r, y + gap + 2 * r), 270f, -180f, false)
            lineTo(w, y + py)
        }
        for (i in nx downTo 1) { // bottom edge, right to left: bites point up
            val x = i * px
            val gap = (px - 2 * r) / 2f
            lineTo(x - gap, h)
            arcTo(Rect(x - gap - 2 * r, h - r, x - gap, h + r), 0f, -180f, false)
            lineTo(x - px, h)
        }
        for (i in ny downTo 1) { // left edge, bottom to top: bites point right
            val y = i * py
            val gap = (py - 2 * r) / 2f
            lineTo(0f, y - gap)
            arcTo(Rect(-r, y - gap - 2 * r, r, y - gap), 90f, -180f, false)
            lineTo(0f, y - py)
        }
        close()
    }
}

private fun DrawScope.drawSticker(
    content: CardContent,
    measurer: TextMeasurer,
    palette: Palette,
    origin: Offset,
    w: Float,
    h: Float,
) {
    rotate(10f, origin + Offset(w / 2f, h / 2f)) {
        translate(origin.x, origin.y) {
            // The artifact draws the outline in a viewBox 6 units larger than the sticker, so the white stroke fits.
            val s = min(w / (w + 6f), h / (h + 6f))
            withTransform({
                translate((w - w * s) / 2f, (h - h * s) / 2f)
                scale(s, s, Offset.Zero)
            }) {
                val outline = perforatedPath(w, h)
                drawPath(outline, palette.stickerFill)
                drawPath(outline, Colors.white, style = Stroke(width = 5f, join = StrokeJoin.Round))
            }
            val icon = TOWER_PATHS
            val iconH = 18f
            val iconScale = iconH / 60f
            // The sticker carries the place name once: 11 sp on one line when short, 8 sp on up to two lines
            // (ellipsized) when long, so a name like "Keraton Ngayogyakarta Hadiningrat" never overflows.
            val stampText = content.placeName.uppercase()
            val longStamp = stampText.length > 8
            val label = if (longStamp) {
                measurer.layout(stampText, jakarta(8f, FontWeight.ExtraBold, palette.stickerText, line = 9f, spacing = 0.3f).copy(textAlign = TextAlign.Center), w - 16f, maxLines = 2)
            } else {
                measurer.layout(stampText, jakarta(11f, FontWeight.ExtraBold, palette.stickerText, line = 13f, spacing = 0.8f), w - 16f)
            }
            val labelH = label.lineCount * (if (longStamp) 9f else 13f)
            val total = iconH + 2f + labelH
            var y = (h - total) / 2f
            withTransform({
                translate((w - 57.85f * iconScale) / 2f, y)
                scale(iconScale, iconScale, Offset.Zero)
            }) { icon.forEach { drawPath(it, palette.stickerText) } }
            y += iconH + 2f
            drawText(label, topLeft = Offset((w - label.textWidth()) / 2f, y))
        }
    }
}

private val TOWER_PATHS: List<Path> by lazy { PostcardArt.TOWER.map(::path) }

/** Round "cap KELANA" with the stamp date on the lower arc. 74 x 74 units at scale 1. */
private fun DrawScope.drawCap(texts: CardTexts, palette: Palette, origin: Offset, factor: Float) {
    val ink = palette.stampInk
    translate(origin.x, origin.y) {
        scale(factor, factor, Offset.Zero) {
            rotate(14f, Offset(37f, 37f)) {
                drawCircle(ink.copy(alpha = 0.88f), 35f, Offset(37f, 37f), style = Stroke(2.4f))
                drawCircle(ink.copy(alpha = 0.88f), 22.2f, Offset(37f, 37f), style = Stroke(1.4f))
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ink.copy(alpha = 0.88f).toArgb()
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val topArc = android.graphics.Path().apply {
                        arcTo(RectF(37f - 26.64f, 37f - 26.64f, 37f + 26.64f, 37f + 26.64f), 180f, 180f)
                    }
                    paint.textSize = 7f
                    paint.letterSpacing = 1.5f / 7f
                    native.drawTextOnPath("KELANA", topArc, (PI * 26.64).toFloat() / 2f, 0f, paint)
                    if (texts.stampDate != null) {
                        val bottomArc = android.graphics.Path().apply {
                            arcTo(RectF(37f - 29.6f, 37f - 29.6f, 37f + 29.6f, 37f + 29.6f), 180f, -180f)
                        }
                        paint.textSize = 5.9f
                        paint.letterSpacing = 1f / 5.9f
                        native.drawTextOnPath(texts.stampDate, bottomArc, (PI * 29.6).toFloat() / 2f, 0f, paint)
                    }
                    paint.textSize = 8.9f
                    paint.letterSpacing = 0f
                    native.drawText(texts.regionCode, 37f, 40.3f, paint)
                }
            }
        }
    }
}

private fun DrawScope.drawFooter(texts: CardTexts, measurer: TextMeasurer, top: Float) {
    drawRect(Colors.backgroundWarm, Offset(0f, top), Size(CARD_W, FOOTER_H))
    // Logo: viewBox 204 1395 166 56 shown 77 x 26 at (14, centred).
    val k = 77f / 166f
    withTransform({
        translate(14f, top + (FOOTER_H - 26f) / 2f)
        scale(k, k, Offset.Zero)
        translate(-204f, -1395f)
    }) {
        withTransform({
            translate(211f, 1406.2f)
            val m = 42f / 1160f
            scale(m, m, Offset.Zero)
            translate(-7450f, -7470f)
        }) {
            drawPath(path(PostcardArt.LOGO_MASCOT_YELLOW), Colors.stampYellow)
            drawPath(path(PostcardArt.LOGO_MASCOT_BROWN), Colors.stampBrown)
        }
        drawPath(path(PostcardArt.LOGO_WORDMARK), Colors.ink)
    }
    val tagline = measurer.layout(texts.tagline, jakarta(9f, FontWeight.ExtraBold, Colors.stampBrown, line = 12f, spacing = 0.6f), 140f)
    drawText(tagline, topLeft = Offset(CARD_W - 14f - tagline.textWidth(), top + (FOOTER_H - 12f) / 2f))
}

package id.kelana.feature.paspor.ui

import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import id.kelana.feature.paspor.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------------------------
// Scene: an illustration laid out in artifact units (the artifact frames are 390 wide) and scaled to the width.
// ---------------------------------------------------------------------------------------------------------------

/** Converts artifact units to dp for the current scene width. */
@Stable
internal class SceneScale(val factor: Float) {
    fun dp(units: Float): Dp = (units * factor).dp
}

/** Radius of the illustration panels (artifact `border-radius: 28px`). */
internal val PanelRadius = 28.dp

/** A box with the artifact's aspect ratio. [content] positions pieces in artifact units through [SceneScale]. */
@Composable
internal fun Scene(
    designWidth: Float,
    designHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(SceneScale) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(designWidth / designHeight)) {
        content(SceneScale(maxWidth.value / designWidth))
    }
}

/** Full-bleed background art (blob and sparkles) that stretches over the scene. */
@Composable
internal fun BoxScope.SceneBackground(@DrawableRes res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.FillBounds,
    )
}

/** One piece of art at artifact coordinates ([x], [y] top-left, square [size]), turned by [rotation] degrees. */
@Composable
internal fun BoxScope.ScenePiece(
    scale: SceneScale,
    @DrawableRes res: Int,
    x: Float,
    y: Float,
    size: Float,
    rotation: Float = 0f,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .offset(scale.dp(x), scale.dp(y))
            .size(scale.dp(size))
            .rotate(rotation),
    )
}

/** Dashed round-capped route curve of the Izin lokasi scene (6.1), drawn in Compose because vector drawables cannot dash. */
@Composable
internal fun BoxScope.SceneIzinRoute(scale: SceneScale, color: Color) {
    Canvas(Modifier.matchParentSize()) {
        val u = scale.factor * density
        val path = Path().apply {
            moveTo(80f * u, 190f * u)
            cubicTo(120f * u, 222f * u, 200f * u, 226f * u, 250f * u, 150f * u)
        }
        drawPath(
            path,
            color,
            style = Stroke(
                width = 3f * u,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u, 7f * u)),
            ),
        )
    }
}

/** Dashed "cap area" circle (and optional centre dot) of the map panels. */
@Composable
internal fun BoxScope.SceneAreaCircle(
    scale: SceneScale,
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    fillAlpha: Float,
) {
    Canvas(Modifier.matchParentSize()) {
        val u = scale.factor * density
        val c = Offset(cx * u, cy * u)
        drawCircle(color.copy(alpha = fillAlpha), radius * u, c)
        drawCircle(
            color,
            radius * u,
            c,
            style = Stroke(2.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * u, 7f * u))),
        )
    }
}

/** The blue "you are here" dot of the map panels. */
@Composable
internal fun BoxScope.SceneLocationDot(scale: SceneScale, cx: Float, cy: Float, color: Color, halo: Boolean) {
    Canvas(Modifier.matchParentSize()) {
        val u = scale.factor * density
        val c = Offset(cx * u, cy * u)
        if (halo) drawCircle(color.copy(alpha = 0.18f), 18f * u, c)
        drawCircle(Color.White, 9.5f * u, c)
        drawCircle(color, 8f * u, c)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Sticker: the perforated stamp with a white edge (artifact `Stiker`).
// ---------------------------------------------------------------------------------------------------------------

internal enum class StickerTone { Yellow, Blue, Brown }

/** Rectangle with half-circle notches (radius [r]) cut into every edge, about one notch per [pitch]. */
internal fun perforatedPath(w: Float, h: Float, r: Float, pitch: Float): Path {
    val nx = max(1, (w / pitch).roundToInt())
    val ny = max(1, (h / pitch).roundToInt())
    val px = w / nx
    val py = h / ny
    val mx = (px - 2 * r) / 2
    val my = (py - 2 * r) / 2
    return Path().apply {
        moveTo(0f, 0f)
        for (i in 0 until nx) { // top, left to right
            val x0 = i * px + mx
            lineTo(x0, 0f)
            arcTo(Rect(x0, -r, x0 + 2 * r, r), 180f, -180f, false)
        }
        lineTo(w, 0f)
        for (i in 0 until ny) { // right, top to bottom
            val y0 = i * py + my
            lineTo(w, y0)
            arcTo(Rect(w - r, y0, w + r, y0 + 2 * r), 270f, -180f, false)
        }
        lineTo(w, h)
        for (i in nx - 1 downTo 0) { // bottom, right to left
            val x1 = i * px + mx + 2 * r
            lineTo(x1, h)
            arcTo(Rect(x1 - 2 * r, h - r, x1, h + r), 0f, -180f, false)
        }
        lineTo(0f, h)
        for (i in ny - 1 downTo 0) { // left, bottom to top
            val y1 = i * py + my + 2 * r
            lineTo(0f, y1)
            arcTo(Rect(-r, y1 - 2 * r, r, y1), 90f, -180f, false)
        }
        close()
    }
}

/**
 * The sticker of one place: [label] and [code] under a silhouette of [icon]. Drop shadow of the artifact is skipped.
 * Yellow tone uses navy ink, Blue and Brown use warm white ink with a yellow icon.
 */
@Composable
internal fun CapSticker(
    label: String,
    code: String,
    @DrawableRes icon: Int,
    tone: StickerTone,
    width: Dp,
    height: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 11.sp,
) {
    val colors = KelanaTheme.colors
    val fill = when (tone) {
        StickerTone.Yellow -> colors.stampYellow
        StickerTone.Blue -> colors.primary
        StickerTone.Brown -> colors.stampBrown
    }
    val ink = if (tone == StickerTone.Yellow) colors.navy else colors.backgroundWarm
    val iconTint = if (tone == StickerTone.Yellow) colors.navy else colors.stampYellow
    Box(
        modifier
            .size(width, height)
            .drawBehind {
                val path = perforatedPath(size.width, size.height, 4.dp.toPx(), 12.dp.toPx())
                drawPath(path, fill)
                drawPath(path, Color.White, style = Stroke(5.dp.toPx(), join = StrokeJoin.Round))
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconTint),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = labelSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                ),
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                color = ink.copy(alpha = 0.9f),
                maxLines = 1,
            )
        }
    }
}

/** Icon of a place's sticker, by category (artifact art: museum, candi, batik, building, tugu). */
@DrawableRes
internal fun PlaceCategory.stampIcon(): Int = when (this) {
    PlaceCategory.Museum -> R.drawable.paspor_art_museum
    PlaceCategory.Candi -> R.drawable.paspor_art_candi
    PlaceCategory.Kerajinan -> R.drawable.paspor_art_batik
    PlaceCategory.Lainnya -> R.drawable.paspor_art_keraton
    PlaceCategory.Tari, PlaceCategory.Pentas, PlaceCategory.SeniMusik -> R.drawable.paspor_art_tugu
}

/**
 * Short sticker label: the artifact prints "SONOBUDOYO", "PRAMBANAN" rather than the full place name. There is no short
 * name in the model, so a leading generic word (Museum, Candi) is dropped. PROVISIONAL until Place carries a short name.
 */
internal fun stampLabel(name: String, locale: java.util.Locale): String {
    val trimmed = name.trim()
    val first = trimmed.substringBefore(' ')
    val rest = trimmed.substringAfter(' ', "")
    val short = if (rest.isNotBlank() && first.lowercase(locale) in GenericLeadWords) rest else trimmed
    return short.uppercase(locale)
}

private val GenericLeadWords = setOf("museum", "candi")

/** Artifact prints labels at 11 sp; longer ones are shrunk so they stay on one line. */
internal fun stampLabelSize(label: String): TextUnit = if (label.length > 10) 8.sp else 11.sp

/** The artifact cycles yellow, blue and brown stickers through the passport. */
internal fun stickerToneFor(index: Int): StickerTone = StickerTone.entries[index.mod(StickerTone.entries.size)]

// ---------------------------------------------------------------------------------------------------------------
// Seal: round "DIKUNJUNGI" / "KELANA" stamp with text on two arcs.
// ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun CapSeal(
    top: String,
    bottom: String,
    center: String,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    color: Color = KelanaTheme.colors.stampYellow,
) {
    Canvas(modifier.size(size).rotate(rotation).alpha(0.88f)) {
        val u = this.size.width / 96f
        val c = Offset(48f * u, 48f * u)
        drawCircle(color, 46f * u, c, style = Stroke(2.4f * u))
        drawCircle(color, 28.8f * u, c, style = Stroke(1.4f * u))
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            fun paint(sizeUnits: Float, spacingEm: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                typeface = Typeface.DEFAULT_BOLD
                textSize = sizeUnits * u
                letterSpacing = spacingEm
            }

            fun arcText(text: String, paint: android.graphics.Paint, radius: Float, onTop: Boolean) {
                val r = radius * u
                val oval = RectF(c.x - r, c.y - r, c.x + r, c.y + r)
                val path = android.graphics.Path()
                path.addArc(oval, 180f, if (onTop) 180f else -180f)
                val offset = ((PI * r) - paint.measureText(text)) / 2
                native.drawTextOnPath(text, path, offset.toFloat(), 0f, paint)
            }
            arcText(top, paint(9.1f, 1.5f / 9.1f), 34.56f, onTop = true)
            arcText(bottom, paint(7.7f, 1f / 7.7f), 38.4f, onTop = false)
            val centerPaint = paint(11.5f, 0f).apply { textAlign = android.graphics.Paint.Align.CENTER }
            native.drawText(center, c.x, 52.3f * u, centerPaint)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Buttons: the design system has no 48 dp outline-on-primary or white-on-orange pill, so these live here.
// ---------------------------------------------------------------------------------------------------------------

internal enum class PasporButtonStyle { Orange, Primary, Outlined, WhiteOnBrown, OutlinedOnBrown }

@Composable
internal fun PasporButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PasporButtonStyle = PasporButtonStyle.Orange,
    height: Dp = KelanaSizes.buttonHeight,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    @DrawableRes icon: Int? = null,
) {
    val colors = KelanaTheme.colors
    val (container, content, border) = when (style) {
        // Artifact 6.2 / 6.4 / 6.5 buttons are #A84E2B (stampBrown), not the shared accentOrange.
        PasporButtonStyle.Orange -> Triple(colors.stampBrown, colors.white, null)
        PasporButtonStyle.Primary -> Triple(colors.primary, colors.white, null)
        PasporButtonStyle.Outlined -> Triple(Color.Transparent, colors.primary, colors.primary)
        PasporButtonStyle.WhiteOnBrown -> Triple(colors.white, colors.stampBrown, null)
        PasporButtonStyle.OutlinedOnBrown -> Triple(Color.Transparent, colors.white, colors.white.copy(alpha = 0.7f))
    }
    Box(
        modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(KelanaShapes.button)
            .background(container)
            .let { if (border != null) it.border(1.5.dp, border, KelanaShapes.button) else it }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize, fontWeight = fontWeight),
                color = content,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The panel frame of Ambil cap screens: rounded 28 dp, clipped. */
@Composable
internal fun PanelFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(PanelRadius))) { content() }
}


package id.kelana.feature.jelajah

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.PlaceCard
import id.kelana.core.designsystem.component.PlaceCardSize
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import java.time.LocalDate

/** Category tile / cover colours of the artifact (JelajahIlustratif tiles, KatMuseum rows). */
internal fun PlaceCategory.tint(colors: KelanaColors): Color = when (this) {
    PlaceCategory.Museum -> colors.skyText
    PlaceCategory.Candi -> colors.peach
    PlaceCategory.Tari -> colors.creamYellow
    PlaceCategory.Topeng -> colors.peach
    PlaceCategory.Gamelan -> colors.creamYellow
    PlaceCategory.Kerajinan -> colors.backgroundWarm
    PlaceCategory.Lainnya -> colors.backgroundWarm
}

/** Illustration of a category, converted from the inline SVGs of the artifact. */
@DrawableRes
internal fun PlaceCategory.art(): Int = when (this) {
    PlaceCategory.Museum -> R.drawable.jelajah_art_museum
    PlaceCategory.Candi -> R.drawable.jelajah_art_candi
    PlaceCategory.Tari -> R.drawable.jelajah_art_tari
    PlaceCategory.Topeng -> R.drawable.jelajah_art_topeng
    PlaceCategory.Gamelan -> R.drawable.jelajah_art_gamelan
    PlaceCategory.Kerajinan -> R.drawable.jelajah_art_batik
    PlaceCategory.Lainnya -> R.drawable.jelajah_art_semua
}

/** A decorative illustration piece. Drawables keep their original colours. */
@Composable
internal fun Art(@DrawableRes id: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painter = painterResource(id), contentDescription = null, modifier = modifier.size(size))
}

/** Illustration piece placed at [x], [y] inside a cover and rotated like the artifact. */
@Composable
internal fun ArtPiece(@DrawableRes id: Int, size: Dp, x: Dp, y: Dp, degrees: Float) {
    Art(id, size, Modifier.offset(x, y).rotate(degrees))
}

/**
 * Cover of a place. [compact] is the 60 dp list thumbnail (tinted tile, 40 dp illustration centred);
 * otherwise the 156 x 112 grid cover with the soft blob and 2 illustration pieces (JelajahIlustratif).
 * The drop shadow of the pieces is skipped.
 */
@Composable
internal fun PlaceCover(category: PlaceCategory, compact: Boolean = true, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Box(modifier.fillMaxSize().background(category.tint(colors)), contentAlignment = Alignment.Center) {
        if (compact) {
            Art(category.art(), 40.dp)
        } else {
            Image(
                painter = painterResource(R.drawable.jelajah_art_cover_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Box(Modifier.fillMaxSize()) {
                ArtPiece(category.art(), 58.dp, 48.dp, 24.dp, -4f)
                ArtPiece(R.drawable.jelajah_art_batik, 30.dp, 16.dp, 62.dp, -12f)
            }
        }
    }
}

/** Green "Diperiksa 20 Nov 2026" pill: stamp icon 14 dp, radius 4, 12/600. */
@Composable
internal fun CheckedPill(checkedAt: LocalDate, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier.clip(KelanaShapes.badge).background(colors.successSurface).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Art(R.drawable.jelajah_art_badge_stamp, 14.dp)
        Text(
            text = stringResource(R.string.badge_checked, formatCheckedDate(checkedAt)),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.successText,
        )
    }
}

/** Amber "... sedang dicek" pill shown instead of any value that has no check date. */
@Composable
internal fun CheckingPill(text: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Text(
        text = text,
        modifier = modifier.clip(KelanaShapes.badge).background(colors.creamYellow).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = colors.warnText,
    )
}

/** Brown "Cap YK-02" pill (KatMuseum). */
@Composable
internal fun CapPill(code: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Text(
        text = stringResource(R.string.badge_cap, code),
        modifier = modifier.clip(KelanaShapes.badge).background(colors.backgroundWarm).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = colors.stampBrown,
    )
}

/** "Diperiksa <tanggal>" when any hour fact was checked, otherwise "Sedang dicek". Never a value without a date. */
@Composable
internal fun PlaceCheckedBadge(place: Place) {
    val checkedAt = place.latestCheckedAt()
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        if (checkedAt != null) {
            CheckedPill(checkedAt)
        } else {
            CheckingPill(stringResource(R.string.badge_checking))
        }
        if (place.capCode.isNotBlank()) CapPill(place.capCode)
    }
}

@Composable
internal fun PlaceItem(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlaceCardSize = PlaceCardSize.List,
) {
    PlaceCard(
        title = place.name,
        subtitle = place.description,
        onClick = onClick,
        modifier = modifier,
        size = size,
        badge = { PlaceCheckedBadge(place) },
        cover = { PlaceCover(place.category, compact = size == PlaceCardSize.List) },
    )
}

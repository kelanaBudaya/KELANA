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
import id.kelana.core.ui.photo.LocalPlaceCovers
import id.kelana.core.ui.photo.PlacePhoto
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
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import java.time.LocalDate

/** Category tile / cover colours of the artifact (JelajahIlustratif tiles, KatMuseum rows). */
internal fun PlaceCategory.tint(colors: KelanaColors): Color = when (this) {
    PlaceCategory.Museum -> colors.skyText
    PlaceCategory.Candi -> colors.peach
    PlaceCategory.Tari -> colors.creamYellow
    PlaceCategory.Pentas -> colors.peach
    PlaceCategory.SeniMusik -> colors.creamYellow
    PlaceCategory.Kerajinan -> colors.backgroundWarm
    PlaceCategory.Lainnya -> colors.backgroundWarm
}

/** Illustration of a category, converted from the inline SVGs of the artifact. */
@DrawableRes
internal fun PlaceCategory.art(): Int = when (this) {
    PlaceCategory.Museum -> R.drawable.jelajah_art_museum
    PlaceCategory.Candi -> R.drawable.jelajah_art_candi
    PlaceCategory.Tari -> R.drawable.jelajah_art_tari
    PlaceCategory.Pentas -> R.drawable.jelajah_art_pentas
    PlaceCategory.SeniMusik -> R.drawable.jelajah_art_seni_musik
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

/** Real cover photo of [place] when it has one; the category illustration otherwise, while loading and on error. */
@Composable
internal fun PlaceCoverPhoto(place: Place, compact: Boolean, modifier: Modifier = Modifier) {
    PlacePhoto(
        thumbUrl = LocalPlaceCovers.current[place.id],
        fallback = { PlaceCover(place.category, compact = compact) },
        contentDescription = place.name,
        modifier = modifier.fillMaxSize(),
    )
}

/** Green "Kunjungi Hari Ini" pill, shown only when the checked hours say the place is open today. */
@Composable
internal fun VisitTodayPill(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Text(
        text = stringResource(R.string.badge_visit_today),
        modifier = modifier.clip(KelanaShapes.badge).background(colors.successSurface).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = colors.successText,
    )
}

/** Brown "Cap <place name>" pill (KatMuseum). */
@Composable
internal fun CapPill(placeName: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Text(
        text = stringResource(R.string.badge_cap, placeName),
        modifier = modifier.clip(KelanaShapes.badge).background(colors.backgroundWarm).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = colors.stampBrown,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

/** "Kunjungi Hari Ini" when open on [today] (checked hours only), plus the stamp pill. No status pill otherwise. */
@Composable
internal fun PlaceCheckedBadge(place: Place, today: LocalDate?) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        if (isVisitToday(place, today)) VisitTodayPill()
        if (place.capCode.isNotBlank()) CapPill(place.name, Modifier.weight(1f, fill = false))
    }
}

@Composable
internal fun PlaceItem(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlaceCardSize = PlaceCardSize.List,
    today: LocalDate? = null,
) {
    PlaceCard(
        title = place.name,
        subtitle = place.description,
        onClick = onClick,
        modifier = modifier,
        size = size,
        badge = { PlaceCheckedBadge(place, today) },
        cover = { PlaceCoverPhoto(place, compact = size == PlaceCardSize.List) },
    )
}

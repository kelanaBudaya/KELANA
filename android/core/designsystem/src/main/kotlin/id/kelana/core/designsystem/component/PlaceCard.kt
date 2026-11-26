package id.kelana.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

enum class PlaceCardSize {
    /** Cover on top, title and subtitle below (Jelajah "Pilihan buat keluargamu", Semua tempat). */
    Grid,

    /** Cover tile on the left, text in the middle, chevron on the right (Cari, kategori lists). */
    List,
}

/**
 * Place card with a cover slot. The caller draws the cover (illustration or image) in [cover].
 * Sizes from JelajahIlustratif (grid cover 156 x 112, radius 16) and KatMuseum (list row: padding 12, radius 20,
 * 1 dp hairline, 60 dp thumb with radius 16, title 16/800, subtitle 13/18 muted). Grid cover aspect 156:112.
 * [badge] sits under the subtitle, for example a status pill.
 */
@Composable
fun PlaceCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlaceCardSize = PlaceCardSize.Grid,
    badge: (@Composable () -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    when (size) {
        PlaceCardSize.Grid -> GridPlaceCard(title, subtitle, onClick, modifier, badge, cover)
        PlaceCardSize.List -> ListPlaceCard(title, subtitle, onClick, modifier, badge, cover)
    }
}

private val CoverShape = KelanaShapes.card

@Composable
private fun GridPlaceCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier,
    badge: (@Composable () -> Unit)?,
    cover: @Composable BoxScope.() -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(modifier = modifier.clip(CoverShape).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(156f / 112f)
                .clip(CoverShape)
                .background(colors.backgroundWarm),
            content = cover,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = KelanaSpacing.sm),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = KelanaSpacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (badge != null) Box(Modifier.padding(top = KelanaSpacing.sm)) { badge() }
    }
}

@Composable
private fun ListPlaceCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier,
    badge: (@Composable () -> Unit)?,
    cover: @Composable BoxScope.() -> Unit,
) {
    val colors = KelanaTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = KelanaShapes.listCard,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(KelanaSizes.listThumb)
                    .clip(CoverShape)
                    .background(colors.backgroundWarm),
                content = cover,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KelanaSpacing.md),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = KelanaSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) Box(Modifier.padding(top = KelanaSpacing.sm)) { badge() }
            }
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PlaceCardPreview() {
    KelanaTheme {
        Column(Modifier.padding(KelanaSpacing.gutter)) {
            PlaceCard(
                title = stringResource(R.string.ds_preview_card_title),
                subtitle = stringResource(R.string.ds_preview_card_body),
                onClick = {},
                modifier = Modifier.size(width = 156.dp, height = 220.dp),
                size = PlaceCardSize.Grid,
                cover = {},
            )
            Spacer(Modifier.height(KelanaSpacing.md))
            PlaceCard(
                title = stringResource(R.string.ds_preview_card_title),
                subtitle = stringResource(R.string.ds_preview_card_body),
                onClick = {},
                size = PlaceCardSize.List,
                cover = {},
            )
        }
    }
}

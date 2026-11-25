package id.kelana.feature.tempat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.feature.tempat.R

/** Official photos gallery (Galeri.dc.html, screen 2.5). The photos do not exist yet: the tiles are placeholders. */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun FotoResmiScreen(
    route: FotoResmiRoute,
    onBack: () -> Unit,
    viewModel: FotoResmiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    when (state.status) {
        PlaceLoadStatus.Loading -> LoadingBody()
        PlaceLoadStatus.NotFound, PlaceLoadStatus.Error -> MessageScreen(
            title = stringResource(R.string.tempat_not_found_title),
            body = stringResource(R.string.tempat_not_found_body),
            onBack = onBack,
        )
        PlaceLoadStatus.Loaded -> FotoResmiContent(state = state, onBack = onBack)
    }
}

@Composable
internal fun FotoResmiContent(state: FotoResmiUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val count = state.photoCount
    val current = selectedIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    Column(modifier.fillMaxSize().background(colors.galleryDark)) {
        // Top bar: close and "Foto resmi · 1 dari 4".
        Row(
            Modifier.fillMaxWidth().padding(start = KelanaSpacing.sm, end = KelanaSpacing.md, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.tempat_ic_close),
                    contentDescription = stringResource(R.string.foto_resmi_close),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(R.string.foto_resmi_title, current + 1, count),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.white,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Hero: placeholder for the photo, with the "Dokumentasi resmi" badge.
            Box(
                Modifier.padding(top = 18.dp).fillMaxWidth().height(420.dp).background(colors.galleryPanel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.foto_resmi_hero, current + 1),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.skyText,
                )
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(KelanaShapes.chip)
                        .background(colors.white)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(R.drawable.tempat_ic_info), contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.foto_resmi_badge),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.ink,
                    )
                }
            }
            // Caption and source are not verified yet, so the app says "sedang dicek" instead of inventing them.
            Column(
                Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.gutter),
            ) {
                Text(
                    text = stringResource(R.string.foto_resmi_caption_pending),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.white,
                )
                Text(
                    text = stringResource(R.string.foto_resmi_source_pending),
                    modifier = Modifier.padding(top = KelanaSpacing.xs),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 18.sp),
                    color = colors.dashMuted,
                )
            }
            // Thumbnails.
            Row(
                Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
            ) {
                for (index in 0 until count) {
                    Thumb(index = index, isSelected = index == current, onSelect = { selectedIndex = index })
                }
            }
            Spacer(Modifier.height(KelanaSpacing.lg))
        }
        // Note: visitor photos live in postcards and videos.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.md, bottom = 28.dp)
                .clip(KelanaShapes.card)
                .background(colors.white.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(R.drawable.tempat_ic_media), contentDescription = null, Modifier.size(26.dp))
            Text(
                text = stringResource(R.string.foto_resmi_note),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                color = colors.skyText,
            )
        }
    }
}

@Composable
private fun RowScope.Thumb(index: Int, isSelected: Boolean, onSelect: () -> Unit) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.foto_resmi_thumb, index + 1)
    Box(
        Modifier
            .weight(1f)
            .height(64.dp)
            .clip(KelanaShapes.input)
            .background(colors.galleryPanel)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) colors.stampYellow else colors.galleryTileBorder,
                shape = KelanaShapes.input,
            )
            .clickable(role = Role.Button, onClick = onSelect)
            .semantics {
                contentDescription = description
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.skyText,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FotoResmiPreview() {
    KelanaTheme {
        FotoResmiContent(
            state = FotoResmiUiState(PlaceLoadStatus.Loaded, "Museum Sonobudoyo", "museum-sonobudoyo", 4),
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun FotoResmiNotFoundPreview() {
    KelanaTheme {
        MessageScreen(
            title = stringResource(R.string.tempat_not_found_title),
            body = stringResource(R.string.tempat_not_found_body),
            onBack = {},
        )
    }
}

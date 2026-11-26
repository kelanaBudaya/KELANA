package id.kelana.feature.tempat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlacePhoto
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.core.ui.openCustomTab
import id.kelana.feature.tempat.R

/**
 * Photo gallery (Galeri.dc.html, screen 2.5): open-licensed photos (Wikimedia Commons) of a place, each with its
 * attribution and a "Lihat sumber" link. Offline, photos already seen still show from the Coil disk cache.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun FotoResmiScreen(
    route: FotoResmiRoute,
    onBack: () -> Unit,
    viewModel: FotoResmiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    when (state.status) {
        PlaceLoadStatus.Loading -> LoadingBody()
        PlaceLoadStatus.NotFound -> MessageScreen(
            title = stringResource(R.string.tempat_not_found_title),
            body = stringResource(R.string.tempat_not_found_body),
            onBack = onBack,
        )
        PlaceLoadStatus.Error -> MessageScreen(
            title = stringResource(R.string.tempat_error_title),
            body = stringResource(R.string.tempat_error_body),
            onBack = onBack,
            actionLabel = stringResource(R.string.tempat_retry),
            onAction = viewModel::retry,
        )
        PlaceLoadStatus.Loaded -> when (state.photoStatus) {
            PhotoStatus.Loading -> LoadingBody()
            else -> FotoResmiContent(
                state = state,
                onBack = onBack,
                onRetry = viewModel::retry,
                onOpenSource = { url -> openCustomTab(context, url) },
            )
        }
    }
}

@Composable
internal fun FotoResmiContent(
    state: FotoResmiUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onOpenSource: (String) -> Unit = {},
) {
    val colors = KelanaTheme.colors
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val count = state.photoCount
    val current = selectedIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val photo = state.photos.getOrNull(current)
    Column(modifier.fillMaxSize().background(colors.galleryDark)) {
        // Top bar: close and "Galeri foto · 1 dari 4".
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
                text = if (photo != null) {
                    stringResource(R.string.foto_resmi_title, current + 1, count)
                } else {
                    stringResource(R.string.foto_resmi_title_plain)
                },
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.white,
            )
        }
        when {
            photo != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = photo.caption ?: stringResource(R.string.foto_resmi_hero, current + 1),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(colors.galleryPanel),
                    error = ColorPainter(colors.galleryPanel),
                    modifier = Modifier.padding(top = 18.dp).fillMaxWidth().height(420.dp).background(colors.galleryPanel),
                )
                Column(
                    Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
                ) {
                    photo.caption?.let { caption ->
                        Text(text = caption, style = MaterialTheme.typography.titleMedium, color = colors.white)
                    }
                    Text(
                        text = photo.attributionText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                        color = colors.skyText,
                    )
                    Text(
                        text = stringResource(R.string.foto_resmi_source),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onOpenSource(photo.sourcePageUrl) }
                            .wrapContentHeight(Alignment.CenterVertically),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                        ),
                        color = colors.stampYellow,
                    )
                }
                if (count > 1) {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = KelanaSpacing.gutterWide, vertical = KelanaSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
                    ) {
                        state.photos.forEachIndexed { index, item ->
                            Thumb(
                                photo = item,
                                index = index,
                                isSelected = index == current,
                                onSelect = { selectedIndex = index },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(KelanaSpacing.lg))
            }
            state.photoStatus == PhotoStatus.Error -> GalleryMessage(
                text = stringResource(R.string.foto_resmi_error_title),
                body = stringResource(R.string.foto_resmi_error_body),
                actionLabel = stringResource(R.string.foto_resmi_retry),
                onAction = onRetry,
                modifier = Modifier.weight(1f),
            )
            else -> GalleryMessage(
                text = stringResource(R.string.foto_resmi_empty),
                modifier = Modifier.weight(1f),
            )
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
private fun GalleryMessage(
    text: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = KelanaTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutterWide),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = colors.white, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(KelanaSpacing.sm))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = colors.skyText, textAlign = TextAlign.Center)
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                modifier = Modifier
                    .padding(top = KelanaSpacing.md)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onAction)
                    .wrapContentHeight(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
                color = colors.stampYellow,
            )
        }
    }
}

@Composable
private fun Thumb(photo: PlacePhoto, index: Int, isSelected: Boolean, onSelect: () -> Unit) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.foto_resmi_thumb, index + 1)
    AsyncImage(
        model = photo.thumbUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(colors.galleryPanel),
        error = ColorPainter(colors.galleryPanel),
        modifier = Modifier
            .size(width = 88.dp, height = 64.dp)
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
    )
}

private fun previewPhoto(i: Int) = PlacePhoto(
    id = "p$i", placeId = "p1", url = "https://example.invalid/$i.jpg", thumbUrl = "https://example.invalid/${i}_t.jpg",
    width = 1600, height = 1000, caption = if (i == 1) "Tampak depan" else null,
    attributionText = "Foto: Contoh · CC BY-SA 4.0 · Wikimedia Commons",
    licenseCode = "CC-BY-SA-4.0", licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
    sourcePageUrl = "https://commons.wikimedia.org/wiki/File:Contoh.jpg",
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FotoResmiPreview() {
    KelanaTheme {
        FotoResmiContent(
            state = FotoResmiUiState(PlaceLoadStatus.Loaded, "Museum Sonobudoyo", (1..4).map(::previewPhoto), PhotoStatus.Loaded),
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun FotoResmiEmptyPreview() {
    KelanaTheme {
        FotoResmiContent(
            state = FotoResmiUiState(PlaceLoadStatus.Loaded, "Museum Sonobudoyo", emptyList(), PhotoStatus.Empty),
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

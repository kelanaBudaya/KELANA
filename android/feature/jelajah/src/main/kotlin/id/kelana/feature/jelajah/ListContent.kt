package id.kelana.feature.jelajah

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.ui.label

/** Hero background shapes of a Kategori header (KatMuseum, KatCandi/KatTopeng, KatTari/KatGamelan; KatSemua has its own). */
@DrawableRes
private fun PlaceCategory?.heroBackground(): Int = when (this) {
    PlaceCategory.Museum -> R.drawable.jelajah_art_hero_museum
    PlaceCategory.Candi, PlaceCategory.Pentas -> R.drawable.jelajah_art_hero_candi
    else -> R.drawable.jelajah_art_hero_tari
}

/**
 * Shared layout of Kategori and Semua tempat, compared with docs/design/artifact/KatMuseum.dc.html (and KatCandi,
 * KatTari, KatGamelan, KatTopeng): 220 dp illustrated hero with back button, title 28/800 and subtitle 13,
 * chip row, list of place rows.
 */
@Composable
private fun ListScaffold(
    title: String,
    subtitle: String,
    headerColor: Color,
    @DrawableRes heroBackground: Int,
    heroPieces: @Composable () -> Unit,
    onBack: () -> Unit,
    chips: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(headerColor)) {
            Image(
                painter = painterResource(heroBackground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            heroPieces()
            Box(
                Modifier.padding(start = 16.dp, top = 14.dp).size(KelanaSizes.backButton).clip(CircleShape)
                    .background(colors.white).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back_cd),
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(colors.ink),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 150.dp, bottom = 20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = colors.ink)
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink,
                )
            }
        }
        Box(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) { chips() }
        body()
    }
}

@Composable
private fun PlaceList(places: List<Place>, today: java.time.LocalDate?, onPlace: (Place) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(places, key = { it.id }) { place -> PlaceItem(place = place, onClick = { onPlace(place) }, today = today) }
    }
}

@StringRes
private fun PlaceCategory?.subtitleRes(): Int = when (this) {
    PlaceCategory.Museum -> R.string.kategori_subtitle_museum
    PlaceCategory.Candi -> R.string.kategori_subtitle_candi
    PlaceCategory.Tari -> R.string.kategori_subtitle_tari
    PlaceCategory.Pentas -> R.string.kategori_subtitle_pentas
    PlaceCategory.SeniMusik -> R.string.kategori_subtitle_seni_musik
    else -> R.string.kategori_subtitle_other
}

/** Empty category (artifact KatTopeng): illustration, title, body and an outlined "Kabari aku" pill. */
@Composable
private fun KategoriEmpty(category: PlaceCategory?, name: String, onNotifyMe: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.jelajah_art_empty_bg),
                contentDescription = null,
                modifier = Modifier.height(110.dp),
                contentScale = ContentScale.FillHeight,
            )
            Art(category?.art() ?: R.drawable.jelajah_art_semua, 64.dp)
        }
        Text(
            stringResource(R.string.kategori_empty_title, name.lowercase()),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.kategori_empty_body),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
        Surface(
            onClick = onNotifyMe,
            modifier = Modifier.padding(top = 4.dp).height(KelanaSizes.inputHeight),
            shape = CircleShape,
            color = colors.background,
            border = BorderStroke(1.5.dp, colors.primary),
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(colors.primary),
                )
                Text(
                    stringResource(R.string.kabari_aku),
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
internal fun KategoriContent(
    state: KategoriUiState,
    onBack: () -> Unit,
    onOpenTodayChange: (Boolean) -> Unit,
    onPlace: (Place) -> Unit,
    onNotifyMe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val name = state.category?.label() ?: stringResource(R.string.kategori_unknown)
    ListScaffold(
        title = name,
        subtitle = stringResource(state.category.subtitleRes(), state.totalCount),
        headerColor = state.category?.tint(colors) ?: colors.backgroundWarm,
        heroBackground = state.category.heroBackground(),
        heroPieces = {
            state.category?.let { ArtPiece(it.art(), 110.dp, 250.dp, 30.dp, 0f) }
            ArtPiece(R.drawable.jelajah_art_pentas, 44.dp, 318.dp, 150.dp, -10f)
        },
        onBack = onBack,
        modifier = modifier,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KelanaChip(
                    label = stringResource(R.string.filter_all_categories),
                    selected = !state.openTodayOnly,
                    onClick = { onOpenTodayChange(false) },
                )
                KelanaChip(
                    label = stringResource(R.string.filter_open_today),
                    selected = state.openTodayOnly,
                    onClick = { onOpenTodayChange(true) },
                )
            }
        },
    ) {
        when {
            state.isCategoryEmpty -> KategoriEmpty(state.category, name, onNotifyMe)
            state.isFilteredEmpty -> EmptyState(
                title = stringResource(R.string.filter_empty_title),
                body = stringResource(R.string.filter_empty_body),
            )
            else -> PlaceList(state.places, state.today, onPlace)
        }
    }
}

@Composable
internal fun SemuaTempatContent(
    state: SemuaTempatUiState,
    onBack: () -> Unit,
    onCategoryChange: (PlaceCategory?) -> Unit,
    onOpenTodayChange: (Boolean) -> Unit,
    onPlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    ListScaffold(
        title = stringResource(R.string.semua_title),
        subtitle = stringResource(R.string.semua_subtitle, state.totalCount),
        headerColor = colors.backgroundWarm,
        heroBackground = R.drawable.jelajah_art_hero_semua,
        heroPieces = {
            ArtPiece(R.drawable.jelajah_art_semua, 90.dp, 262.dp, 36.dp, -6f)
            ArtPiece(R.drawable.jelajah_art_peta, 44.dp, 320.dp, 156.dp, 8f)
        },
        onBack = onBack,
        modifier = modifier,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KelanaChip(
                    label = stringResource(R.string.filter_all_categories),
                    selected = state.category == null,
                    onClick = { onCategoryChange(null) },
                )
                PlaceCategory.entries.forEach { category ->
                    KelanaChip(
                        label = category.label(),
                        selected = state.category == category,
                        onClick = { onCategoryChange(category) },
                    )
                }
                KelanaChip(
                    label = stringResource(R.string.filter_open_today),
                    selected = state.openTodayOnly,
                    onClick = { onOpenTodayChange(!state.openTodayOnly) },
                )
            }
        },
    ) {
        if (state.isEmpty) {
            EmptyState(
                title = stringResource(R.string.filter_empty_title),
                body = stringResource(R.string.filter_empty_body),
            )
        } else {
            PlaceList(state.places, state.today, onPlace)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun KategoriListPreview() {
    KelanaTheme {
        KategoriContent(
            state = KategoriUiState(PlaceCategory.Museum, false, false, 3, PreviewData.places),
            onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun KategoriEmptyPreview() {
    KelanaTheme {
        KategoriContent(
            state = KategoriUiState(PlaceCategory.Pentas, false),
            onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KategoriFilteredEmptyPreview() {
    KelanaTheme {
        KategoriContent(
            state = KategoriUiState(PlaceCategory.Museum, false, true, 3, emptyList()),
            onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SemuaTempatPreview() {
    KelanaTheme {
        SemuaTempatContent(
            state = SemuaTempatUiState(false, null, false, 3, PreviewData.places),
            onBack = {}, onCategoryChange = {}, onOpenTodayChange = {}, onPlace = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SemuaTempatEmptyPreview() {
    KelanaTheme {
        SemuaTempatContent(
            state = SemuaTempatUiState(false, PlaceCategory.Pentas, false, 3, emptyList()),
            onBack = {}, onCategoryChange = {}, onOpenTodayChange = {}, onPlace = {},
        )
    }
}

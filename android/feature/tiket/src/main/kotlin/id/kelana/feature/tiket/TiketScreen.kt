package id.kelana.feature.tiket

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.navigation.TiketRoute
import id.kelana.core.ui.formatRupiah
import java.time.LocalDate

/**
 * Registers this feature's destination. Called by :app only. [navigate] takes a route object from :core:navigation
 * (here `OfficialSiteRoute`, the "Ke situs tiket resmi" confirm sheet); [onBack] pops the back stack.
 */
fun NavGraphBuilder.tiketScreen(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<TiketRoute> { TiketScreen(navigate = navigate, onBack = onBack) }
}

@Composable
internal fun TiketScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: TiketViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    TiketContent(
        state = state,
        onBack = onBack,
        onSelectFilter = viewModel::selectFilter,
        onRetry = viewModel::retry,
        // "Buka situs" on the confirm sheet opens a Custom Tab; nothing is bought inside the app.
        onOpenSeller = { url, label -> navigate(OfficialSiteRoute(url = url, label = label)) },
    )
}

// Layout from docs/design/artifact/KatTiket.dc.html (screen 9.5), compared as code, not as a render.
@Composable
internal fun TiketContent(
    state: TiketUiState,
    onBack: () -> Unit,
    onSelectFilter: (TiketKind?) -> Unit,
    onRetry: () -> Unit,
    onOpenSeller: (url: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(KelanaTheme.colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = KelanaSpacing.lg),
    ) {
        item { TiketHero(onBack) }
        item { FilterRow(state.filter, onSelectFilter) }
        item { Banner(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 14.dp)) }
        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(KelanaSpacing.lg), contentAlignment = Alignment.Center) {
                    val loading = stringResource(R.string.tiket_loading)
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
                }
            }
            state.isError -> item {
                EmptyState(
                    title = stringResource(R.string.tiket_error_title),
                    body = stringResource(R.string.tiket_error_body),
                    actionLabel = stringResource(R.string.tiket_retry),
                    onAction = onRetry,
                )
            }
            state.isEmpty -> item {
                EmptyState(
                    title = stringResource(R.string.tiket_empty_title),
                    body = stringResource(R.string.tiket_empty_body),
                )
            }
            else -> items(state.items, key = { it.id }) { item ->
                TiketRow(
                    item = item,
                    onOpenSeller = onOpenSeller,
                    modifier = Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 10.dp),
                )
            }
        }
    }
}

// PROVISIONAL: the artifact hero is 220 dp tall with the stamp 110 dp at left 250 / top 30, rotated -8 degrees.
@Composable
private fun TiketHero(onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(colors.skyText),
    ) {
        Image(
            painter = painterResource(R.drawable.tiket_hero_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Image(
            painter = painterResource(R.drawable.tiket_art_stamp),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 250.dp, y = 30.dp)
                .size(110.dp)
                .rotate(-8f),
        )
        val back = stringResource(R.string.tiket_back_cd)
        // The artifact's round button is 44 dp; the touch target around it is the 48 dp minimum.
        Box(
            modifier = Modifier
                .padding(start = 14.dp, top = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = back },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(KelanaSizes.backButton).clip(CircleShape).background(colors.white),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.tiket_ic_back), contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp, end = 150.dp),
        ) {
            Text(
                text = stringResource(R.string.tiket_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 32.sp),
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.tiket_subtitle),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun FilterRow(selected: TiketKind?, onSelect: (TiketKind?) -> Unit) {
    val options: List<Pair<TiketKind?, Int>> = listOf(
        null to R.string.tiket_filter_all,
        TiketKind.Tempat to R.string.tiket_filter_place,
        TiketKind.Pentas to R.string.tiket_filter_show,
        TiketKind.Kelas to R.string.tiket_filter_class,
    )
    LazyRow(
        contentPadding = PaddingValues(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        items(options, key = { it.second }) { (kind, label) ->
            KelanaChip(label = stringResource(label), selected = selected == kind, onClick = { onSelect(kind) })
        }
    }
}

/** Green info card: KELANA sells nothing (artifact radius 16, padding 12/14, text 13/19). */
@Composable
private fun Banner(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.successSurface, KelanaShapes.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(painterResource(R.drawable.tiket_ic_shield), contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.tiket_banner),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = colors.successTextDeep,
        )
    }
}

private class RowArt(@param:DrawableRes val icon: Int, val tile: Color)

// PROVISIONAL mapping from category to the artifact's three row marks (tari on cream, blue hall on sky, craft on peach).
@Composable
private fun rowArt(category: PlaceCategory): RowArt {
    val colors = KelanaTheme.colors
    return when (category) {
        PlaceCategory.Tari -> RowArt(R.drawable.tiket_art_tari, colors.creamYellow)
        PlaceCategory.Kerajinan -> RowArt(R.drawable.tiket_art_batik, colors.peach)
        PlaceCategory.Museum, PlaceCategory.SeniMusik, PlaceCategory.Pentas ->
            RowArt(R.drawable.tiket_art_building_blue, colors.skyText)
        PlaceCategory.Candi, PlaceCategory.Lainnya -> RowArt(R.drawable.tiket_art_building, colors.backgroundWarm)
    }
}

@Composable
private fun TiketRow(
    item: TiketItem,
    onOpenSeller: (url: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val art = rowArt(item.category)
    val presentation = rowPresentation(item.onSite, item.sellerUrl)
    val url = item.sellerUrl.takeIf { presentation.opensSeller }
    val rowShape = KelanaShapes.listCard
    val openLabel = stringResource(R.string.tiket_open_action)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(colors.white)
            .border(1.dp, colors.outline, rowShape)
            .then(
                if (url != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = openLabel) { onOpenSeller(url, item.title) }
                } else {
                    Modifier
                },
            )
            .heightIn(min = 48.dp)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(KelanaSizes.listThumb)
                .background(art.tile, KelanaShapes.card),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(art.icon), contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val price = visiblePrice(item)?.let {
                if (it == 0L) stringResource(R.string.tiket_price_free) else stringResource(R.string.tiket_price_from, formatRupiah(it))
            }
            val subtitle = listOfNotNull(item.placeName, price).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            presentation.note?.let { note ->
                Text(
                    text = stringResource(
                        if (note == TiketNote.AlsoAtCounter) R.string.tiket_note_also_counter else R.string.tiket_note_buy_counter,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            PriceBadge(item.checkedAt, Modifier.padding(top = 2.dp))
        }
        if (url != null) {
            Image(painterResource(R.drawable.tiket_ic_open), contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** "Harga sedang dicek" in yellow when there is no check date; with a date nothing is shown (the date is not rendered). */
@Composable
private fun PriceBadge(checkedAt: LocalDate?, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    if (checkedAt == null) {
        Text(
            text = stringResource(R.string.tiket_price_pending),
            modifier = modifier
                .background(colors.creamYellow, KelanaShapes.badge)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.warnText,
        )
    }
}

private val PreviewChecked: LocalDate = LocalDate.of(2026, 11, 20)

private val PreviewItems = listOf(
    TiketItem("1", TiketKind.Pentas, "Sendratari Prambanan", PlaceCategory.Tari, "Candi Prambanan",
        "https://tiket.example.org/x", 150_000, PreviewChecked),
    TiketItem("2", TiketKind.Tempat, "Keraton Yogyakarta", PlaceCategory.Candi, null,
        "https://tiket.example.org/y", null, null),
    TiketItem("3", TiketKind.Kelas, "Kelas membatik", PlaceCategory.Kerajinan, null,
        "https://tiket.example.org/z", 90_000, PreviewChecked, onSite = true),
    TiketItem("4", TiketKind.Tempat, "Museum Sonobudoyo", PlaceCategory.Museum, null,
        null, 15_000, PreviewChecked, onSite = true),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun TiketLoadedPreview() {
    KelanaTheme { TiketContent(TiketUiState(isLoading = false, items = PreviewItems), {}, {}, {}, { _, _ -> }) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun TiketLoadingPreview() {
    KelanaTheme { TiketContent(TiketUiState(), {}, {}, {}, { _, _ -> }) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun TiketEmptyPreview() {
    KelanaTheme { TiketContent(TiketUiState(isLoading = false), {}, {}, {}, { _, _ -> }) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun TiketErrorPreview() {
    KelanaTheme { TiketContent(TiketUiState(isLoading = false, isError = true), {}, {}, {}, { _, _ -> }) }
}

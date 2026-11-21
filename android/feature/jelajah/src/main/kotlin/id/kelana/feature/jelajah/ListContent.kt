package id.kelana.feature.jelajah

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.ui.label

/**
 * Shared layout of Kategori and Semua tempat: illustrated header (PLACEHOLDER shape), chip row, list of places.
 * No frame PNG exists for these two screens; layout is taken from overview slide conv 9 and is PROVISIONAL.
 */
@Composable
private fun ListScaffold(
    title: String,
    subtitle: String,
    headerColor: Color,
    onBack: () -> Unit,
    chips: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxWidth().height(160.dp).background(headerColor)) {
            Box(
                Modifier.padding(KelanaSpacing.sm).size(48.dp).clip(CircleShape).background(colors.white)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back_cd),
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colors.ink),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(KelanaSpacing.gutter)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FrauncesFontFamily),
                    color = colors.ink,
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
        }
        Box(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md),
        ) { chips() }
        body()
    }
}

@Composable
private fun PlaceList(places: List<Place>, onPlace: (Place) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        items(places, key = { it.id }) { place -> PlaceItem(place = place, onClick = { onPlace(place) }) }
    }
}

@StringRes
private fun PlaceCategory?.subtitleRes(): Int = when (this) {
    PlaceCategory.Museum -> R.string.kategori_subtitle_museum
    PlaceCategory.Candi -> R.string.kategori_subtitle_candi
    PlaceCategory.Tari -> R.string.kategori_subtitle_tari
    PlaceCategory.Topeng -> R.string.kategori_subtitle_topeng
    PlaceCategory.Gamelan -> R.string.kategori_subtitle_gamelan
    else -> R.string.kategori_subtitle_other
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
        subtitle = stringResource(state.category.subtitleRes()),
        headerColor = state.category?.tint(colors) ?: colors.backgroundWarm,
        onBack = onBack,
        modifier = modifier,
        chips = {
            KelanaChip(
                label = stringResource(R.string.filter_open_today),
                selected = state.openTodayOnly,
                onClick = { onOpenTodayChange(!state.openTodayOnly) },
            )
        },
    ) {
        when {
            state.isCategoryEmpty -> EmptyState(
                title = stringResource(R.string.kategori_empty_title, name.lowercase()),
                body = stringResource(R.string.kategori_empty_body),
                actionLabel = stringResource(R.string.kabari_aku),
                onAction = onNotifyMe,
            )
            state.isFilteredEmpty -> EmptyState(
                title = stringResource(R.string.filter_empty_title),
                body = stringResource(R.string.filter_empty_body),
            )
            else -> PlaceList(state.places, onPlace)
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
        subtitle = stringResource(R.string.semua_subtitle),
        headerColor = colors.skyText,
        onBack = onBack,
        modifier = modifier,
        chips = {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm)) {
                KelanaChip(
                    label = stringResource(R.string.filter_open_today),
                    selected = state.openTodayOnly,
                    onClick = { onOpenTodayChange(!state.openTodayOnly) },
                )
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
            }
        },
    ) {
        if (state.isEmpty) {
            EmptyState(
                title = stringResource(R.string.filter_empty_title),
                body = stringResource(R.string.filter_empty_body),
            )
        } else {
            PlaceList(state.places, onPlace)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KategoriListPreview() {
    KelanaTheme {
        KategoriContent(
            state = KategoriUiState(PlaceCategory.Museum, false, false, 3, PreviewData.places),
            onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KategoriEmptyPreview() {
    KelanaTheme {
        KategoriContent(
            state = KategoriUiState(PlaceCategory.Topeng, false),
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

@Preview(showBackground = true)
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
            state = SemuaTempatUiState(false, PlaceCategory.Topeng, false, 3, emptyList()),
            onBack = {}, onCategoryChange = {}, onOpenTodayChange = {}, onPlace = {},
        )
    }
}

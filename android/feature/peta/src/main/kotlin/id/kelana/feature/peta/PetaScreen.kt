package id.kelana.feature.peta

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.PetaRoute
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Registers this feature's destination. Called by :app only. [navigate] takes a route object from
 * :core:navigation (here only `DetailTempatRoute`); [onBack] pops the back stack.
 */
fun NavGraphBuilder.petaScreen(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<PetaRoute> {
        val viewModel: PetaViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()
        PetaScreen(
            state = state,
            onBack = onBack,
            onFilterChange = viewModel::onFilterChange,
            onPinTap = viewModel::onPinTap,
            onRetry = viewModel::onRetry,
            onOpenDetail = { navigate(DetailTempatRoute(it)) },
        )
    }
}

@Composable
internal fun PetaScreen(
    state: PetaUiState,
    onBack: () -> Unit,
    onFilterChange: (PetaFilter) -> Unit,
    onPinTap: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(KelanaTheme.colors.background)) {
        when {
            state.isLoading -> {
                MapBackdrop()
                val loadingCd = stringResource(R.string.peta_loading_cd)
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = loadingCd },
                    color = KelanaTheme.colors.primary,
                )
            }
            state.hasError -> StateCard(
                title = stringResource(R.string.peta_error_title),
                body = stringResource(R.string.peta_error_body),
                actionLabel = stringResource(R.string.peta_retry),
                onAction = onRetry,
            )
            state.isEmpty -> StateCard(
                title = stringResource(R.string.peta_empty_title),
                body = stringResource(R.string.peta_empty_body),
            )
            else -> {
                PetaMap(pins = state.pins, selectedId = state.selectedId, onPinTap = onPinTap)
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selected = state.selected
                    val today = state.today
                    if (selected != null && today != null) {
                        PlaceCard(
                            place = selected,
                            today = today,
                            onOpenDetail = { onOpenDetail(selected.place.id) },
                            onOpenInMaps = { openInMapsApp(context, selected.place) },
                        )
                    }
                    HintBar(noMatch = state.pins.isEmpty())
                }
            }
        }
        TopRow(
            filter = state.filter,
            showFilters = !state.isLoading && !state.hasError && !state.isEmpty,
            onBack = onBack,
            onFilterChange = onFilterChange,
        )
    }
}

@Composable
private fun MapBackdrop() {
    Image(
        painter = painterResource(R.drawable.peta_map_bg),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
    )
}

/** Round back button and the three filter chips, over the map (KatPeta.dc.html top row, 12 dp inset). */
@Composable
private fun TopRow(
    filter: PetaFilter,
    showFilters: Boolean,
    onBack: () -> Unit,
    onFilterChange: (PetaFilter) -> Unit,
) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val backCd = stringResource(R.string.peta_back_cd)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack, role = Role.Button)
                .semantics { contentDescription = backCd },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(KelanaSizes.backButton).background(colors.white, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(painter = painterResource(R.drawable.peta_ic_back), contentDescription = null)
            }
        }
        if (showFilters) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PetaFilter.entries) { item ->
                    KelanaChip(
                        label = stringResource(item.labelRes()),
                        selected = item == filter,
                        onClick = { onFilterChange(item) },
                    )
                }
            }
        }
    }
}

private fun PetaFilter.labelRes(): Int = when (this) {
    PetaFilter.All -> R.string.peta_filter_all
    PetaFilter.OpenToday -> R.string.peta_filter_open_today
    PetaFilter.HasCap -> R.string.peta_filter_has_cap
}

/** Hint with the footnote: stamp icon, 13/18 text, translucent card (KatPeta.dc.html bottom bar). */
@Composable
private fun HintBar(noMatch: Boolean) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.95f), KelanaShapes.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.peta_hint_stamp),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
        )
        val footnote = stringResource(R.string.peta_footnote)
        Text(
            text = if (noMatch) {
                buildAnnotatedString {
                    append(stringResource(R.string.peta_filter_no_match))
                    append(" ")
                    withStyle(SpanStyle(color = colors.muted)) { append(footnote) }
                }
            } else {
                buildAnnotatedString {
                    append(stringResource(R.string.peta_hint))
                    append(" ")
                    withStyle(SpanStyle(color = colors.muted)) { append(footnote) }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink,
        )
    }
}

/** Empty and error states: the drawn map stays behind a centred message. */
@Composable
private fun StateCard(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        MapBackdrop()
        EmptyState(
            title = title,
            body = body,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .background(KelanaTheme.colors.background.copy(alpha = 0.95f), KelanaShapes.card),
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }
}

/**
 * "Buka di aplikasi peta": a `geo:` intent with the place's own coordinates. No user location is read. Does
 * nothing when no app can handle it.
 */
private fun openInMapsApp(context: Context, place: Place) {
    val uri = Uri.parse("geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}(${Uri.encode(place.name)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No maps app installed: nothing to open.
    }
}

// Previews --------------------------------------------------------------------------------------------------

private val PreviewDate: LocalDate = LocalDate.of(2026, 11, 20)

private fun previewPlace(id: String, name: String, category: PlaceCategory, lat: Double, lng: Double, checked: Boolean) = Place(
    id = id, slug = id, name = name, category = category, capCode = "YK-00", lat = lat, lng = lng,
    address = "", description = "", coverKey = id,
    hours = DayOfWeek.entries.map {
        OfficialFact(
            OpeningHours(it, isClosed = false, opens = LocalTime.of(9, 0), closes = LocalTime.of(15, 0)),
            sourceUrl = null,
            checkedAt = if (checked) PreviewDate else null,
        )
    },
)

private val PreviewPlaces = listOf(
    PetaPlace(previewPlace("a", "Candi Prambanan", PlaceCategory.Candi, -7.752, 110.491, true), openToday = true, showToday = true),
    PetaPlace(previewPlace("b", "Museum Sonobudoyo", PlaceCategory.Museum, -7.8013, 110.3641, true), openToday = true, showToday = false),
    PetaPlace(previewPlace("c", "Keraton Yogyakarta", PlaceCategory.Lainnya, -7.8053, 110.3642, false), openToday = false, showToday = false),
    PetaPlace(previewPlace("d", "Kotagede", PlaceCategory.Kerajinan, -7.8279, 110.3987, true), openToday = true, showToday = false),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaContentPreview() = KelanaTheme {
    PetaScreen(PetaUiState(isLoading = false, allPlaces = PreviewPlaces, today = PreviewDate), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaSelectedPreview() = KelanaTheme {
    PetaScreen(
        PetaUiState(isLoading = false, allPlaces = PreviewPlaces, selectedId = "a", today = PreviewDate),
        {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaCheckingPreview() = KelanaTheme {
    PetaScreen(
        PetaUiState(isLoading = false, allPlaces = PreviewPlaces, selectedId = "c", today = PreviewDate),
        {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaFilterNoMatchPreview() = KelanaTheme {
    PetaScreen(
        PetaUiState(isLoading = false, allPlaces = PreviewPlaces.map { it.copy(openToday = false) }, filter = PetaFilter.OpenToday, today = PreviewDate),
        {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaLoadingPreview() = KelanaTheme { PetaScreen(PetaUiState(), {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaEmptyPreview() = KelanaTheme {
    PetaScreen(PetaUiState(isLoading = false, today = PreviewDate), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaErrorPreview() = KelanaTheme {
    PetaScreen(PetaUiState(isLoading = false, hasError = true), {}, {}, {}, {}, {})
}

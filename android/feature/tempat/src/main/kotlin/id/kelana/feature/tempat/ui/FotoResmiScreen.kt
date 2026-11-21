package id.kelana.feature.tempat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.feature.tempat.R

/**
 * Official photos gallery. PROVISIONAL layout (no frame PNG): back bar, a note, a 2-column grid of placeholder
 * tiles derived from the cover key. Real official photos replace the tiles later.
 */
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
    Column(modifier.fillMaxSize()) {
        KelanaBackTopBar(title = stringResource(R.string.foto_resmi_title), onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(KelanaSpacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.foto_resmi_note),
                    modifier = Modifier.padding(bottom = KelanaSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = KelanaTheme.colors.muted,
                )
            }
            items((1..state.photoCount).toList()) { index ->
                CoverPlaceholder(
                    coverKey = state.coverKey + index,
                    description = stringResource(R.string.foto_resmi_item, index, state.photoCount, state.placeName),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
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

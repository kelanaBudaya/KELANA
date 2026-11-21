package id.kelana.feature.tempat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.tempat.R

/**
 * PROVISIONAL placeholder for an illustrated cover or an official photo: a navy gradient whose angle depends on
 * [coverKey]. The cover drawables are not in :core:designsystem yet, so no real illustration is drawn.
 */
@Composable
internal fun CoverPlaceholder(coverKey: String, modifier: Modifier = Modifier, description: String? = null) {
    val colors = KelanaTheme.colors
    val shift = (coverKey.hashCode().and(0x7fffffff) % 3)
    val (from, to) = when (shift) {
        0 -> colors.primaryDeep to colors.primaryBrightAlt
        1 -> colors.primaryDeepAlt to colors.stampBrown
        else -> colors.navy to colors.primaryBright
    }
    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(from, to)))
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    )
}

/** Full-screen state used by all three screens while loading. */
@Composable
internal fun LoadingBody(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.tempat_loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KelanaTheme.colors.primary)
    }
}

/** Full-screen not-found / error state with a back bar. */
@Composable
internal fun MessageScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize()) {
        KelanaBackTopBar(title = "", onBack = onBack)
        Box(Modifier.fillMaxSize().padding(bottom = 56.dp), contentAlignment = Alignment.Center) {
            EmptyState(title = title, body = body, actionLabel = actionLabel, onAction = onAction)
        }
    }
}

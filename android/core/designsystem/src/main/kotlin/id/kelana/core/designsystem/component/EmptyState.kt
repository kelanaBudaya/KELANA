package id.kelana.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

/**
 * Centered empty, offline or "no results" state: illustration slot, title, body and an optional action.
 * The caller draws the illustration (about 160 dp tall, PROVISIONAL; title 16/800, body 14/21 muted, pill action button) in [illustration].
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KelanaSpacing.lg, vertical = KelanaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        if (illustration != null) {
            Box(Modifier.heightIn(max = 160.dp).padding(bottom = KelanaSpacing.sm)) { illustration() }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            KelanaButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = KelanaSpacing.sm),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun EmptyStatePreview() {
    KelanaTheme {
        EmptyState(
            title = stringResource(R.string.ds_preview_empty_title),
            body = stringResource(R.string.ds_preview_empty_body),
            actionLabel = stringResource(R.string.ds_preview_button),
            onAction = {},
        )
    }
}

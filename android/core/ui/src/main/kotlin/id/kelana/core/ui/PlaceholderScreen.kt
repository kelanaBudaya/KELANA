package id.kelana.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

/**
 * Centered text shown by feature modules until their real screens are built.
 * With [onBack] a back top bar is shown above the text (for non-tab destinations).
 */
@Composable
fun PlaceholderScreen(text: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    Column(modifier = modifier.fillMaxSize()) {
        if (onBack != null) KelanaBackTopBar(title = text, onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = KelanaSpacing.gutter),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = KelanaTheme.colors.ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

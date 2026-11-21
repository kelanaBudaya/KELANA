package id.kelana.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

/** Simple top bar: optional text back action, then the title. */
@Composable
fun KelanaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KelanaSizes.topBarHeight)
            .padding(horizontal = KelanaSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.ds_back), color = colors.navy) }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
    }
}

@Preview(showBackground = true)
@Composable
private fun KelanaTopBarPreview() {
    KelanaTheme {
        KelanaTopBar(title = stringResource(R.string.ds_preview_card_title), onBack = {})
    }
}

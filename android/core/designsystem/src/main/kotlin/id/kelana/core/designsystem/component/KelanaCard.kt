package id.kelana.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme

@Composable
fun KelanaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KelanaTheme.colors
    Card(
        modifier = modifier,
        shape = KelanaShapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.white, contentColor = colors.ink),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Preview(showBackground = true)
@Composable
private fun KelanaCardPreview() {
    KelanaTheme {
        KelanaCard(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.ds_preview_card_title), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.ds_preview_card_body), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}

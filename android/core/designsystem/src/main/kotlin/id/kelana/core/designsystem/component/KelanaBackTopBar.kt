package id.kelana.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

/**
 * Top bar with an arrow back button and a title, for detail screens.
 * Arrow tap target 48 dp (accessibility minimum; the artifact's round back button is 44 dp, white), bar 56 dp PROVISIONAL.
 * Title 16/800 (titleMedium).
 */
@Composable
fun KelanaBackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KelanaSizes.topBarHeight)
            .padding(horizontal = KelanaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.ds_back),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = KelanaSpacing.xs),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun KelanaBackTopBarPreview() {
    KelanaTheme {
        KelanaBackTopBar(title = stringResource(R.string.ds_preview_card_title), onBack = {})
    }
}

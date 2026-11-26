package id.kelana.feature.rencana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.rencana.R

/**
 * Full-screen status layout of TanpaInternetIlustratif.dc.html (warm art panel, centred title and body), reused for the
 * planner's error states (build failed, no place data, plan not loaded, re-plan not loaded). The art defaults to the
 * offline illustration; [art] may swap it.
 */
@Composable
internal fun RencanaStatusContent(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: () -> Unit = {},
    art: @Composable BoxScope.() -> Unit = { StatusArt() },
) {
    val colors = KelanaTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 70.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(KelanaShapes.imagePanel)
                .background(colors.backgroundWarm),
            content = art,
        )
        Text(
            title,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 26.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
        )
        Text(
            body,
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = colors.muted,
        )
        KelanaButton(
            text = primaryText,
            onClick = onPrimary,
            variant = KelanaButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
        )
        if (secondaryText != null) {
            KelanaButton(
                text = secondaryText,
                onClick = onSecondary,
                variant = KelanaButtonVariant.Text,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.StatusArt() {
    ArtBlob(R.drawable.rencana_offline_blob)
    ArtMark(R.drawable.rencana_art_route, 90, 50, 50, -10f)
    ArtMark(R.drawable.rencana_art_bookmark, 64, 230, 60, 8f)
    ArtMark(R.drawable.rencana_veh_walk, 40, 170, 180, 190f)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StatusPreview() = KelanaTheme {
    RencanaStatusContent(
        title = stringResource(R.string.rencana_loaderror_title),
        body = stringResource(R.string.rencana_loaderror_body),
        primaryText = stringResource(R.string.rencana_retry),
        onPrimary = {},
        secondaryText = stringResource(R.string.rencana_status_back),
    )
}

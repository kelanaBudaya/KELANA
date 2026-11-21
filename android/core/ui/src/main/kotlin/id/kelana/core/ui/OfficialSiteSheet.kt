package id.kelana.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

/** Only http and https links are opened; anything else (intent:, file:, javascript:) is refused. */
fun isOpenableUrl(url: String): Boolean {
    val scheme = runCatching { Uri.parse(url).scheme }.getOrNull()?.lowercase()
    return scheme == "https" || scheme == "http"
}

/** Opens [url] in a Custom Tab. Returns false when the link is refused or no browser is available. */
fun openCustomTab(context: Context, url: String): Boolean {
    if (!isOpenableUrl(url)) return false
    return try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/**
 * Bottom sheet "Ke situs tiket resmi" (Figma 03.04). KELANA sells no tickets: "Buka situs" hands the user to the
 * official seller [label]. Copy and layout are PROVISIONAL (frame 03.04 was not exported when this was written).
 * :app shows it as a dialog destination for `OfficialSiteRoute`; features only navigate to that route.
 */
@Composable
fun OfficialSiteSheet(
    label: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                // Swallow taps so they do not reach the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .navigationBarsPadding()
                .padding(KelanaSpacing.gutter),
        ) {
            Text(
                text = stringResource(R.string.official_site_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.official_site_body, label),
                modifier = Modifier.padding(top = KelanaSpacing.sm, bottom = KelanaSpacing.lg),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            KelanaButton(
                text = stringResource(R.string.official_site_open),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
            KelanaButton(
                text = stringResource(R.string.official_site_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = KelanaButtonVariant.Text,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun OfficialSiteSheetPreview() {
    KelanaTheme {
        OfficialSiteSheet(label = stringResource(R.string.official_site_preview_label), onOpen = {}, onDismiss = {})
    }
}

package id.kelana.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.ui.R

/**
 * The map credit (OpenFreeMap, OpenMapTiles, OpenStreetMap contributors), which the licences require to stay
 * reachable from every map. It is a small "i" button, like the collapsed attribution of MapLibre itself; tapping it
 * shows the credit text and a link to the OpenStreetMap copyright page. Show it with every [OsmMap].
 */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    var open by remember { mutableStateOf(false) }
    val cd = stringResource(R.string.map_attribution_cd)
    // 48 dp touch target around a 32 dp circle.
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { open = true }
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(colors.white.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Text("i", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = colors.ink)
        }
    }
    if (open) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.map_attribution_title)) },
            text = { Text(stringResource(R.string.map_attribution)) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { uriHandler.openUri(ATTRIBUTION_URL) }
                    open = false
                }) { Text(stringResource(R.string.map_attribution_open)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.map_attribution_close)) } },
        )
    }
}

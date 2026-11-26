package id.kelana.core.ui.map

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import id.kelana.core.ui.R

/**
 * The map credit (OpenFreeMap, OpenMapTiles, OpenStreetMap contributors) with a link to the OpenStreetMap copyright
 * page. The maps themselves show no credit; the licences require it to stay reachable, so the app offers this dialog
 * from Profil > Pengaturan > Tentang peta.
 */
@Composable
fun MapCreditDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_attribution_title)) },
        text = { Text(stringResource(R.string.map_attribution)) },
        confirmButton = {
            TextButton(onClick = {
                runCatching { uriHandler.openUri(ATTRIBUTION_URL) }
                onDismiss()
            }) { Text(stringResource(R.string.map_attribution_open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_attribution_close)) } },
    )
}

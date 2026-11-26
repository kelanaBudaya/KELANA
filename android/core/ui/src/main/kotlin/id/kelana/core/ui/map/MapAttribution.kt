package id.kelana.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.ui.R

/** The OpenFreeMap / OpenMapTiles / OpenStreetMap attribution; opens the OSM copyright page (ODbL credit). Show it with every [OsmMap]. */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val cd = stringResource(R.string.map_attribution_cd)
    Text(
        text = stringResource(R.string.map_attribution),
        style = MaterialTheme.typography.labelSmall,
        color = KelanaTheme.colors.ink,
        modifier = modifier
            .clip(KelanaShapes.card)
            .background(KelanaTheme.colors.white.copy(alpha = 0.85f))
            .clickable(role = Role.Button) { runCatching { uriHandler.openUri(ATTRIBUTION_URL) } }
            .semantics { contentDescription = cd }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

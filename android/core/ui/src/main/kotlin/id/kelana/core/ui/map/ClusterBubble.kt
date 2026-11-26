package id.kelana.core.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.ui.R

internal val ClusterTouchSize = 48.dp
internal val ClusterDotSize = 44.dp

private const val CD_NAME_COUNT = 3
private const val COUNT_OVERFLOW_ABOVE = 99

/** Count bubble for a cluster: a filled circle in the shared accent with a white 3 dp border and a white count. */
@Composable
internal fun ClusterBubble(
    members: List<MapMarker>,
    opensList: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val n = members.size
    val accent = members.map { it.accent }.distinct().singleOrNull() ?: colors.primary
    val names = members.take(CD_NAME_COUNT).joinToString(", ") { it.description }
    val namesText = if (n > CD_NAME_COUNT) {
        stringResource(R.string.map_cluster_names_more, names, n - CD_NAME_COUNT)
    } else {
        names
    }
    val cd = pluralStringResource(R.plurals.map_cluster_cd, n, n, namesText)
    val actionLabel = stringResource(
        if (opensList) R.string.map_cluster_action_list else R.string.map_cluster_action_zoom,
    )
    Box(
        modifier = modifier
            .size(ClusterTouchSize)
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ClusterDotSize)
                .clip(CircleShape)
                .background(accent)
                .border(3.dp, colors.white, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (n > COUNT_OVERFLOW_ABOVE) stringResource(R.string.map_cluster_count_overflow) else n.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.white,
            )
        }
    }
}

/** Bottom sheet that lists members that cannot be split by zooming; tapping a row picks that marker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClusterListSheet(
    members: List<MapMarker>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KelanaTheme.colors
    val n = members.size
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.map_cluster_sheet_title, n, n),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            members.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { onPick(m.key) }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = painterResource(m.glyph),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = m.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

private fun previewMarker(i: Int, accent: androidx.compose.ui.graphics.Color) = MapMarker(
    key = "k$i",
    lat = 0.0,
    lng = 0.0,
    label = "P$i",
    description = "Place $i",
    glyph = R.drawable.ic_flag_id,
    accent = accent,
)

@Preview(showBackground = true)
@Composable
private fun ClusterBubbleMixedPreview() {
    KelanaTheme {
        val c = KelanaTheme.colors
        ClusterBubble(
            members = listOf(previewMarker(1, c.accentOrange), previewMarker(2, c.primary)),
            opensList = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClusterBubbleOverflowPreview() {
    KelanaTheme {
        val c = KelanaTheme.colors
        ClusterBubble(
            members = List(120) { previewMarker(it, c.accentOrange) },
            opensList = true,
            onClick = {},
        )
    }
}

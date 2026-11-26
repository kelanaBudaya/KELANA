package id.kelana.feature.rencana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.ui.label
import id.kelana.feature.rencana.R

/**
 * The "ransel": places added with "Masuk rencana" on Detail tempat. The artifact has no screen for it, so the rows
 * reuse the card look of the other Rencana cards (white card, ink title, muted detail line).
 */
@Composable
internal fun BackpackSection(
    places: List<Place>,
    onOpenPlace: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            pluralStringResource(R.plurals.rencana_backpack_title, places.size, places.size),
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        places.forEach { place ->
            val removeDescription = stringResource(R.string.rencana_backpack_remove_cd, place.name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KelanaShapes.card)
                    .background(colors.surface)
                    .clickable(role = Role.Button) { onOpenPlace(place.id) }
                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(place.name, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(
                        "${place.category.label()} · ${place.capCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.muted,
                    )
                }
                Text(
                    stringResource(R.string.rencana_backpack_remove),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onRemove(place.id) }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .semantics { contentDescription = removeDescription },
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.danger,
                )
            }
        }
    }
}

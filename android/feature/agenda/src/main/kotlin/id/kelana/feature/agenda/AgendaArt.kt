package id.kelana.feature.agenda

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Event
import id.kelana.core.model.EventTag
import id.kelana.core.model.PlaceCategory
import java.time.LocalDate

/** Icon and tile colour of an event row (AgendaIlustratif: tari on peach, other icons on sky or warm tiles). */
internal data class EventArt(@param:DrawableRes val icon: Int, val tile: Color)

/**
 * PROVISIONAL mapping: the artifact draws the tari mark for the sendratari row and a building mark for the
 * macapat and wayang wong rows. Wayang gets its own mark (used by the Wayang chip), everything else the building.
 */
@Composable
internal fun eventArt(event: Event): EventArt {
    val colors = KelanaTheme.colors
    return when {
        EventTag.Wayang in event.tags -> EventArt(R.drawable.agenda_art_wayang, colors.skyText)
        event.category == PlaceCategory.Tari -> EventArt(R.drawable.agenda_art_tari, colors.peach)
        else -> EventArt(R.drawable.agenda_art_building, colors.backgroundWarm)
    }
}

/** Green badge "Diperiksa 20 Nov 2026" (AgendaIlustratif: radius 4, padding 3/8, 12/600/16, check icon 14). */
@Composable
internal fun CheckedBadge(checkedAt: LocalDate, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val text = stringResource(R.string.agenda_checked, remember(checkedAt) { formatCheckedDate(checkedAt) })
    Row(
        modifier = modifier
            .background(colors.successSurface, KelanaShapes.badge)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.agenda_ic_check), contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.successText,
        )
    }
}

/** Pulls the element up over the previous sibling by [amount] (CSS `margin-top: -20px`). */
internal fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
}

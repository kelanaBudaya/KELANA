package id.kelana.feature.agenda

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Event
import id.kelana.core.model.EventTag
import id.kelana.core.model.PlaceCategory

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

/** Pulls the element up over the previous sibling by [amount] (CSS `margin-top: -20px`). */
internal fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
}

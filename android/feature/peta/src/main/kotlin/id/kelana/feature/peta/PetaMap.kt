package id.kelana.feature.peta

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import id.kelana.core.ui.map.MapMarker
import id.kelana.core.ui.map.OsmMap
import id.kelana.core.ui.map.pinLabel

@DrawableRes
private fun PinGlyph.drawable(): Int = when (this) {
    PinGlyph.Candi -> R.drawable.peta_glyph_candi
    PinGlyph.Museum -> R.drawable.peta_glyph_museum
    PinGlyph.Batik -> R.drawable.peta_glyph_batik
    PinGlyph.Building -> R.drawable.peta_glyph_keraton
}

/**
 * The shared OpenStreetMap view ([OsmMap], owned by :core:ui) with one tappable pin per place. The camera frames
 * [allPlaces] once, so a filter that hides places does not move the map.
 */
@Composable
internal fun PetaMap(
    allPlaces: List<PetaPlace>,
    pins: List<PetaPlace>,
    selectedId: String?,
    onPinTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val markers = remember(pins, colors) {
        pins.map { pin ->
            val place = pin.place
            MapMarker(
                key = place.id,
                lat = place.lat,
                lng = place.lng,
                label = pinLabel(place.name),
                description = place.name,
                glyph = place.category.glyph().drawable(),
                accent = if (place.category == PlaceCategory.Candi) colors.accentOrange else colors.primary,
            )
        }
    }
    val framed = remember(allPlaces) { allPlaces.map { it.place.lat to it.place.lng } }
    OsmMap(
        markers = markers,
        framedPoints = framed,
        selectedKey = selectedId,
        modifier = modifier,
        onMarkerTap = onPinTap,
    )
}

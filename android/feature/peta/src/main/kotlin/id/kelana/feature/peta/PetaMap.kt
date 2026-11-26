package id.kelana.feature.peta

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import id.kelana.core.ui.map.MapMarker
import id.kelana.core.ui.map.MarkerOverlap
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
    // Room taken by the overlays: the back button and chip row on top (12 + 48 dp + 12 dp margin below the status bar),
    // the hint card at the bottom (26 dp offset + about 64 dp card + 14 dp margin above the navigation bar).
    val contentPadding = PaddingValues(
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp,
    )
    OsmMap(
        contentPadding = contentPadding,
        markers = markers,
        framedPoints = framed,
        selectedKey = selectedId,
        modifier = modifier,
        onMarkerTap = onPinTap,
        overlap = MarkerOverlap.Cluster,
    )
}

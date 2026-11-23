package id.kelana.feature.peta

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory

/** Width of the pin box (drop plus label), PROVISIONAL, matches [PIN_MIN_DX_DP] minus a gap. */
private val PinWidth = 76.dp
private val DropSize = 44.dp

/**
 * The illustrated map (vector drawable stretched over the whole screen) with one tappable pin per place.
 * Pin positions come from [projectToUnit] and [layoutPins]; see PetaProjection.kt.
 */
@Composable
internal fun PetaMap(
    pins: List<PetaPlace>,
    selectedId: String?,
    onPinTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        Image(
            painter = painterResource(R.drawable.peta_map_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        val positions = remember(pins, widthDp, heightDp) {
            val seeds = pins.map {
                val p = projectToUnit(it.place.lat, it.place.lng)
                PinSeed(it.place.id, p.x, p.y)
            }
            layoutPins(seeds, widthDp, heightDp).associateBy { it.key }
        }
        // The selected pin is drawn last so it stays on top of its neighbours.
        val ordered = pins.sortedBy { it.place.id == selectedId }
        for (pin in ordered) {
            val pos = positions[pin.place.id] ?: continue
            MapPin(
                place = pin,
                selected = pin.place.id == selectedId,
                onClick = { onPinTap(pin.place.id) },
                modifier = Modifier.offset(x = (pos.xDp - PinWidth.value / 2).dp, y = (pos.yDp - DropSize.value / 2).dp),
            )
        }
    }
}

@DrawableRes
private fun PinGlyph.drawable(): Int = when (this) {
    PinGlyph.Candi -> R.drawable.peta_glyph_candi
    PinGlyph.Museum -> R.drawable.peta_glyph_museum
    PinGlyph.Batik -> R.drawable.peta_glyph_batik
    PinGlyph.Building -> R.drawable.peta_glyph_keraton
}

/** Teardrop pin (44 dp, 3 dp border, tip down) with a short label under it, as in KatPeta.dc.html. */
@Composable
internal fun MapPin(
    place: PetaPlace,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val accent = if (place.place.category == PlaceCategory.Candi) colors.accentOrange else colors.primary
    val selectedText = stringResource(R.string.peta_pin_selected)
    Column(
        modifier = modifier
            .width(PinWidth)
            .defaultMinSize(minHeight = 48.dp)
            .scale(if (selected) 1.12f else 1f)
            .clip(RoundedCornerShape(KelanaRadii.card))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = place.place.name
                this.selected = selected
                if (selected) stateDescription = selectedText
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DropSize)
                .graphicsLayer { rotationZ = -45f }
                .clip(DropShape)
                .background(if (selected) colors.skyText else colors.white)
                .border(3.dp, accent, DropShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(place.place.category.glyph().drawable()),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = 45f },
            )
        }
        Text(
            text = pinLabel(place.place.name),
            style = MaterialTheme.typography.labelSmall,
            color = colors.white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(KelanaRadii.pill))
                .background(accent)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** CSS `border-radius: 999px 999px 999px 4px`: round except the bottom-left corner, which becomes the tip. */
private val DropShape = RoundedCornerShape(
    topStart = 999.dp,
    topEnd = 999.dp,
    bottomEnd = 999.dp,
    bottomStart = 4.dp,
)

package id.kelana.core.ui.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import id.kelana.core.ui.R

/** One pin on the map: where it is, what it shows and its accent colour (border and label). */
data class MapMarker(
    val key: String,
    val lat: Double,
    val lng: Double,
    val label: String,
    val description: String,
    @DrawableRes val glyph: Int,
    val accent: Color,
)

/** Width of the pin box (drop plus label). */
internal val PinWidth = 76.dp
internal val DropSize = 44.dp

/** Teardrop pin (44 dp, 3 dp border, tip down) with a short label under it, as in KatPeta.dc.html. */
@Composable
internal fun MapPin(
    marker: MapMarker,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val selectedText = stringResource(R.string.map_pin_selected)
    Column(
        modifier = modifier
            .width(PinWidth)
            .defaultMinSize(minHeight = 48.dp)
            .scale(if (selected) 1.12f else 1f)
            .clip(RoundedCornerShape(KelanaRadii.card))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                if (onClick != null) role = Role.Button
                contentDescription = marker.description
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
                .border(3.dp, marker.accent, DropShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(marker.glyph),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = 45f },
            )
        }
        Text(
            text = marker.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(KelanaRadii.pill))
                .background(marker.accent)
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

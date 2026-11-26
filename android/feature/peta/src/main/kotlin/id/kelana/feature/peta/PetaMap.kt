package id.kelana.feature.peta

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
import id.kelana.core.model.PlaceCategory

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

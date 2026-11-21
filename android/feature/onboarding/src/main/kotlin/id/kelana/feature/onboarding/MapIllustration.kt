package id.kelana.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.RegionStatus

/**
 * PROVISIONAL placeholder for the Natural Earth map: simple blobs for Sumatera, Jawa, Kalimantan, Sulawesi
 * and Papua. The real map (MapLibre) comes later.
 */
@Composable
internal fun ArchipelagoBackdrop(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Canvas(modifier) {
        drawRect(colors.skyText)
        val land = colors.creamYellowAlt
        fun blob(x: Float, y: Float, w: Float, h: Float) = drawRoundRect(
            color = land,
            topLeft = Offset(size.width * x, size.height * y),
            size = Size(size.width * w, size.height * h),
            cornerRadius = CornerRadius(h * size.height / 2),
        )
        blob(0.0f, 0.38f, 0.22f, 0.18f) // Sumatera
        blob(0.34f, 0.40f, 0.24f, 0.07f) // Jawa
        blob(0.36f, 0.14f, 0.16f, 0.22f) // Kalimantan
        blob(0.58f, 0.18f, 0.10f, 0.26f) // Sulawesi
        blob(0.78f, 0.34f, 0.22f, 0.14f) // Papua
    }
}

/** PROVISIONAL positions by region code until Region carries coordinates. Unknown codes get a fallback spot. */
internal fun pinPositionFor(regionId: String, index: Int): PinPosition = when (regionId) {
    "DIY" -> PinPosition(0.48f, 0.43f)
    "JTG" -> PinPosition(0.42f, 0.41f)
    "BALI" -> PinPosition(0.58f, 0.45f)
    "JTM" -> PinPosition(0.53f, 0.42f)
    "JBR" -> PinPosition(0.36f, 0.43f)
    "SUMBAR" -> PinPosition(0.08f, 0.44f)
    "SULSEL" -> PinPosition(0.62f, 0.34f)
    "NTT" -> PinPosition(0.68f, 0.50f)
    "KALTIM" -> PinPosition(0.46f, 0.22f)
    "PAPUA" -> PinPosition(0.86f, 0.38f)
    else -> PinPosition(0.1f + 0.07f * (index % 10), 0.7f)
}

@Composable
internal fun statusLabel(status: RegionStatus): String = stringResource(
    when (status) {
        RegionStatus.Available -> R.string.status_available
        RegionStatus.Upcoming -> R.string.status_upcoming
        RegionStatus.NotOpen -> R.string.status_not_open
    },
)

@Composable
internal fun statusColor(status: RegionStatus): Color {
    val c = KelanaTheme.colors
    return when (status) {
        RegionStatus.Available -> c.accentOrange
        RegionStatus.Upcoming -> c.navy
        RegionStatus.NotOpen -> c.iconMuted
    }
}

/** Pins laid over the map. Each pin has a 48 dp touch target and a content description with name and status. */
@Composable
internal fun RegionPins(
    pins: List<RegionPin>,
    onPinTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        pins.forEach { pin ->
            val status = statusLabel(pin.region.status)
            val description = stringResource(R.string.pin_description, pin.region.name, status)
            Box(
                modifier = Modifier
                    .offset(maxWidth * pin.position.x - 24.dp, maxHeight * pin.position.y - 24.dp)
                    .size(48.dp)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    }
                    .clickable { onPinTapped(pin.region.id) },
                contentAlignment = Alignment.Center,
            ) {
                val size = if (pin.selected) 22.dp else 14.dp
                Box(
                    Modifier
                        .size(size)
                        .background(statusColor(pin.region.status), CircleShape),
                )
                if (pin.selected) Box(Modifier.size(7.dp).background(KelanaTheme.colors.white, CircleShape))
            }
        }
    }
}

package id.kelana.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus

/** Map drawing size in artifact units (PilihDaerah.dc.html: 350 x 210) and where it sits in the 360 tall panel. */
internal const val MapUnitsWidth = 350f
internal const val MapUnitsHeight = 210f
internal const val MapPanelHeightUnits = 360f
internal const val MapTopUnits = 65f // round((360 - 210) / 2) - 10

/** Artifact zoom levels (zoom index 0..2). */
internal val MapZoomScales = floatArrayOf(1f, 2f, 3.2f)

private val Spots = mapOf(
    "DIY" to PinPosition(118f, 144.1f, below = true),
    "BALI" to PinPosition(153.6f, 148.6f),
    "SUMUT" to PinPosition(32.5f, 67.1f),
    "JKT" to PinPosition(92f, 132.2f, left = true),
    "BDG" to PinPosition(97.6f, 137.5f),
    "JTM" to PinPosition(135.5f, 140f),
    "SULSEL" to PinPosition(184.9f, 124.3f),
    "KALTIM" to PinPosition(168f, 90f),
    "MALUT" to PinPosition(243.6f, 80.5f),
    "PAPUA" to PinPosition(342f, 105.2f, left = true),
    // PROVISIONAL: these sample regions are not in the artifact's list, so they sit near their neighbours.
    "JBR" to PinPosition(97.6f, 137.5f),
    "JTG" to PinPosition(126f, 141f),
    "SUMBAR" to PinPosition(26f, 80f),
    "NTT" to PinPosition(172f, 152f),
)

/** PROVISIONAL positions by region code until Region carries coordinates. Unknown codes get a fallback spot. */
internal fun pinPositionFor(regionId: String, index: Int): PinPosition =
    Spots[regionId] ?: PinPosition(20f + 25f * (index % 12), 175f)

/** Island of a region (artifact "Pulau ..." line), or null when unknown. PROVISIONAL until Region has an island. */
internal fun islandNameRes(regionId: String): Int? = when (regionId) {
    "DIY", "JTG", "JTM", "JBR", "JKT", "BDG" -> R.string.island_jawa
    "BALI", "NTT" -> R.string.island_bali_nusa
    "SUMUT", "SUMBAR" -> R.string.island_sumatera
    "SULSEL" -> R.string.island_sulawesi
    "KALTIM" -> R.string.island_kalimantan
    "MALUT" -> R.string.island_maluku
    "PAPUA" -> R.string.island_papua
    else -> null
}

@Composable
internal fun statusLabel(status: RegionStatus): String = stringResource(
    when (status) {
        RegionStatus.Available -> R.string.status_available
        RegionStatus.Upcoming -> R.string.status_upcoming
        RegionStatus.NotOpen -> R.string.status_not_open
    },
)

/** Pin and legend colors: `#C94D1A` available, `#24458F` soon, `#8C93A0` closed (PilihDaerah.dc.html COL). */
@Composable
internal fun statusColor(status: RegionStatus): Color {
    val c = KelanaTheme.colors
    return when (status) {
        RegionStatus.Available -> c.accentOrange
        RegionStatus.Upcoming -> c.primary
        RegionStatus.NotOpen -> c.iconMuted
    }
}

@Composable
internal fun islandLabel(region: Region): String? =
    islandNameRes(region.id)?.let { stringResource(R.string.island_label, stringResource(it)) }

/**
 * One pin. The 48 dp box is its accessibility bounds; the pin tip is at the position given by the caller (box centre +
 * 14 dp down). Selected: teardrop with halo, 22 dp. Others: 12 dp dot with a white edge (PilihDaerah.dc.html).
 *
 * The box does not handle touches itself: neighbouring pins (Jawa Tengah and DI Yogyakarta are about 17 dp apart) have
 * overlapping boxes, so the topmost box would win every tap. The map picks the pin nearest to the tap instead (see
 * [nearestPin]); TalkBack still gets a click action here.
 */
@Composable
internal fun RegionPinMarker(
    region: Region,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val fill = statusColor(region.status)
    val description = stringResource(R.string.pin_description, region.name, statusLabel(region.status))
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val halo = colors.ink.copy(alpha = 0.18f)
        val edge = colors.white
        Canvas(Modifier.size(48.dp)) {
            val d = density
            if (selected) {
                val c = Offset(24f * d, 19f * d)
                rotate(-45f, pivot = c) {
                    fun teardrop(half: Float) = Path().apply {
                        val r = CornerRadius(half)
                        addRoundRect(
                            RoundRect(
                                c.x - half, c.y - half, c.x + half, c.y + half,
                                topLeftCornerRadius = r, topRightCornerRadius = r, bottomRightCornerRadius = r,
                                bottomLeftCornerRadius = CornerRadius(3f * d),
                            ),
                        )
                    }
                    drawPath(teardrop(16f * d), halo)
                    drawPath(teardrop(11f * d), edge)
                    drawPath(teardrop(8.5f * d), fill)
                }
                drawCircle(edge, 3f * d, c)
            } else {
                val c = Offset(24f * d, 35f * d)
                drawCircle(edge, 6f * d, c)
                drawCircle(fill, 4f * d, c)
            }
        }
    }
}

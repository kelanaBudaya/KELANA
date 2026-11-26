package id.kelana.feature.rencana.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanStop
import id.kelana.core.model.TravelMode
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.formatTimeRange
import id.kelana.feature.rencana.R

internal enum class RencanaTone { Warm, Success, Sky, Peach }

/** Rounded 16 dp note with an icon, as the tip boxes of the artifact (13/19 text). */
@Composable
internal fun RencanaBanner(
    text: String,
    @DrawableRes iconRes: Int?,
    tone: RencanaTone,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val (background, content) = when (tone) {
        RencanaTone.Warm -> colors.creamYellow to colors.warnTextDeep
        RencanaTone.Success -> colors.successSurface to colors.successTextDeep
        RencanaTone.Sky -> colors.skyText to colors.navy
        RencanaTone.Peach -> colors.peach to colors.stampBrown
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (iconRes != null) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp), color = content)
    }
}

/** Icon of a category tile (the six Minat icons of the artifact). */
@DrawableRes
internal fun categoryArt(category: PlaceCategory, isEvent: Boolean): Int = when {
    isEvent -> R.drawable.rencana_cat_pentas
    category == PlaceCategory.Museum -> R.drawable.rencana_cat_museum
    category == PlaceCategory.Candi -> R.drawable.rencana_cat_candi
    category == PlaceCategory.Kerajinan -> R.drawable.rencana_cat_kerajinan
    category == PlaceCategory.Pentas -> R.drawable.rencana_cat_pentas
    category == PlaceCategory.Lainnya -> R.drawable.rencana_cat_keraton
    else -> R.drawable.rencana_cat_pentas
}

/** "Jalan kaki" / "Angkutan umum" / "Kendaraan pribadi" plus the estimate. Always marked as an estimate. */
@Composable
internal fun travelText(mode: TravelMode, minutes: Int): String {
    if (minutes <= 0) return stringResource(R.string.rencana_travel_adjacent)
    val label = stringResource(
        when (mode) {
            TravelMode.Walk -> R.string.rencana_travel_walk
            TravelMode.Transit -> R.string.rencana_travel_transit
            TravelMode.Car -> R.string.rencana_travel_car
            TravelMode.RideHailing -> R.string.rencana_travel_ride
        },
    )
    return stringResource(R.string.rencana_travel_estimate, label, minutes)
}

/** Opening hours of a place stop, or "sedang dicek" (the check date is not rendered). Shows nothing for a show. */
@Composable
internal fun hoursText(stop: PlanStop): String? {
    val fact = stop.hours ?: return null
    return when (val display = fact.display()) {
        is FactDisplay.Checked -> {
            val h = display.value
            val opens = h.opens
            val closes = h.closes
            if (h.isClosed || opens == null || closes == null) {
                stringResource(R.string.rencana_hours_checking)
            } else {
                stringResource(
                    R.string.rencana_hours_value,
                    formatTimeRange(opens, closes),
                )
            }
        }
        FactDisplay.Checking -> stringResource(R.string.rencana_hours_checking)
    }
}

/**
 * Price of a stop, or null when the stop has no ticket fact at all (free or not listed). A fact without check date, or
 * without a value, is "Harga sedang dicek" (AGENTS.md). A show with a checked fact but no price shows nothing.
 */
@Composable
internal fun priceText(stop: PlanStop): String? {
    val fact: OfficialFact<Long?> = stop.price ?: return null
    return when (val display = fact.display()) {
        is FactDisplay.Checked -> {
            val value = display.value
            when {
                value != null -> stringResource(R.string.rencana_price_value, formatRupiah(value))
                stop.isEvent -> null
                else -> stringResource(R.string.rencana_price_checking)
            }
        }
        FactDisplay.Checking -> stringResource(R.string.rencana_price_checking)
    }
}

/** Time label of a stop: the official show time , or the planned visit. */
@Composable
internal fun stopTimeText(stop: PlanStop): String {
    val schedule = stop.schedule
    if (schedule != null) {
        return when (val display = schedule.display()) {
            is FactDisplay.Checked -> formatTimeRange(display.value.start, display.value.end)
            FactDisplay.Checking -> stringResource(R.string.rencana_schedule_checking)
        }
    }
    return formatTimeRange(stop.start, stop.end)
}

/** "Jadwal 19.30–20.30" for a show stop, null for a place stop. */
@Composable
internal fun scheduleText(stop: PlanStop): String? {
    val schedule = stop.schedule ?: return null
    return when (val display = schedule.display()) {
        is FactDisplay.Checked -> stringResource(
            R.string.rencana_schedule_value,
            formatTimeRange(display.value.start, display.value.end),
        )
        FactDisplay.Checking -> stringResource(R.string.rencana_schedule_checking)
    }
}

@Composable
internal fun SmallFact(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = KelanaTheme.colors.muted)
}

/** Illustration blob that fills its panel (the artifact draws it at the panel size). */
@Composable
internal fun BoxScope.ArtBlob(@DrawableRes res: Int) {
    Image(painterResource(res), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
}

/**
 * One scattered mark of an illustration panel, placed like the artifact (`left`, `top`, size, rotation, all in dp).
 * The artifact's hard drop shadow (`drop-shadow(3px 4px 0 ...)`) is not drawn.
 */
@Composable
internal fun BoxScope.ArtMark(@DrawableRes res: Int, size: Int, x: Int, y: Int, rotation: Float = 0f) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.align(Alignment.TopStart).offset(x = x.dp, y = y.dp).size(size.dp).rotate(rotation),
    )
}

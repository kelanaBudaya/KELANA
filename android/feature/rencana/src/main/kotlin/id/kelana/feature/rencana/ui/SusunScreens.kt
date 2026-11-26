package id.kelana.feature.rencana.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Interest
import id.kelana.core.model.Pace
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.Vehicle
import id.kelana.core.ui.formatClock
import id.kelana.feature.rencana.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private val CardShape = RoundedCornerShape(20.dp)

// ---------------------------------------------------------------------------------------------------------------
// 1/6 Tanggal (SusunTanggal.dc.html)
// ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun SusunTanggalContent(
    state: SusunUiState,
    onPick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 1,
        title = stringResource(R.string.rencana_tanggal_title),
        hint = stringResource(R.string.rencana_tanggal_hint),
        buttonText = stringResource(R.string.rencana_next),
        canContinue = state.canContinueTanggal,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .fillMaxWidth()
                .clip(CardShape)
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.outline), CardShape)
                .padding(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMonthYear(state.visibleMonth.atDay(1)),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    color = colors.ink,
                )
                MonthArrow(R.drawable.rencana_ic_back, stringResource(R.string.rencana_prev_month), state.canGoPreviousMonth, onPreviousMonth)
                MonthArrow(R.drawable.rencana_ic_chevron, stringResource(R.string.rencana_next_month), true, onNextMonth)
            }
            val order = listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                order.forEach { day ->
                    Text(
                        text = weekdayShort(day),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
            MonthGrid(state, onPick)
        }
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .fillMaxWidth()
                .clip(KelanaShapes.card)
                .background(colors.skyText)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.rencana_calendar), contentDescription = null, modifier = Modifier.size(30.dp))
            Column {
                Text(
                    text = stringResource(
                        R.string.rencana_tanggal_summary,
                        formatRange(state.startDate, state.endDate),
                        state.dayCount,
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    color = colors.navy,
                )
                Text(
                    text = stringResource(R.string.rencana_tanggal_summary_hint, 7),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.navy,
                )
            }
        }
    }
}

@Composable
private fun MonthArrow(@DrawableRes icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            alpha = if (enabled) 1f else 0.3f,
        )
    }
}

@Composable
private fun MonthGrid(state: SusunUiState, onPick: (LocalDate) -> Unit) {
    val first = state.visibleMonth.atDay(1)
    val leading = first.dayOfWeek.value % 7 // Sunday first
    val days = state.visibleMonth.lengthOfMonth()
    val rows = (leading + days + 6) / 7
    Column(Modifier.padding(top = 4.dp)) {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - leading + 1
                    if (dayNumber < 1 || dayNumber > days) {
                        Box(Modifier.weight(1f).height(42.dp))
                    } else {
                        DayCell(state, state.visibleMonth.atDay(dayNumber), onPick, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(state: SusunUiState, date: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier) {
    val colors = KelanaTheme.colors
    val past = date.isBefore(state.today)
    val end = state.endDate
    val isStart = date == state.startDate
    val isEnd = end != null && date == end
    val inRange = end != null && date.isAfter(state.startDate) && date.isBefore(end)
    val endpoint = isStart || isEnd
    val label = formatDayNumberMonth(date)
    val stripColor = colors.peach
    Box(
        modifier = modifier
            .height(42.dp)
            .drawBehind {
                val half = size.width / 2
                when {
                    inRange -> drawRect(stripColor, Offset.Zero, size)
                    isStart && end != null && end != state.startDate -> drawRect(stripColor, Offset(half, 0f), Size(half, size.height))
                    isEnd -> drawRect(stripColor, Offset.Zero, Size(half, size.height))
                }
            }
            .semantics {
                contentDescription = label
                selected = endpoint || inRange
            }
            .clickable(enabled = !past) { onPick(date) },
        contentAlignment = Alignment.Center,
    ) {
        if (endpoint) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(colors.accentOrange), contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = colors.white)
            }
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (inRange) FontWeight.Bold else FontWeight.SemiBold),
                color = when {
                    past -> colors.iconMuted
                    inRange -> colors.stampBrown
                    else -> colors.ink
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 2/6 Teman (SusunRencanaIlustratif.dc.html, "Kamu berkelana bersama siapa?")
// ---------------------------------------------------------------------------------------------------------------

private data class CompanionArt(
    val group: TravelGroup,
    @param:DrawableRes val art: Int,
    val label: Int,
    val hint: Int,
)

private val CompanionOptions = listOf(
    CompanionArt(TravelGroup.Solo, R.drawable.rencana_traveler_solo, R.string.rencana_teman_solo, R.string.rencana_teman_solo_hint),
    CompanionArt(TravelGroup.Couple, R.drawable.rencana_traveler_couple, R.string.rencana_teman_couple, R.string.rencana_teman_couple_hint),
    CompanionArt(TravelGroup.Family, R.drawable.rencana_traveler_family, R.string.rencana_teman_family, R.string.rencana_teman_family_hint),
    CompanionArt(TravelGroup.Group, R.drawable.rencana_traveler_group, R.string.rencana_teman_group, R.string.rencana_teman_group_hint),
)

@Composable
internal fun SusunTemanContent(
    state: SusunUiState,
    onSelect: (TravelGroup) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 2,
        title = stringResource(R.string.rencana_teman_title),
        titleTop = 14,
        hint = stringResource(R.string.rencana_teman_hint),
        buttonText = stringResource(R.string.rencana_next),
        canContinue = state.canContinueTeman,
        onBack = onBack,
        onContinue = onContinue,
        topContent = {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 14.dp)
                    .height(30.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.skyText)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(painterResource(R.drawable.rencana_calendar), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = formatRange(state.startDate, state.endDate),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.navy,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompanionOptions.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { option ->
                        val selected = state.companion == option.group
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 150.dp)
                                .susunCard(selected, CardShape)
                                .singleChoice(selected) { onSelect(option.group) },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp)
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(Modifier.height(56.dp), contentAlignment = Alignment.BottomStart) {
                                    Image(painterResource(option.art), contentDescription = null, modifier = Modifier.size(width = 64.dp, height = 52.dp))
                                }
                                Column {
                                    Text(stringResource(option.label), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp), color = colors.ink)
                                    Text(stringResource(option.hint), modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = colors.muted)
                                }
                            }
                            if (selected) {
                                Image(
                                    painter = painterResource(R.drawable.rencana_check),
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (state.companion == TravelGroup.Family) {
            RencanaBanner(
                text = stringResource(R.string.rencana_teman_family_tip),
                iconRes = R.drawable.rencana_info,
                tone = RencanaTone.Warm,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 3/6 Minat (SusunMinat.dc.html)
// ---------------------------------------------------------------------------------------------------------------

private const val InterestColumns = 3
private val InterestGap = 10.dp

private data class InterestArt(val interest: Interest, @param:DrawableRes val art: Int, val label: Int)

private val InterestOptions = listOf(
    InterestArt(Interest.Semua, R.drawable.rencana_cat_semua, R.string.rencana_minat_semua),
    InterestArt(Interest.Museum, R.drawable.rencana_cat_museum, R.string.rencana_minat_museum),
    InterestArt(Interest.Candi, R.drawable.rencana_cat_candi, R.string.rencana_minat_candi),
    InterestArt(Interest.Pentas, R.drawable.rencana_cat_pentas, R.string.rencana_minat_pentas),
    InterestArt(Interest.Kerajinan, R.drawable.rencana_cat_kerajinan, R.string.rencana_minat_kerajinan),
    InterestArt(Interest.Keraton, R.drawable.rencana_cat_keraton, R.string.rencana_minat_keraton),
)

@Composable
internal fun SusunMinatContent(
    state: SusunUiState,
    onToggle: (Interest) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 3,
        title = stringResource(R.string.rencana_minat_title),
        hint = stringResource(R.string.rencana_minat_hint),
        buttonText = stringResource(R.string.rencana_next),
        canContinue = state.canContinueMinat,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Six cards in two rows of three, all the same width ("Semua jenis" is the first card).
            BoxWithConstraints {
                val cardWidth = (maxWidth - InterestGap * (InterestColumns - 1)) / InterestColumns
                Column(verticalArrangement = Arrangement.spacedBy(InterestGap)) {
                    InterestOptions.chunked(InterestColumns).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(InterestGap, Alignment.CenterHorizontally),
                        ) {
                            rowItems.forEach { option ->
                                val checked = option.interest in state.interests
                                Box(
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .height(112.dp)
                                        .susunCard(checked, CardShape)
                                        .multiChoice(checked) { onToggle(option.interest) },
                                ) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Image(painterResource(option.art), contentDescription = null, modifier = Modifier.size(48.dp))
                                        Text(
                                            stringResource(option.label),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = colors.ink,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    if (checked) {
                                        Image(
                                            painter = painterResource(R.drawable.rencana_check),
                                            contentDescription = null,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 4/6 Waktu (SusunWaktu.dc.html)
// ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun SusunWaktuContent(
    state: SusunUiState,
    onStart: (LocalTime) -> Unit,
    onEnd: (LocalTime) -> Unit,
    onPace: (Pace) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 4,
        title = stringResource(R.string.rencana_waktu_title),
        hint = stringResource(R.string.rencana_waktu_hint),
        buttonText = stringResource(R.string.rencana_next),
        canContinue = state.canContinueWaktu,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimeCard(stringResource(R.string.rencana_waktu_start), state.windowStart, onStart, Modifier.weight(1f))
            TimeCard(stringResource(R.string.rencana_waktu_end), state.windowEnd, onEnd, Modifier.weight(1f))
        }
        if (state.windowTooShort) {
            RencanaBanner(
                text = stringResource(R.string.rencana_waktu_too_short),
                iconRes = R.drawable.rencana_info,
                tone = RencanaTone.Warm,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Text(
                text = stringResource(R.string.rencana_waktu_pace_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.backgroundWarm)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Pace.entries.forEach { pace ->
                    val selected = state.pace == pace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.surface else Color.Transparent)
                            .singleChoice(selected) { onPace(pace) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(paceLabel(pace)),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
                            color = if (selected) colors.navy else colors.muted,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(CardShape)
                    .background(colors.creamYellow),
            ) {
                Image(painterResource(R.drawable.rencana_pace_blob), contentDescription = null, modifier = Modifier.fillMaxSize())
                Column(Modifier.padding(start = 16.dp, top = 18.dp, end = 140.dp)) {
                    Text(stringResource(paceLabel(state.pace)), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = colors.ink)
                    Text(stringResource(paceHint(state.pace)), style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp), color = colors.warnTextDeep)
                }
                // Two footprint marks as in the artifact (left 252/top 40, 44 dp, -8 deg; left 298/top 20, 36 dp, 6 deg).
                // The artifact's hard drop shadow (3px 4px 0) is not drawn.
                Image(
                    painter = painterResource(R.drawable.rencana_veh_walk),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-54).dp, y = 40.dp).size(44.dp).rotate(-8f),
                )
                Image(
                    painter = painterResource(R.drawable.rencana_veh_walk),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = 20.dp).size(36.dp).rotate(6f),
                )
            }
        }
    }
}

private fun paceLabel(pace: Pace): Int = when (pace) {
    Pace.Relaxed -> R.string.rencana_pace_relaxed
    Pace.Moderate -> R.string.rencana_pace_moderate
    Pace.Packed -> R.string.rencana_pace_packed
}

private fun paceHint(pace: Pace): Int = when (pace) {
    Pace.Relaxed -> R.string.rencana_pace_relaxed_hint
    Pace.Moderate -> R.string.rencana_pace_moderate_hint
    Pace.Packed -> R.string.rencana_pace_packed_hint
}

/** Label above two wheels (hour and minute). Wheel picker per user decision; the artifact shows a native time input. */
@Composable
private fun TimeCard(label: String, time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .clip(KelanaShapes.card)
            .background(colors.surface)
            .border(BorderStroke(1.5.dp, colors.outline), KelanaShapes.card)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colors.muted)
        TimeWheels(time, onChange, label, Modifier.padding(top = 6.dp))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 5/6 Kendaraan (SusunKendaraan.dc.html)
// ---------------------------------------------------------------------------------------------------------------

private data class VehicleArt(val vehicle: Vehicle, @param:DrawableRes val art: Int, val label: Int, val hint: Int, val artSize: Int)

private val VehicleOptions = listOf(
    VehicleArt(Vehicle.TransitAndWalk, R.drawable.rencana_veh_bus, R.string.rencana_vehicle_transit, R.string.rencana_vehicle_transit_hint, 50),
    // Ojek online: added by user decision, not in SusunKendaraan.dc.html.
    VehicleArt(Vehicle.RideHailing, R.drawable.rencana_veh_ojek, R.string.rencana_vehicle_ride, R.string.rencana_vehicle_ride_hint, 50),
    VehicleArt(Vehicle.PrivateVehicle, R.drawable.rencana_veh_car, R.string.rencana_vehicle_private, R.string.rencana_vehicle_private_hint, 52),
    VehicleArt(Vehicle.WalkOnly, R.drawable.rencana_veh_walk, R.string.rencana_vehicle_walk, R.string.rencana_vehicle_walk_hint, 46),
)

@Composable
internal fun SusunKendaraanContent(
    state: SusunUiState,
    onSelect: (Vehicle) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 5,
        title = stringResource(R.string.rencana_kendaraan_title),
        hint = stringResource(R.string.rencana_kendaraan_hint),
        buttonText = stringResource(R.string.rencana_next),
        canContinue = true,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VehicleOptions.forEach { option ->
                val selected = state.vehicle == option.vehicle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .susunCard(selected, CardShape)
                        .singleChoice(selected) { onSelect(option.vehicle) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(option.art), contentDescription = null, modifier = Modifier.size(option.artSize.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(option.label), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                        Text(
                            stringResource(option.hint),
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                    if (selected) {
                        Image(painterResource(R.drawable.rencana_check), contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 6/6 Kebutuhan (SusunKebutuhan.dc.html)
// ---------------------------------------------------------------------------------------------------------------

private data class NeedArt(val need: SpecialNeed, @param:DrawableRes val art: Int, val label: Int, val artSize: Int)

private val NeedOptions = listOf(
    NeedArt(SpecialNeed.Wheelchair, R.drawable.rencana_need_wheelchair, R.string.rencana_need_wheelchair, 40),
    NeedArt(SpecialNeed.AvoidStairs, R.drawable.rencana_need_stairs, R.string.rencana_need_stairs, 36),
    NeedArt(SpecialNeed.MoreRests, R.drawable.rencana_calendar, R.string.rencana_need_rests, 36),
)

@Composable
internal fun SusunKebutuhanContent(
    state: SusunUiState,
    onToggle: (SpecialNeed) -> Unit,
    onNone: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = KelanaTheme.colors
    SusunScaffold(
        step = 6,
        title = stringResource(R.string.rencana_kebutuhan_title),
        hint = stringResource(R.string.rencana_kebutuhan_hint),
        buttonText = stringResource(R.string.rencana_kebutuhan_submit),
        canContinue = state.canSubmit,
        onBack = onBack,
        onContinue = onSubmit,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NeedOptions.forEach { option ->
                val checked = option.need in state.needs
                NeedRow(option.art, option.artSize, stringResource(option.label), checked) { onToggle(option.need) }
            }
            NeedRow(R.drawable.rencana_info, 36, stringResource(R.string.rencana_need_none), state.needs.isEmpty()) { onNone() }
        }
        RencanaBanner(
            text = stringResource(R.string.rencana_kebutuhan_tip),
            iconRes = R.drawable.rencana_need_shield,
            tone = RencanaTone.Success,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
    }
}

@Composable
private fun NeedRow(@DrawableRes art: Int, artSize: Int, label: String, checked: Boolean, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .susunCard(checked, shape)
            .multiChoice(checked) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(art), contentDescription = null, modifier = Modifier.size(artSize.dp))
        }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp), color = colors.ink)
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = colors.accentOrange, checkmarkColor = colors.white),
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews (PROVISIONAL sample states; not data of the app)
// ---------------------------------------------------------------------------------------------------------------

private val PreviewToday: LocalDate = LocalDate.of(2026, 11, 20)

private fun previewState() = SusunUiState(
    today = PreviewToday,
    visibleMonth = YearMonth.of(2026, 11),
    startDate = PreviewToday,
    endDate = PreviewToday.plusDays(2),
    companion = TravelGroup.Family,
    interests = setOf(Interest.Museum, Interest.Pentas, Interest.Keraton),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TanggalPreview() = KelanaTheme { SusunTanggalContent(previewState(), {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TemanPreview() = KelanaTheme { SusunTemanContent(previewState(), {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MinatPreview() = KelanaTheme { SusunMinatContent(previewState(), {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WaktuPreview() = KelanaTheme { SusunWaktuContent(previewState(), {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KendaraanPreview() = KelanaTheme { SusunKendaraanContent(previewState(), {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KebutuhanPreview() = KelanaTheme {
    SusunKebutuhanContent(previewState().copy(needs = setOf(SpecialNeed.Wheelchair, SpecialNeed.MoreRests)), {}, {}, {}, {})
}

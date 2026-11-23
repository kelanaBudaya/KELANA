package id.kelana.feature.rencana.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.VerifiedBadge
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlanStop
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TravelMode
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PengingatRoute
import id.kelana.core.navigation.PetaRuteRoute
import id.kelana.core.navigation.SusunGraphRoute
import id.kelana.core.navigation.TempatTutupRoute
import id.kelana.core.ui.formatClock
import id.kelana.core.ui.formatTimeRange
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.rencana.R
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.ClosedStop
import java.time.LocalTime

@Composable
internal fun RencanaScreen(navigate: (Any) -> Unit, viewModel: RencanaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    RencanaContent(
        state = state,
        onEdit = { navigate(SusunGraphRoute) },
        onOpenRoute = { navigate(PetaRuteRoute) },
        onOpenClosed = { closed -> navigate(TempatTutupRoute(closed.stop.placeId, state.plan?.date.toString())) },
        onTakeStamp = { requireLogin { navigate(PasporRoute) } },
        onRemind = { eventId -> requireLogin { navigate(PengingatRoute(eventId)) } },
        onOpenEvent = { navigate(DetailPentasRoute(it)) },
    )
}

// Layout from RencanaHariIniIlustratif.dc.html (filled) and RencanaKosongIlustratif.dc.html (empty), compared as code.
@Composable
internal fun RencanaContent(
    state: RencanaUiState,
    onEdit: () -> Unit,
    onOpenRoute: () -> Unit,
    onOpenClosed: (ClosedStop) -> Unit,
    onTakeStamp: () -> Unit,
    onRemind: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val plan = state.plan
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    val loading = stringResource(R.string.rencana_loading)
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
                }
            }
            return@LazyColumn
        }
        if (plan == null) {
            item { EmptyPlan(state, onEdit, onOpenEvent) }
            return@LazyColumn
        }
        item { PlanHeader(state, onEdit) }
        if (state.isOffline) {
            item {
                RencanaBanner(
                    text = stringResource(R.string.rencana_offline_banner),
                    iconRes = R.drawable.rencana_info,
                    tone = RencanaTone.Sky,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
        }
        state.closedStops.firstOrNull()?.let { closed ->
            item { ClosedPrompt(closed, onOpenClosed, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) }
        }
        if (plan.stops.isNotEmpty()) {
            item { RouteCard(plan.stops.size, state.isToday, onOpenRoute) }
            item { Timeline(state, plan.stops, onTakeStamp, onRemind) }
            plan.stops.firstOrNull { it.isEvent }?.let { show ->
                val minAdvice = show.arrivalAdviceMin
                val maxAdvice = show.arrivalAdviceMax
                if (minAdvice != null && maxAdvice != null && show.schedule?.checkedAt != null) {
                    item {
                        RencanaBanner(
                            text = stringResource(
                                R.string.rencana_arrival_advice,
                                show.placeName,
                                formatClockText(show.schedule!!.value.start.minusMinutes(maxAdvice.toLong())),
                                minAdvice,
                                maxAdvice,
                            ),
                            iconRes = R.drawable.rencana_need_stairs,
                            tone = RencanaTone.Peach,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.rencana_day_empty),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
        if (plan.noShowFound) {
            item {
                RencanaBanner(
                    text = stringResource(R.string.rencana_no_show),
                    iconRes = R.drawable.rencana_info,
                    tone = RencanaTone.Warm,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }
        }
        if (plan.skipped.isNotEmpty()) {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.rencana_skipped_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.ink,
                    )
                    plan.skipped.forEach { skipped ->
                        val reason = stringResource(skipReasonRes(skipped.reason))
                        SmallFact(
                            if (skipped.detail != null) "${skipped.placeName}: $reason (${skipped.detail})" else "${skipped.placeName}: $reason",
                        )
                    }
                }
            }
        }
    }
}

private fun formatClockText(time: LocalTime): String = formatClock(time)

@Composable
private fun PlanHeader(state: RencanaUiState, onEdit: () -> Unit) {
    val colors = KelanaTheme.colors
    val plan = state.plan ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(if (state.isToday) R.string.rencana_title_today else R.string.rencana_title_upcoming),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            val who = state.companion?.let { stringResource(companionLabelRes(it)).lowercase() }
            Text(
                text = if (who != null) "${formatDayLong(plan.date)} · $who" else formatDayLong(plan.date),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Box(
            modifier = Modifier
                .size(KelanaSizes.inputHeight)
                .clip(CircleShape)
                .background(colors.backgroundWarm)
                .clickable(role = Role.Button, onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.rencana_edit),
                contentDescription = stringResource(R.string.rencana_edit),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

internal fun companionLabelRes(group: TravelGroup): Int = when (group) {
    TravelGroup.Solo -> R.string.rencana_teman_solo
    TravelGroup.Couple -> R.string.rencana_teman_couple
    TravelGroup.Family -> R.string.rencana_teman_family
    TravelGroup.Group -> R.string.rencana_teman_group
}

@Composable
private fun ClosedPrompt(closed: ClosedStop, onOpen: (ClosedStop) -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.peach)
            .clickable(role = Role.Button) { onOpen(closed) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.rencana_closed_prompt_title, closed.stop.placeName),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.stampBrown,
            )
            Text(
                stringResource(R.string.rencana_closed_prompt_action),
                style = MaterialTheme.typography.labelMedium,
                color = colors.stampBrown,
            )
        }
        Image(painterResource(R.drawable.rencana_ic_chevron), contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/** "RUTE HARI INI" card. PROVISIONAL art: the artifact draws a map blob with scattered marks; a flat navy panel here. */
@Composable
private fun RouteCard(count: Int, isToday: Boolean, onOpen: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 14.dp)
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShapeBig)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        ArtBlob(R.drawable.rencana_route_blob)
        ArtMark(R.drawable.rencana_art_route, 62, 268, 18, 8f)
        ArtMark(R.drawable.rencana_cat_candi, 54, 212, 80, -4f)
        ArtMark(R.drawable.rencana_veh_walk, 30, 168, 40, 200f)
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 18.dp).width(180.dp)) {
            Text(
                stringResource(if (isToday) R.string.rencana_route_label_today else R.string.rencana_route_label_upcoming),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                color = colors.creamYellow,
            )
            Text(
                stringResource(if (isToday) R.string.rencana_route_headline else R.string.rencana_route_headline_upcoming, count),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 25.sp, fontSize = 20.sp),
                color = colors.white,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .height(32.dp)
                .clip(KelanaShapes.chip)
                .background(colors.white)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.rencana_route_open_map), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = colors.navy)
        }
    }
}

private val RoundedCornerShapeBig = RoundedCornerShape(24.dp)

@Composable
private fun Timeline(
    state: RencanaUiState,
    stops: List<PlanStop>,
    onTakeStamp: () -> Unit,
    onRemind: (String) -> Unit,
) {
    val colors = KelanaTheme.colors
    val dash = colors.divider
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 18.dp)
            .drawBehind {
                // dashed spine behind the number circles (artifact: left 32, 2 dp dashed)
                drawLine(
                    color = dash,
                    start = Offset(17.dp.toPx(), 40.dp.toPx()),
                    end = Offset(17.dp.toPx(), size.height - 40.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                )
            },
    ) {
        stops.forEachIndexed { index, stop ->
            val travel = stop.travel
            if (index > 0 && travel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(34.dp).background(colors.background), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(
                                when (travel.mode) {
                                    TravelMode.Walk -> R.drawable.rencana_veh_walk
                                    TravelMode.Transit -> R.drawable.rencana_veh_bus
                                    TravelMode.Car -> R.drawable.rencana_veh_car
                                },
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).then(if (travel.mode == TravelMode.Walk) Modifier.rotate(180f) else Modifier),
                        )
                    }
                    Text(travelText(travel.mode, travel.minutes), style = MaterialTheme.typography.bodySmall, color = colors.muted)
                }
            }
            StopRow(
                number = index + 1,
                stop = stop,
                done = state.isToday && state.now >= stop.end,
                current = stop.key == state.currentKey,
                reminder = stop.eventId?.let { state.reminders[it] },
                onTakeStamp = onTakeStamp,
                onRemind = onRemind,
            )
        }
    }
}

@Composable
private fun StopRow(
    number: Int,
    stop: PlanStop,
    done: Boolean,
    current: Boolean,
    reminder: ReminderChoice?,
    onTakeStamp: () -> Unit,
    onRemind: (String) -> Unit,
) {
    val colors = KelanaTheme.colors
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier.fillMaxWidth().then(if (done) Modifier.alpha(0.72f) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.width(34.dp).padding(top = 14.dp).background(colors.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (done) {
                Image(painterResource(R.drawable.rencana_check), contentDescription = stringResource(R.string.rencana_stop_done), modifier = Modifier.size(30.dp))
            } else {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(if (current) colors.accentOrange else colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(number.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = colors.white)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .background(colors.surface)
                .border(if (current) 2.dp else 1.dp, if (current) colors.accentOrange else colors.outline, cardShape)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(KelanaShapes.card)
                        .background(if (stop.isEvent) colors.creamYellow else colors.skyText),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(categoryArt(stop.category, stop.isEvent)), contentDescription = null, modifier = Modifier.size(40.dp))
                }
                Column(Modifier.weight(1f)) {
                    val timeLabel = stopTimeText(stop)
                    Text(
                        text = if (current) stringResource(R.string.rencana_stop_now, timeLabel) else timeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = when {
                            current -> colors.accentOrange
                            done -> colors.successText
                            stop.isEvent -> colors.primary
                            else -> colors.successText
                        },
                    )
                    Text(stop.eventTitle ?: stop.placeName, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    if (stop.isEvent) {
                        Text(stop.placeName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = colors.muted)
                    }
                }
            }
            val facts = listOfNotNull(if (stop.isEvent) scheduleText(stop) else hoursText(stop), priceText(stop))
            facts.forEach { SmallFact(it, Modifier.padding(top = 6.dp)) }
            if (stop.accessUnverified) {
                SmallFact(stringResource(R.string.rencana_access_unverified), Modifier.padding(top = 4.dp))
            }
            if (stop.isEvent) {
                val eventId = stop.eventId.orEmpty()
                val remindText = reminder?.let { stringResource(reminderLabelRes(it.timing)) }
                Text(
                    text = remindText?.let { stringResource(R.string.rencana_reminder_set, it) } ?: stringResource(R.string.rencana_reminder_action),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .heightIn(min = 40.dp)
                        .clickable(role = Role.Button) { onRemind(eventId) }
                        .padding(vertical = 10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
            }
            if (current && !stop.isEvent) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(KelanaShapes.button)
                        .background(colors.stampBrown)
                        .clickable(role = Role.Button, onClick = onTakeStamp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(painterResource(R.drawable.rencana_check), contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            stringResource(R.string.rencana_take_stamp, stop.capCode),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = colors.white,
                        )
                    }
                }
            }
        }
    }
}

internal fun reminderLabelRes(timing: ReminderTiming): Int = when (timing) {
    ReminderTiming.WhenToLeave -> R.string.rencana_reminder_when_to_leave
    ReminderTiming.OneHourBefore -> R.string.rencana_reminder_one_hour
    ReminderTiming.ThirtyMinutesBefore -> R.string.rencana_reminder_thirty
}

@Composable
private fun EmptyPlan(state: RencanaUiState, onStart: () -> Unit, onOpenEvent: (String) -> Unit) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.rencana_title_empty),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShapeBig)
                .background(colors.peach),
        ) {
            ArtBlob(R.drawable.rencana_kosong_blob)
            ArtMark(R.drawable.rencana_art_route, 92, 30, 40, -10f)
            ArtMark(R.drawable.rencana_cat_candi, 96, 146, 74)
            ArtMark(R.drawable.rencana_art_ticket, 58, 256, 40, 14f)
            ArtMark(R.drawable.rencana_veh_walk, 46, 256, 180, 170f)
        }
        Text(
            stringResource(R.string.rencana_empty_title),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.ink,
        )
        Text(
            stringResource(R.string.rencana_empty_body),
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = colors.muted,
        )
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(KelanaShapes.input)
                .background(colors.accentOrange)
                .clickable(role = Role.Button, onClick = onStart),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.rencana_add), contentDescription = null, modifier = Modifier.size(19.dp))
            Text(
                stringResource(R.string.rencana_empty_cta),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = colors.white,
            )
        }
        state.tonight?.let { tonight -> TonightCard(tonight, onOpenEvent) }
    }
}

@Composable
private fun TonightCard(occurrence: EventOccurrence, onOpen: (String) -> Unit) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    val schedule: OfficialFact<EventSchedule> = event.schedule
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.creamYellow)
            .clickable(role = Role.Button) { onOpen(event.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(R.drawable.rencana_cat_pentas), contentDescription = null, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.rencana_tonight_title, event.title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Text(
                "${event.placeName} · ${formatTimeRange(schedule.value.start, schedule.value.end)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.warnTextDeep,
            )
            schedule.checkedAt?.let { VerifiedBadge(it, Modifier.padding(top = 2.dp)) }
        }
        Image(painterResource(R.drawable.rencana_ic_chevron), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.warnTextDeep))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// TanpaInternetIlustratif.dc.html
// ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun RencanaOfflineScreen(
    onOpenPlan: () -> Unit,
    onOpenPaspor: () -> Unit,
    viewModel: RencanaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    RencanaOfflineContent(hasPlan = state.plan != null, onOpenPlan = onOpenPlan, onOpenPaspor = onOpenPaspor)
}

@Composable
internal fun RencanaOfflineContent(hasPlan: Boolean, onOpenPlan: () -> Unit, onOpenPaspor: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 70.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(KelanaShapes.imagePanel)
                .background(colors.backgroundWarm),
        ) {
            ArtBlob(R.drawable.rencana_offline_blob)
            ArtMark(R.drawable.rencana_art_route, 90, 50, 50, -10f)
            ArtMark(R.drawable.rencana_art_bookmark, 64, 230, 60, 8f)
            ArtMark(R.drawable.rencana_veh_walk, 40, 170, 180, 190f)
        }
        Text(
            stringResource(R.string.rencana_offline_title),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 26.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
        )
        Text(
            stringResource(if (hasPlan) R.string.rencana_offline_body else R.string.rencana_offline_body_no_plan),
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = colors.muted,
        )
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPlan) {
                OfflineLink(R.drawable.rencana_art_route, colors.skyText, stringResource(R.string.rencana_offline_link_plan), stringResource(R.string.rencana_offline_link_plan_hint), onOpenPlan)
            }
            OfflineLink(R.drawable.rencana_check, colors.successSurface, stringResource(R.string.rencana_offline_link_paspor), stringResource(R.string.rencana_offline_link_paspor_hint), onOpenPaspor)
        }
        Text(
            stringResource(R.string.rencana_offline_stamp_note),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.muted,
        )
    }
}

@Composable
private fun OfflineLink(art: Int, tile: Color, title: String, hint: String, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(KelanaSizes.listThumb).clip(KelanaShapes.card).background(tile), contentAlignment = Alignment.Center) {
            Image(painterResource(art), contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Image(painterResource(R.drawable.rencana_ic_chevron), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.muted))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews (PROVISIONAL sample data, never shown in the app)
// ---------------------------------------------------------------------------------------------------------------

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun FilledPreview() = KelanaTheme { RencanaContent(PreviewRencana.filled(), {}, {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun OfflinePlanPreview() = KelanaTheme { RencanaContent(PreviewRencana.filled().copy(isOffline = true), {}, {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun EmptyPreview() = KelanaTheme { RencanaContent(PreviewRencana.empty(), {}, {}, {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OfflineScreenPreview() = KelanaTheme { RencanaOfflineContent(hasPlan = true, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun ClosedPromptPreview() = KelanaTheme {
    ClosedPrompt(
        ClosedStop(PreviewRencana.filled().plan!!.stops.first(), Availability.ClosedThatDay),
        {},
        Modifier.padding(16.dp),
    )
}

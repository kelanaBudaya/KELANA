package id.kelana.feature.rencana.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.VerifiedBadge
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Closure
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlanStop
import id.kelana.core.ui.formatClock
import id.kelana.feature.rencana.R
import id.kelana.feature.rencana.domain.Availability
import java.time.LocalDate

@Composable
internal fun TempatTutupScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onChooseOtherDay: () -> Unit,
    onOpenPaspor: () -> Unit,
    viewModel: TempatTutupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.outcome) {
        when (state.outcome) {
            TempatTutupOutcome.Done -> onDone()
            TempatTutupOutcome.GoToWizard -> onChooseOtherDay()
            // Signal was lost while applying: [TempatTutupUiState.isOffline] flips and the offline state shows.
            TempatTutupOutcome.GoOffline -> Unit
            null -> Unit
        }
        if (state.outcome != null) viewModel.consumeOutcome()
    }
    LaunchedEffect(state.notFound) { if (state.notFound) onBack() }
    TempatTutupContent(state, onBack, viewModel::select, viewModel::apply, onOpenPaspor)
}

// Layout from TempatTutupIlustratif.dc.html. The "CONTOH SKENARIO" label of the artifact is dropped: this is real data.
@Composable
internal fun TempatTutupContent(
    state: TempatTutupUiState,
    onBack: () -> Unit,
    onSelect: (ClosedResolution) -> Unit,
    onApply: () -> Unit,
    onOpenPaspor: () -> Unit,
) {
    val colors = KelanaTheme.colors
    if (state.isOffline) {
        // Re-planning needs the server: show the offline state (AGENTS.md).
        RencanaOfflineContent(hasPlan = true, onOpenPlan = onBack, onOpenPaspor = onOpenPaspor)
        return
    }
    val stop = state.stop
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 12.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(KelanaSizes.backButton)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.rencana_ic_back), contentDescription = stringResource(R.string.rencana_back), modifier = Modifier.size(24.dp))
                }
            }
            // PROVISIONAL art: the artifact draws a blob with three marks.
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.peach),
            ) {
                Image(painterResource(R.drawable.rencana_cat_keraton), contentDescription = null, modifier = Modifier.align(Alignment.Center).size(88.dp))
                Image(painterResource(R.drawable.rencana_info), contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 40.dp).size(56.dp).rotate(10f))
                Image(painterResource(R.drawable.rencana_veh_walk), contentDescription = null, modifier = Modifier.align(Alignment.BottomStart).padding(start = 50.dp, bottom = 30.dp).size(48.dp).rotate(-10f))
            }
            if (state.isLoading || stop == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                    Text(
                        stringResource(R.string.rencana_closed_kicker),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                        color = colors.stampBrown,
                    )
                    Text(
                        stringResource(R.string.rencana_closed_title, stop.placeName),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
                        color = colors.ink,
                    )
                    ClosedExplanation(stop, state.availability)
                }
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.rencana_closed_question),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                        color = colors.ink,
                    )
                    val replacement = state.replacement
                    ResolutionCard(
                        selected = state.selected == ClosedResolution.ReplaceWithOther,
                        enabled = replacement != null,
                        art = R.drawable.rencana_cat_keraton,
                        title = if (replacement != null) stringResource(R.string.rencana_closed_replace, replacement.placeName) else stringResource(R.string.rencana_closed_replace_none),
                        hint = replacement?.let { hoursText(it) } ?: stringResource(R.string.rencana_closed_replace_none_hint),
                        onClick = { onSelect(ClosedResolution.ReplaceWithOther) },
                    )
                    ResolutionCard(
                        selected = state.selected == ClosedResolution.MoveToOtherDay,
                        enabled = true,
                        art = R.drawable.rencana_calendar,
                        title = stringResource(R.string.rencana_closed_move),
                        hint = stringResource(R.string.rencana_closed_move_hint),
                        onClick = { onSelect(ClosedResolution.MoveToOtherDay) },
                    )
                    ResolutionCard(
                        selected = state.selected == ClosedResolution.Remove,
                        enabled = true,
                        art = R.drawable.rencana_need_stairs,
                        title = stringResource(R.string.rencana_closed_remove),
                        hint = stringResource(R.string.rencana_closed_remove_hint),
                        onClick = { onSelect(ClosedResolution.Remove) },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 26.dp)
                .navigationBarsPadding(),
        ) {
            KelanaButton(
                text = stringResource(R.string.rencana_closed_apply),
                onClick = onApply,
                enabled = stop != null && !state.isApplying,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Why the place is closed, with the check date of the fact it comes from. */
@Composable
private fun ClosedExplanation(stop: PlanStop, availability: Availability?) {
    val colors = KelanaTheme.colors
    val date: LocalDate?
    val text: String = when (availability) {
        is Availability.TemporarilyClosed -> {
            val closure = availability.closure
            date = closure.checkedAt
            val reason = closure.value.reason ?: stringResource(R.string.rencana_closed_reason_default)
            stringResource(
                R.string.rencana_closed_reason_period,
                reason,
                if (closure.value.startsOn == closure.value.endsOn) formatDayLong(closure.value.startsOn)
                else "${formatDayLong(closure.value.startsOn)} – ${formatDayLong(closure.value.endsOn)}",
            )
        }
        Availability.ClosedThatDay -> {
            date = null
            stringResource(R.string.rencana_closed_reason_weekday, stop.placeName)
        }
        is Availability.Open -> {
            date = availability.hours.checkedAt
            stringResource(R.string.rencana_closed_reason_hours_moved, formatClock(availability.opens), formatClock(availability.closes))
        }
        Availability.Unknown, null -> {
            date = null
            stringResource(R.string.rencana_closed_reason_unknown)
        }
    }
    Text(
        text,
        modifier = Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
        color = colors.muted,
    )
    if (date != null) VerifiedBadge(date, Modifier.padding(top = 6.dp))
}

@Composable
private fun ResolutionCard(
    selected: Boolean,
    enabled: Boolean,
    art: Int,
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .susunCard(selected, shape)
            .singleChoice(selected) { if (enabled) onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(art), contentDescription = null, modifier = Modifier.size(44.dp), alpha = if (enabled) 1f else 0.4f)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) colors.ink else colors.muted)
            Text(hint, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        if (selected) Image(painterResource(R.drawable.rencana_check), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

private fun previewClosed(): TempatTutupUiState {
    val stops = PreviewRencana.stops()
    val closure = OfficialFact(
        Closure(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 21), "Acara keraton"),
        "https://example.org/preview",
        LocalDate.of(2026, 11, 20),
    )
    return TempatTutupUiState(
        isLoading = false,
        stop = stops[0],
        availability = Availability.TemporarilyClosed(closure),
        replacement = stops[1],
        selected = ClosedResolution.ReplaceWithOther,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ClosedPreview() = KelanaTheme { TempatTutupContent(previewClosed(), {}, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ClosedNoReplacementPreview() = KelanaTheme {
    TempatTutupContent(previewClosed().copy(replacement = null, selected = ClosedResolution.Remove), {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ClosedOfflinePreview() = KelanaTheme { TempatTutupContent(previewClosed().copy(isOffline = true), {}, {}, {}, {}) }

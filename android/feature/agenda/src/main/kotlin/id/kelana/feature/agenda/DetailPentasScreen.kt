package id.kelana.feature.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.VerifiedBadge
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.Recurrence
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.ui.OfficialFactRow
import id.kelana.core.ui.formatClock
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.label
import java.time.Duration

@Composable
internal fun DetailPentasScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailPentasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    DetailPentasContent(
        state = state,
        onBack = onBack,
        // The sheet "Ke situs tiket resmi" is the OfficialSiteRoute dialog owned by :app; it opens the Custom Tab.
        onBuyTicket = { url, label -> navigate(OfficialSiteRoute(url = url, label = label)) },
    )
}

// Layout from overview slide conv 4 (middle phone), not the Figma frame 03.02 (not exported): PROVISIONAL.
@Composable
internal fun DetailPentasContent(
    state: DetailPentasUiState,
    onBack: () -> Unit,
    onBuyTicket: (url: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        KelanaBackTopBar(title = stringResource(R.string.detail_title), onBack = onBack)
        when (state) {
            DetailPentasUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            DetailPentasUiState.NotFound -> EmptyState(
                title = stringResource(R.string.detail_not_found_title),
                body = stringResource(R.string.detail_not_found_body),
                modifier = Modifier.padding(top = KelanaSpacing.lg),
            )
            is DetailPentasUiState.Found -> FoundContent(state, onBuyTicket)
        }
    }
}

@Composable
private fun FoundContent(state: DetailPentasUiState.Found, onBuyTicket: (String, String) -> Unit) {
    val colors = KelanaTheme.colors
    val event = state.event
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = KelanaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        // PROVISIONAL: header height 180 dp and the placeholder tile stand in for the illustrated cover.
        CoverPlaceholder(event.category, Modifier.fillMaxWidth().height(180.dp))
        Column(
            Modifier.padding(horizontal = KelanaSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
            ) {
                Surface(shape = KelanaShapes.chip, color = colors.skyText) {
                    Text(
                        text = event.category.label(),
                        modifier = Modifier.padding(horizontal = KelanaSpacing.md, vertical = KelanaSpacing.xs),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.navy,
                    )
                }
                event.schedule.checkedAt?.let { VerifiedBadge(checkedAt = it) }
            }
            Text(event.title, style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            PlaceAndDuration(event)

            Surface(
                shape = KelanaShapes.card,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Column(
                    Modifier.padding(KelanaSpacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
                ) {
                    OfficialFactRow(stringResource(R.string.detail_schedule_label), event.schedule) { scheduleText(it) }
                    OfficialFactRow(stringResource(R.string.detail_price_label), event.ticket) { priceText(it) }
                    // PROVISIONAL: plain label/value row, no Figma frame for Penampil yet.
                    if (event.performers.isNotEmpty()) {
                        PerformersRow(event.performers)
                    }
                }
            }
            arrivalAdviceText(event.arrivalAdviceMin, event.arrivalAdviceMax)?.let {
                // PROVISIONAL: note style (muted bodySmall) is an estimate.
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            state.sourceNote?.let {
                Text(
                    text = stringResource(R.string.detail_source_note, it.host, formatCheckedDate(it.openedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            if (event.description.isNotBlank()) {
                Text(
                    stringResource(R.string.detail_story_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                Text(event.description, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            }
            state.ticketUrl?.let { url ->
                Text(
                    stringResource(R.string.detail_no_payment),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                KelanaButton(
                    text = stringResource(R.string.detail_buy_ticket),
                    onClick = { onBuyTicket(url, event.placeName) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PerformersRow(performers: List<String>) {
    val colors = KelanaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs)) {
        Text(
            stringResource(R.string.detail_performers_label),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        Text(performers.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

/**
 * "Disarankan tiba 30 sampai 45 menit sebelum pentas". Equal bounds read "sekitar 30 menit"; a single bound is used
 * as it is; null when no advice is known.
 */
@Composable
private fun arrivalAdviceText(min: Int?, max: Int?): String? = when (val advice = arrivalAdvice(min, max)) {
    is ArrivalAdvice.Range -> stringResource(R.string.detail_arrival_range, advice.min, advice.max)
    is ArrivalAdvice.About -> stringResource(R.string.detail_arrival_single, advice.minutes)
    null -> null
}

/** "Candi Prambanan · 1 jam"; the duration is part of the schedule fact, so it is hidden when that is unchecked. */
@Composable
private fun PlaceAndDuration(event: Event) {
    val schedule = event.schedule.takeIf { it.checkedAt != null }?.value
    val minutes = schedule?.end?.let { Duration.between(schedule.start, it).toMinutes().toInt() }?.takeIf { it > 0 }
    val text = if (minutes == null) {
        event.placeName
    } else {
        stringResource(R.string.detail_place_and_duration, event.placeName, durationText(minutes))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = KelanaTheme.colors.muted)
}

@Composable
private fun durationText(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.detail_duration_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.detail_duration_hours, hours)
        else -> stringResource(R.string.detail_duration_minutes, minutes)
    }
}

/** "Setiap Jumat, 19.30 sampai 20.30". */
@Composable
private fun scheduleText(schedule: EventSchedule): String {
    val start = formatClock(schedule.start)
    val end = schedule.end?.let(::formatClock)
    return when (val recurrence = schedule.recurrence) {
        is Recurrence.Weekly -> {
            val day = weekdayName(recurrence.weekday)
            if (end == null) {
                stringResource(R.string.detail_schedule_weekly_no_end, day, start)
            } else {
                stringResource(R.string.detail_schedule_weekly, day, start, end)
            }
        }
        Recurrence.Daily ->
            if (end == null) {
                stringResource(R.string.detail_schedule_daily_no_end, start)
            } else {
                stringResource(R.string.detail_schedule_daily, start, end)
            }
        is Recurrence.Once -> {
            val day = formatDayLong(recurrence.date)
            if (end == null) {
                stringResource(R.string.detail_schedule_once_no_end, day, start)
            } else {
                stringResource(R.string.detail_schedule_once, day, start, end)
            }
        }
    }
}

@Composable
private fun priceText(ticket: EventTicket): String =
    ticket.priceFromIdr?.let { stringResource(R.string.detail_price_from, formatRupiah(it)) }
        ?: stringResource(R.string.detail_price_free)

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun DetailPentasPreview() {
    KelanaTheme {
        DetailPentasContent(
            DetailPentasUiState.Found(
                PreviewData.roro,
                SourceNote("example.org", PreviewData.today),
                "https://example.org/tiket",
            ),
            onBack = {},
            onBuyTicket = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun DetailPentasUncheckedPreview() {
    val unchecked = PreviewData.roro.copy(
        schedule = PreviewData.roro.schedule.copy(checkedAt = null),
        ticket = PreviewData.roro.ticket.copy(checkedAt = null),
    )
    KelanaTheme {
        DetailPentasContent(toFound(unchecked), onBack = {}, onBuyTicket = { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun DetailPentasNotFoundPreview() {
    KelanaTheme { DetailPentasContent(DetailPentasUiState.NotFound, onBack = {}, onBuyTicket = { _, _ -> }) }
}

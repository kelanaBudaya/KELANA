package id.kelana.feature.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.EventRow
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.component.VerifiedBadge
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.ui.formatTimeRange
import id.kelana.core.ui.label
import java.time.LocalDate

/**
 * Registers this feature's destinations: Agenda (tab home) and Detail pentas. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation (for example
 * `OfficialSiteRoute` for the ticket site); [onBack] pops the back stack.
 */
fun NavGraphBuilder.agendaScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<AgendaRoute> { AgendaScreen(navigate = navigate) }
    composable<DetailPentasRoute> { DetailPentasScreen(navigate = navigate, onBack = onBack) }
}

@Composable
internal fun AgendaScreen(navigate: (Any) -> Unit, viewModel: AgendaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    AgendaContent(
        state = state,
        onSelectDate = viewModel::selectDate,
        onSelectFilter = viewModel::selectFilter,
        onOpenEvent = { navigate(DetailPentasRoute(it)) },
    )
}

// Layout from overview slide conv 4 (left phone), not the Figma frame 03.01 (not exported): PROVISIONAL.
@Composable
internal fun AgendaContent(
    state: AgendaUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectFilter: (PlaceCategory?) -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = KelanaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        item {
            Column(Modifier.padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md)) {
                Text(
                    text = stringResource(R.string.agenda_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink,
                )
                val month = formatMonthYear(state.selectedDate)
                Text(
                    text = state.regionName?.let { stringResource(R.string.agenda_subtitle, it, month) } ?: month,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
        item { DateStrip(state.days, state.selectedDate, onSelectDate) }
        if (state.filters.isNotEmpty()) {
            item { FilterRow(state.filters, state.selectedFilter, onSelectFilter) }
        }
        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(KelanaSpacing.lg), contentAlignment = Alignment.Center) {
                    val loading = stringResource(R.string.agenda_loading)
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
                }
            }
        } else {
            val isToday = state.selectedDate == state.today
            state.tonight?.let { tonight ->
                item {
                    TonightCard(
                        occurrence = tonight,
                        isToday = isToday,
                        onOpen = { onOpenEvent(tonight.event.id) },
                        modifier = Modifier.padding(horizontal = KelanaSpacing.gutter),
                    )
                }
            }
            if (state.alsoToday.isNotEmpty()) {
                item {
                    val heading = when {
                        state.tonight != null && isToday -> R.string.agenda_also_today
                        state.tonight != null -> R.string.agenda_also_other_day
                        isToday -> R.string.agenda_today
                        else -> R.string.agenda_other_day
                    }
                    SectionHeading(stringResource(heading))
                }
                items(state.alsoToday, key = { "${it.event.id}-${it.date}" }) { occurrence ->
                    OccurrenceRow(occurrence, onOpenEvent, Modifier.padding(horizontal = KelanaSpacing.gutter))
                }
            }
            if (state.isSelectedDayEmpty) {
                item {
                    EmptyState(
                        title = stringResource(R.string.agenda_empty_title),
                        body = stringResource(R.string.agenda_empty_body),
                    )
                }
            }
            state.upcoming.forEach { section ->
                item(key = "heading-${section.date}") { SectionHeading(formatDayLong(section.date)) }
                items(section.items, key = { "${it.event.id}-${it.date}" }) { occurrence ->
                    OccurrenceRow(occurrence, onOpenEvent, Modifier.padding(horizontal = KelanaSpacing.gutter))
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = KelanaSpacing.gutter).padding(top = KelanaSpacing.sm),
        style = MaterialTheme.typography.titleMedium,
        color = KelanaTheme.colors.ink,
    )
}

@Composable
private fun DateStrip(days: List<AgendaDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val colors = KelanaTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = KelanaSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        items(days, key = { it.date.toString() }) { day ->
            val isSelected = day.date == selected
            val description = if (day.hasEvents) {
                stringResource(R.string.agenda_day_has_events_cd, formatDayLong(day.date))
            } else {
                formatDayLong(day.date)
            }
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .heightIn(min = 72.dp)
                    .background(if (isSelected) colors.navy else colors.surface, KelanaShapes.button)
                    .clickable(role = Role.Tab) { onSelect(day.date) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = description
                    }
                    .padding(vertical = KelanaSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs, Alignment.CenterVertically),
            ) {
                val textColor = if (isSelected) colors.white else colors.ink
                Text(formatDayShort(day.date), style = MaterialTheme.typography.bodySmall, color = textColor)
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .background(if (day.hasEvents) colors.accentOrange else Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun FilterRow(filters: List<PlaceCategory>, selected: PlaceCategory?, onSelect: (PlaceCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = KelanaSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        item {
            KelanaChip(stringResource(R.string.agenda_filter_all), selected == null, onClick = { onSelect(null) })
        }
        items(filters, key = { it.key }) { category ->
            KelanaChip(category.label(), selected == category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun TonightCard(
    occurrence: EventOccurrence,
    isToday: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = KelanaShapes.card,
        color = colors.creamYellowAlt,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(KelanaSpacing.gutter), verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs)) {
            Text(
                text = stringResource(if (isToday) R.string.agenda_tonight else R.string.agenda_tonight_other_day),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.accentOrange,
            )
            Text(event.title, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(event.placeName, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Text(whenText(occurrence), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            event.schedule.checkedAt?.let {
                VerifiedBadge(checkedAt = it, modifier = Modifier.padding(top = KelanaSpacing.xs))
            }
            KelanaButton(
                text = stringResource(R.string.agenda_see_detail),
                onClick = onOpen,
                modifier = Modifier.padding(top = KelanaSpacing.sm),
                variant = KelanaButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun OccurrenceRow(occurrence: EventOccurrence, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val event = occurrence.event
    EventRow(
        title = event.title,
        place = event.placeName,
        whenText = whenText(occurrence),
        onClick = { onOpen(event.id) },
        modifier = modifier,
        badge = event.schedule.checkedAt?.let { checked -> { VerifiedBadge(checkedAt = checked) } },
        leading = { CoverPlaceholder(event.category, Modifier.fillMaxSize()) },
    )
}

@Composable
private fun whenText(occurrence: EventOccurrence): String {
    val schedule = occurrence.event.schedule.value
    return stringResource(
        R.string.agenda_event_when,
        weekdayName(occurrence.date.dayOfWeek),
        formatTimeRange(schedule.start, schedule.end),
    )
}

/**
 * PROVISIONAL placeholder for the illustrated event cover: the cover SVGs are not in :core:designsystem yet.
 * Shows the category initial on a warm tile; replace with the real cover drawable when it exists.
 */
@Composable
internal fun CoverPlaceholder(category: PlaceCategory, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.agenda_illustration_placeholder_cd)
    Box(
        modifier = modifier
            .background(colors.creamYellow)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = category.label().take(1),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.accentOrange,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun AgendaPreview() {
    KelanaTheme { AgendaContent(PreviewData.agendaState, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun AgendaEmptyPreview() {
    KelanaTheme {
        AgendaContent(
            PreviewData.agendaState.copy(tonight = null, alsoToday = emptyList(), upcoming = emptyList()),
            {},
            {},
            {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 300)
@Composable
private fun AgendaLoadingPreview() {
    KelanaTheme {
        AgendaContent(AgendaUiState(today = PreviewData.today, selectedDate = PreviewData.today), {}, {}, {})
    }
}

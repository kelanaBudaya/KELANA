package id.kelana.feature.agenda

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.theme.ChipLabel
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventTag
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.navigation.PetaRoute
import id.kelana.core.ui.formatClock
import id.kelana.core.ui.formatTimeRange
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
        onOpenMap = { navigate(PetaRoute) },
    )
}

// Layout from docs/design/artifact/AgendaIlustratif.dc.html (screen 3.1), compared as code, not as a render.
@Composable
internal fun AgendaContent(
    state: AgendaUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectFilter: (EventTag?) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = KelanaSpacing.lg),
    ) {
        item { AgendaHeader(state, onOpenMap) }
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
            val rowModifier = Modifier.padding(horizontal = KelanaSpacing.gutter).padding(top = 10.dp)
            state.tonight?.let { tonight ->
                item {
                    TonightCard(
                        occurrence = tonight,
                        isToday = isToday,
                        onOpen = { onOpenEvent(tonight.event.id) },
                        modifier = Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 16.dp),
                    )
                }
            }
            // The first heading of the list sits 20 dp below what is above it, every later item 10 dp.
            var firstHeading = true
            if (state.alsoToday.isNotEmpty()) {
                item {
                    val heading = when {
                        state.tonight != null && isToday -> R.string.agenda_also_today
                        state.tonight != null -> R.string.agenda_also_other_day
                        isToday -> R.string.agenda_today
                        else -> R.string.agenda_other_day
                    }
                    SectionHeading(stringResource(heading), first = true)
                }
                firstHeading = false
                items(state.alsoToday, key = { "${it.event.id}-${it.date}" }) { occurrence ->
                    OccurrenceRow(occurrence, onOpenEvent, rowModifier)
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
                val first = firstHeading
                firstHeading = false
                item(key = "heading-${section.date}") { SectionHeading(formatDayLong(section.date), first) }
                items(section.items, key = { "${it.event.id}-${it.date}" }) { occurrence ->
                    OccurrenceRow(occurrence, onOpenEvent, rowModifier)
                }
            }
        }
    }
}

@Composable
private fun AgendaHeader(state: AgendaUiState, onOpenMap: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.agenda_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            val month = formatMonthYear(state.selectedDate)
            Text(
                text = state.regionName?.let { stringResource(R.string.agenda_subtitle, it, month) } ?: month,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = colors.muted,
            )
        }
        val mapDescription = stringResource(R.string.agenda_map_cd)
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(KelanaSizes.inputHeight)
                .clip(CircleShape)
                .background(colors.backgroundWarm)
                .clickable(role = Role.Button, onClick = onOpenMap)
                .semantics { contentDescription = mapDescription },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.agenda_ic_map), contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SectionHeading(text: String, first: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = KelanaSpacing.gutter)
            .padding(top = if (first) 20.dp else 10.dp),
        style = MaterialTheme.typography.titleLarge,
        color = KelanaTheme.colors.ink,
    )
}

// PROVISIONAL: the 18 dp radius of the day tiles has no token (artifact: 54 x 74, radius 18).
private val DayTileShape = RoundedCornerShape(18.dp)

// PROVISIONAL: the 24 dp radius of the hero card has no token (artifact: border-radius 24).
private val HeroShape = RoundedCornerShape(24.dp)

/** Fraunces 22/26 w600 in the artifact; the bundled static Fraunces has Regular and Bold, Bold is the closest. */
private val HeroTitle = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
)

@Composable
private fun DateStrip(days: List<AgendaDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val colors = KelanaTheme.colors
    LazyRow(
        modifier = Modifier.padding(top = 14.dp),
        contentPadding = PaddingValues(horizontal = KelanaSpacing.gutterWide),
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
                    .size(width = 54.dp, height = 74.dp)
                    .clip(DayTileShape)
                    .background(if (isSelected) colors.primary else colors.surface)
                    .then(if (isSelected) Modifier else Modifier.border(1.dp, colors.outline, DayTileShape))
                    .clickable(role = Role.Tab) { onSelect(day.date) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = description
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            ) {
                val textColor = if (isSelected) colors.white else colors.ink
                Text(formatDayShort(day.date), style = MaterialTheme.typography.labelMedium, color = textColor)
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 24.sp),
                    color = textColor,
                )
                val dot = when {
                    !day.hasEvents -> Color.Transparent
                    isSelected -> colors.stampYellow
                    else -> colors.accentOrange
                }
                Box(Modifier.size(6.dp).background(dot, CircleShape))
            }
        }
    }
}

@Composable
private fun FilterRow(filters: List<EventTag>, selected: EventTag?, onSelect: (EventTag?) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(top = 14.dp),
        contentPadding = PaddingValues(horizontal = KelanaSpacing.gutterWide),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        item {
            AgendaChip(stringResource(R.string.agenda_filter_all), selected == null, null) { onSelect(null) }
        }
        items(filters, key = { it.key }) { tag ->
            val icon = when (tag) {
                EventTag.Tari -> R.drawable.agenda_art_tari
                EventTag.Wayang -> R.drawable.agenda_art_wayang
                EventTag.RamahAnak -> null
            }
            AgendaChip(tag.label(), selected == tag, icon) { onSelect(tag) }
        }
    }
}

/** Filter chip with an optional 16 dp mark before the label (the shared KelanaChip has no icon slot). */
@Composable
private fun AgendaChip(label: String, selected: Boolean, @DrawableRes icon: Int?, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .height(KelanaSizes.chipHeight)
            .clip(KelanaShapes.chip)
            .background(if (selected) colors.primary else colors.surface)
            .border(1.5.dp, if (selected) colors.primary else colors.outline, KelanaShapes.chip)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = ChipLabel, color = if (selected) colors.white else colors.ink)
    }
}

/** "Malam ini" hero card, 358 x 190 (AgendaIlustratif): primary panel, radius 24, illustration on the right. */
@Composable
private fun TonightCard(
    occurrence: EventOccurrence,
    isToday: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    val schedule = event.schedule.value
    val label = stringResource(
        R.string.agenda_tonight_label,
        stringResource(if (isToday) R.string.agenda_tonight else R.string.agenda_tonight_other_day),
        formatClock(schedule.start),
    ).uppercase(IdLocale)
    val placeLine = schedule.end?.let { stringResource(R.string.agenda_ends_at, event.placeName, formatClock(it)) }
        ?: event.placeName
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(HeroShape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Image(
            painter = painterResource(R.drawable.agenda_hero_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        // Offsets are measured from the right edge of the 358 dp card; the marks' drop shadows are skipped.
        Image(
            painter = painterResource(R.drawable.agenda_art_tari),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 40.dp).size(110.dp).rotate(-6f),
        )
        Image(
            painter = painterResource(R.drawable.agenda_art_stamp),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 128.dp, top = 120.dp).size(50.dp).rotate(6f),
        )
        Column(Modifier.padding(start = 18.dp, top = 18.dp, end = 160.dp, bottom = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                color = colors.stampYellow,
            )
            Text(
                text = event.title,
                modifier = Modifier.padding(top = 6.dp),
                style = HeroTitle,
                color = colors.white,
            )
            Text(
                text = placeLine,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.skyText,
            )
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(32.dp)
                    .background(colors.stampYellow, KelanaShapes.button)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.agenda_see_detail),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.navy,
                )
            }
        }
    }
}

/** Event row of AgendaIlustratif: radius 20, 12 dp padding, 60 dp tile with a 40 dp mark, badge and chevron. */
@Composable
private fun OccurrenceRow(occurrence: EventOccurrence, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    val art = eventArt(event)
    val schedule = event.schedule.value
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = KelanaShapes.listCard,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        onClick = { onOpen(event.id) },
    ) {
        Row(
            modifier = Modifier.padding(KelanaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(KelanaSizes.listThumb).background(art.tile, KelanaShapes.card),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(art.icon), contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.agenda_place_when,
                        event.placeName,
                        formatTimeRange(schedule.start, schedule.end),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                event.schedule.checkedAt?.let { CheckedBadge(it, Modifier.padding(top = 2.dp)) }
            }
            Image(painterResource(R.drawable.agenda_ic_chevron), contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun AgendaPreview() {
    KelanaTheme { AgendaContent(PreviewData.agendaState, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun AgendaSelectedFilterPreview() {
    KelanaTheme { AgendaContent(PreviewData.agendaState.copy(selectedFilter = EventTag.Tari), {}, {}, {}, {}) }
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
            {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 300)
@Composable
private fun AgendaLoadingPreview() {
    KelanaTheme {
        AgendaContent(AgendaUiState(today = PreviewData.today, selectedDate = PreviewData.today), {}, {}, {}, {})
    }
}

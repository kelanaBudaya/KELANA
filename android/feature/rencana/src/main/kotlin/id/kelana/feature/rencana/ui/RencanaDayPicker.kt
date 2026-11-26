package id.kelana.feature.rencana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.rencana.R
import java.time.LocalDate

/** Plan title: today's plan, "Rencana berikutnya" for the default upcoming day, "Rencana Hari N" for any other day. */
@Composable
internal fun planTitle(state: RencanaUiState): String {
    val number = state.dayNumber
    return when {
        state.isToday -> stringResource(R.string.rencana_title_today)
        number != null && state.showDayPicker && !(state.isDefaultDay && state.plan?.date?.isAfter(state.today) == true) ->
            stringResource(R.string.rencana_title_day, number)
        else -> stringResource(R.string.rencana_title_upcoming)
    }
}

/** Horizontally scrollable "Hari 1 / Jum 27" chips, one per day of the trip. Shown only for trips of 2+ days. */
@Composable
internal fun DayPicker(state: RencanaUiState, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val selectedIndex = state.days.indexOfFirst { it.date == state.plan?.date }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(top = 14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.days, key = { _, day -> day.date.toString() }) { index, day ->
            DayChip(number = index + 1, date = day.date, selected = index == selectedIndex, onClick = { onSelect(day.date) })
        }
    }
}

@Composable
private fun DayChip(number: Int, date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.rencana_day_chip_desc, number, formatDayNumberMonth(date))
    Column(
        modifier = Modifier
            .widthIn(min = 72.dp)
            .heightIn(min = 48.dp)
            .clip(KelanaShapes.chip)
            .background(if (selected) colors.primary else colors.white)
            .border(1.5.dp, if (selected) colors.primary else colors.outline, KelanaShapes.chip)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.rencana_day_chip, number),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (selected) colors.white else colors.ink,
        )
        Text(
            text = "${weekdayShort(date.dayOfWeek)} ${date.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.white else colors.muted,
        )
    }
}

/** A day without stops: later days of a trip can be thin because places are never repeated across days. */
@Composable
internal fun EmptyDayCard(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, KelanaShapes.card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.rencana_day_empty_title), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text(stringResource(R.string.rencana_day_empty_body), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

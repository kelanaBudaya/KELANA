package id.kelana.feature.jelajah

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.EventRow
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import id.kelana.core.ui.formatTimeRange
import java.time.format.TextStyle
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/**
 * Cari, compared with docs/design/artifact/Cari.dc.html (code against the HTML, no render seen). PROVISIONAL: the "Kelas" chip, the "Pencarian terakhirmu"
 * list and the "Usulkan ke tim KELANA" banner are not built (no data source or route yet).
 */
@Composable
internal fun CariContent(
    state: CariUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (CariFilter) -> Unit,
    onPlace: (Place) -> Unit,
    onEvent: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(KelanaSizes.backButton).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back_cd),
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(colors.ink),
                )
            }
            Surface(
                modifier = Modifier.weight(1f).height(46.dp),
                shape = CircleShape,
                color = colors.white,
                border = BorderStroke(2.dp, colors.primary),
            ) {
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(colors.primary),
                    )
                    Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) {
                            Text(
                                stringResource(R.string.cari_hint),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.muted,
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                    if (state.query.isNotEmpty()) {
                        Box(
                            Modifier.size(KelanaSizes.chipHeight).clickable(role = Role.Button) { onQueryChange("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.cari_clear_cd),
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(colors.muted),
                            )
                        }
                    }
                }
            }
        }
        LazyRow(
            modifier = Modifier.padding(top = 14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CariFilter.entries) { filter ->
                KelanaChip(
                    label = stringResource(filter.label()),
                    selected = state.filter == filter,
                    onClick = { onFilterChange(filter) },
                )
            }
        }
        when {
            !state.hasQuery -> Text(
                stringResource(R.string.cari_prompt),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            state.isEmptyResult -> EmptyState(
                title = stringResource(R.string.cari_empty_title),
                body = stringResource(R.string.cari_empty_body),
                modifier = Modifier.padding(top = 24.dp),
            )
            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.events.isNotEmpty()) {
                    item { SectionLabel(R.string.cari_section_events) }
                    items(state.events, key = { "e-" + it.event.id }) { occurrence ->
                        EventResult(occurrence, onClick = { onEvent(occurrence) })
                    }
                }
                if (state.places.isNotEmpty()) {
                    item { SectionLabel(R.string.cari_section_places) }
                    items(state.places, key = { "p-" + it.id }) { place ->
                        PlaceItem(place = place, onClick = { onPlace(place) })
                    }
                }
            }
        }
    }
}

private fun CariFilter.label(): Int = when (this) {
    CariFilter.Semua -> R.string.cari_filter_all
    CariFilter.Tempat -> R.string.cari_filter_places
    CariFilter.Pentas -> R.string.cari_filter_events
}

@Composable
private fun SectionLabel(res: Int) {
    Text(
        stringResource(res),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
        color = KelanaTheme.colors.muted,
    )
}

/** Event row. The show time is an official fact: without a check date it shows "Jadwal sedang dicek", no time. */
@Composable
private fun EventResult(occurrence: EventOccurrence, onClick: () -> Unit) {
    val event = occurrence.event
    val shown = event.schedule.display()
    val whenText = when (shown) {
        is FactDisplay.Checked -> stringResource(
            R.string.event_when,
            occurrence.date.dayOfWeek.getDisplayName(TextStyle.FULL, IdLocale),
            formatTimeRange(shown.value.start, shown.value.end),
        )
        FactDisplay.Checking -> stringResource(R.string.event_checking)
    }
    EventRow(
        title = event.title,
        place = event.placeName,
        whenText = whenText,
        onClick = onClick,
        badge = (shown as? FactDisplay.Checked)?.let { checked -> { CheckedPill(checkedAt = checked.checkedAt) } },
        leading = { PlaceCover(event.category) },
    )
}

@Preview(showBackground = true)
@Composable
private fun CariResultsPreview() {
    KelanaTheme {
        CariContent(
            state = CariUiState(
                query = "wayang",
                places = PreviewData.places.take(1),
                events = listOf(EventOccurrence(PreviewData.event, java.time.LocalDate.of(2026, 11, 20))),
            ),
            onBack = {}, onQueryChange = {}, onFilterChange = {}, onPlace = {}, onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CariEmptyPreview() {
    KelanaTheme {
        CariContent(
            state = CariUiState(query = "zzz"),
            onBack = {}, onQueryChange = {}, onFilterChange = {}, onPlace = {}, onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CariIdlePreview() {
    KelanaTheme {
        CariContent(
            state = CariUiState(),
            onBack = {}, onQueryChange = {}, onFilterChange = {}, onPlace = {}, onEvent = {},
        )
    }
}

package id.kelana.feature.tempat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.ui.formatClock
import id.kelana.core.ui.formatRupiah
import id.kelana.feature.tempat.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/**
 * "Pertunjukan di sini": events hosted at this place. Renders nothing for an empty list (also when loading or when
 * the events failed to load). Source and check date are not shown here by product decision; the caller passes only
 * events whose schedule is checked. Tap opens the event detail.
 */
@Composable
internal fun PlaceEventsSection(
    events: List<Event>,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) return
    val colors = KelanaTheme.colors
    Column(modifier.padding(top = 26.dp)) {
        Text(
            text = stringResource(R.string.tempat_events_title),
            modifier = Modifier.padding(horizontal = KelanaSpacing.gutter),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Column(
            Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            groupWeeklyEvents(events).forEach { group ->
                EventItem(group = group, onClick = { onOpenEvent(group.events.first().id) })
            }
        }
    }
}

@Composable
private fun EventItem(group: EventGroup, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val event = group.events.first()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.backgroundWarm)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(KelanaSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = event.title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Text(
            text = scheduleLine(group),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        // The price is an official fact too: shown only when it has a check date.
        val price = event.ticket.takeIf { it.checkedAt != null }?.value?.priceFromIdr
        if (price != null) {
            Text(
                text = stringResource(R.string.tempat_events_price_from, formatRupiah(price)),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.primary,
            )
        }
    }
}

/**
 * Events of one place that differ only by weekday (the table holds one weekday per row) are shown as one row, e.g.
 * "Setiap Selasa–Sabtu · 20.00".
 */
internal data class EventGroup(val events: List<Event>)

internal fun groupWeeklyEvents(events: List<Event>): List<EventGroup> {
    val weekly = events.filter { it.schedule.value.recurrence is Recurrence.Weekly }
    val grouped = weekly.groupBy { Triple(it.title, it.schedule.value.start, it.ticket.value.priceFromIdr) }
    val seen = mutableSetOf<Triple<String, LocalTime, Long?>>()
    return events.mapNotNull { event ->
        if (event.schedule.value.recurrence !is Recurrence.Weekly) return@mapNotNull EventGroup(listOf(event))
        val key = Triple(event.title, event.schedule.value.start, event.ticket.value.priceFromIdr)
        if (!seen.add(key)) return@mapNotNull null
        EventGroup(grouped.getValue(key).sortedBy { (it.schedule.value.recurrence as Recurrence.Weekly).weekday })
    }
}

/** "Setiap hari · 19.30", "Setiap Selasa–Sabtu · 20.00" or "Jumat, 20 November · 19.00". */
@Composable
private fun scheduleLine(group: EventGroup): String {
    val schedule = group.events.first().schedule.value
    val time = formatClock(schedule.start)
    return when (val r = schedule.recurrence) {
        Recurrence.Daily -> stringResource(R.string.tempat_events_daily, time)
        is Recurrence.Weekly -> {
            val days = group.events.map { (it.schedule.value.recurrence as Recurrence.Weekly).weekday }
            stringResource(R.string.tempat_events_weekly, weekdaysLabel(days), time)
        }
        is Recurrence.Once -> stringResource(
            R.string.tempat_events_once,
            DateTimeFormatter.ofPattern("EEEE, d MMMM", IdLocale).format(r.date),
            time,
        )
    }
}

/** Consecutive days of 3 or more become a range ("Selasa–Sabtu"); otherwise a comma list. */
internal fun weekdaysLabel(days: List<DayOfWeek>): String {
    val sorted = days.distinct().sorted()
    fun name(d: DayOfWeek) = d.getDisplayName(TextStyle.FULL, IdLocale)
    val consecutive = sorted.zipWithNext().all { (a, b) -> b.value == a.value + 1 }
    return if (sorted.size >= 3 && consecutive) "${name(sorted.first())}–${name(sorted.last())}"
    else sorted.joinToString(", ") { name(it) }
}

internal fun sampleHostedEvent(id: String, title: String, recurrence: Recurrence, price: Long?, checked: Boolean = true): Event {
    val checkedAt = if (checked) LocalDate.of(2026, 11, 20) else null
    return Event(
        id = id, placeId = "museum-sonobudoyo", placeName = "Museum Sonobudoyo", title = title, description = "",
        category = PlaceCategory.Tari,
        schedule = OfficialFact(EventSchedule(recurrence, LocalTime.of(20, 0), null), null, checkedAt),
        ticket = OfficialFact(EventTicket(price, null), null, checkedAt),
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PlaceEventsSectionPreview() {
    KelanaTheme {
        PlaceEventsSection(
            events = listOf(
                sampleHostedEvent("e1", "Wayang kulit semalam", Recurrence.Weekly(DayOfWeek.SATURDAY), 60_000),
                sampleHostedEvent("e2", "Pentas tari", Recurrence.Daily, null),
                sampleHostedEvent("e3", "Festival dalang", Recurrence.Once(LocalDate.of(2026, 11, 27)), 25_000),
            ),
            onOpenEvent = {},
        )
    }
}

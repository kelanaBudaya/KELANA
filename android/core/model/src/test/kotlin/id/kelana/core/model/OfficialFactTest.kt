package id.kelana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class OfficialFactTest {
    private val checked = LocalDate.of(2026, 11, 20)

    @Test
    fun factWithCheckDate_displaysValueAndDate() {
        val fact = OfficialFact(value = "08.00-21.00", sourceUrl = "https://example.org", checkedAt = checked)
        assertEquals(FactDisplay.Checked("08.00-21.00", checked, "https://example.org"), fact.display())
    }

    @Test
    fun factWithoutCheckDate_displaysCheckingAndNeverTheValue() {
        val fact = OfficialFact(value = TicketPrice("Dewasa", 10_000, null), sourceUrl = null, checkedAt = null)
        assertEquals(FactDisplay.Checking, fact.display())
    }

    @Test
    fun checkedOnly_dropsFactsWithoutDate() {
        val facts = listOf(OfficialFact("a", null, checked), OfficialFact("b", null, null))
        assertEquals(listOf("a"), facts.checkedOnly().map { it.value })
    }

    @Test
    fun closure_containsBothEndDates() {
        val closure = Closure(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), null)
        assertTrue(LocalDate.of(2026, 12, 1) in closure)
        assertTrue(LocalDate.of(2026, 12, 3) in closure)
        assertTrue(LocalDate.of(2026, 12, 4) !in closure)
    }

    @Test
    fun weeklyEvent_occursOnItsWeekdayOnly() {
        val event = Event(
            id = "e", placeId = "p", placeName = "P", title = "T", description = "",
            category = PlaceCategory.Tari,
            schedule = OfficialFact(
                EventSchedule(Recurrence.Weekly(DayOfWeek.FRIDAY), LocalTime.of(19, 30), null),
                null, checked,
            ),
            ticket = OfficialFact(EventTicket(null, null), null, checked),
        )
        // 2026-11-20 is a Friday.
        assertEquals(
            listOf(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 27)),
            event.occurrencesBetween(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 28)),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 27), LocalDate.of(2026, 12, 4)),
            event.nextOccurrences(LocalDate.of(2026, 11, 20), 3),
        )
    }

    @Test
    fun categoryAndReasonKeys_roundTrip() {
        PlaceCategory.entries.forEach { assertEquals(it, PlaceCategory.fromKey(it.key)) }
        ReportReason.entries.forEach { assertEquals(it, ReportReason.fromKey(it.key)) }
    }
}

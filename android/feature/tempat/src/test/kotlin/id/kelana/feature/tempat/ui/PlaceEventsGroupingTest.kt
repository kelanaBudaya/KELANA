package id.kelana.feature.tempat.ui

import id.kelana.core.model.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals

class PlaceEventsGroupingTest {
    private fun weekly(id: String, day: DayOfWeek, title: String = "Pagelaran Wayang") =
        sampleHostedEvent(id, title, Recurrence.Weekly(day), 20_000)

    @Test
    fun `weekly rows with the same title, time and price become one group sorted by weekday`() {
        val events = listOf(
            weekly("e4", DayOfWeek.THURSDAY),
            weekly("e2", DayOfWeek.TUESDAY),
            weekly("e3", DayOfWeek.WEDNESDAY),
            sampleHostedEvent("d", "Angklung", Recurrence.Daily, null),
        )
        val groups = groupWeeklyEvents(events)
        assertEquals(2, groups.size)
        assertEquals(listOf("e2", "e3", "e4"), groups[0].events.map { it.id })
        assertEquals(listOf("d"), groups[1].events.map { it.id })
    }

    @Test
    fun `different titles and one-time events stay separate`() {
        val once = sampleHostedEvent("o", "Festival", Recurrence.Once(LocalDate.of(2026, 12, 1)), null)
        val groups = groupWeeklyEvents(listOf(weekly("a", DayOfWeek.MONDAY), weekly("b", DayOfWeek.MONDAY, "Lain"), once))
        assertEquals(3, groups.size)
    }

    @Test
    fun `three or more consecutive days read as a range, otherwise a list`() {
        assertEquals("Selasa–Sabtu", weekdaysLabel(DayOfWeek.values().filter { it.value in 2..6 }))
        assertEquals("Selasa, Kamis", weekdaysLabel(listOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)))
    }

    private val tueToSat = listOf(
        weekly("tue", DayOfWeek.TUESDAY), weekly("wed", DayOfWeek.WEDNESDAY), weekly("thu", DayOfWeek.THURSDAY),
        weekly("fri", DayOfWeek.FRIDAY), weekly("sat", DayOfWeek.SATURDAY),
    )

    @Test
    fun `group opens today's member when today is one of the days`() {
        // 2026-11-26 is a Thursday.
        assertEquals("thu", groupTarget(EventGroup(tueToSat), LocalDate.of(2026, 11, 26)).id)
    }

    @Test
    fun `group opens the first day of next week when today is after the last day`() {
        // 2026-11-29 is a Sunday: next occurrence is Tuesday.
        assertEquals("tue", groupTarget(EventGroup(tueToSat), LocalDate.of(2026, 11, 29)).id)
    }

    @Test
    fun `single event is its own target`() {
        val once = sampleHostedEvent("o", "Festival", Recurrence.Once(LocalDate.of(2026, 12, 1)), null)
        assertEquals("o", groupTarget(EventGroup(listOf(once)), LocalDate.of(2026, 11, 26)).id)
    }
}

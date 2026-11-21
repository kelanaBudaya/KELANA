package id.kelana.feature.tempat.domain

import id.kelana.core.model.FactDisplay
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.TicketPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PlaceFactsTest {
    private val d1 = LocalDate.of(2026, 11, 20)
    private val d0 = LocalDate.of(2026, 11, 1)

    private fun hours(day: DayOfWeek, closed: Boolean, checked: LocalDate?) = OfficialFact(
        OpeningHours(day, closed, if (closed) null else LocalTime.of(9, 0), if (closed) null else LocalTime.of(15, 0)),
        "https://www.example.org/x",
        checked,
    )

    private fun place(hours: List<OfficialFact<OpeningHours>>, tickets: List<OfficialFact<TicketPrice>> = emptyList()) =
        Place("p", "p", "P", PlaceCategory.Museum, "YK-01", 0.0, 0.0, "a", "d", "p", hours, tickets)

    @Test
    fun todayHours_picks_the_weekday() {
        val list = listOf(hours(DayOfWeek.MONDAY, true, d1), hours(DayOfWeek.TUESDAY, false, d1))
        assertEquals(DayOfWeek.TUESDAY, todayHours(list, DayOfWeek.TUESDAY)?.value?.weekday)
        assertNull(todayHours(list, DayOfWeek.FRIDAY))
    }

    @Test
    fun closedDays_use_oldest_date_when_all_checked() {
        val fact = closedDaysFact(listOf(hours(DayOfWeek.MONDAY, true, d1), hours(DayOfWeek.TUESDAY, false, d0)))!!
        assertEquals(listOf(DayOfWeek.MONDAY), fact.value)
        assertEquals(d0, fact.checkedAt)
    }

    @Test
    fun closedDays_without_a_check_date_render_as_checking() {
        val fact = closedDaysFact(listOf(hours(DayOfWeek.MONDAY, true, d1), hours(DayOfWeek.TUESDAY, false, null)))!!
        assertNull(fact.checkedAt)
        assertEquals(FactDisplay.Checking, fact.display())
    }

    @Test
    fun closedDays_null_without_hours() {
        assertNull(closedDaysFact(emptyList()))
    }

    @Test
    fun sourceHost_only_from_checked_facts() {
        assertEquals("example.org", sourceHost(place(listOf(hours(DayOfWeek.MONDAY, true, d1)))))
        assertNull(sourceHost(place(listOf(hours(DayOfWeek.MONDAY, true, null)))))
    }

    @Test
    fun sellerUrl_only_from_checked_tickets() {
        val checked = OfficialFact(TicketPrice("Dewasa", 1, "https://tiket.example"), null, d1)
        val unchecked = OfficialFact(TicketPrice("Dewasa", 1, "https://tiket.example"), null, null)
        assertEquals("https://tiket.example", officialSellerUrl(place(emptyList(), listOf(checked))))
        assertNull(officialSellerUrl(place(emptyList(), listOf(unchecked))))
        assertTrue(OFFICIAL_PHOTO_COUNT > 0)
    }

    @Test
    fun hoursSummary_is_one_run_of_open_days() {
        val list = listOf(
            hours(DayOfWeek.MONDAY, true, d1),
            hours(DayOfWeek.TUESDAY, false, d1),
            hours(DayOfWeek.WEDNESDAY, false, d0),
            hours(DayOfWeek.THURSDAY, false, d1),
        )
        val summary = hoursSummary(list)!!
        assertEquals(DayOfWeek.TUESDAY, summary.value.firstDay)
        assertEquals(DayOfWeek.THURSDAY, summary.value.lastDay)
        assertEquals(d0, summary.checkedAt)
    }

    @Test
    fun hoursSummary_has_no_date_when_any_hours_is_unchecked() {
        val list = listOf(hours(DayOfWeek.TUESDAY, false, d1), hours(DayOfWeek.WEDNESDAY, false, null))
        assertNull(hoursSummary(list)!!.checkedAt)
    }

    @Test
    fun hoursSummary_is_null_when_open_days_have_a_gap_or_no_open_day() {
        val gap = listOf(hours(DayOfWeek.MONDAY, false, d1), hours(DayOfWeek.WEDNESDAY, false, d1))
        assertNull(hoursSummary(gap))
        assertNull(hoursSummary(listOf(hours(DayOfWeek.MONDAY, true, d1))))
        assertNull(hoursSummary(emptyList()))
    }

    @Test
    fun infoCheckedAt_is_oldest_checked_date_or_null() {
        val list = listOf(hours(DayOfWeek.MONDAY, true, d1), hours(DayOfWeek.TUESDAY, false, d0), hours(DayOfWeek.FRIDAY, false, null))
        assertEquals(d0, infoCheckedAt(place(list)))
        assertNull(infoCheckedAt(place(listOf(hours(DayOfWeek.MONDAY, true, null)))))
    }
}

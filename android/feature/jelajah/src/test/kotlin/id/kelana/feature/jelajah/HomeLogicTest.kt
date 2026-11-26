package id.kelana.feature.jelajah

import id.kelana.core.model.Closure
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.core.navigation.RencanaRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class HomeLogicTest {
    private val friday = LocalDate.of(2026, 11, 20)

    private fun signedIn(name: String?, email: String? = "a@b.id") =
        SessionState.SignedIn(SessionUser("1", email, name))

    // Greeting

    @Test fun greetingUsesFirstWordOfFullName() =
        assertEquals(GreetingName.Named("Budi"), greetingNameOf(signedIn("Budi Santoso")))

    @Test fun greetingSingleName() =
        assertEquals(GreetingName.Named("Sari"), greetingNameOf(signedIn("Sari")))

    @Test fun greetingIgnoresExtraSpaces() =
        assertEquals(GreetingName.Named("Dewi"), greetingNameOf(signedIn("   Dewi   Lestari  ")))

    @Test fun greetingBlankNameIsPlain() =
        assertEquals(GreetingName.Anonymous, greetingNameOf(signedIn("   ")))

    @Test fun greetingNullNameIsPlainNeverEmail() =
        assertEquals(GreetingName.Anonymous, greetingNameOf(signedIn(null, "budi@x.id")))

    @Test fun greetingGuestAndLoadingHaveNone() {
        assertEquals(GreetingName.None, greetingNameOf(SessionState.Guest))
        assertEquals(GreetingName.None, greetingNameOf(SessionState.Loading))
    }

    // Quick actions

    @Test fun quickActionRoutes() {
        assertEquals(AgendaRoute, quickActionRoute(QuickAction.Agenda))
        assertEquals(passportRoute, quickActionRoute(QuickAction.AmbilCap))
        assertEquals(RencanaRoute, quickActionRoute(QuickAction.Rencana))
        assertEquals(PostcardRoute(), quickActionRoute(QuickAction.Postcard))
    }

    @Test fun onlyStampAndPostcardNeedAnAccount() {
        assertEquals(setOf(QuickAction.AmbilCap, QuickAction.Postcard), QuickAction.entries.filter { it.needsAccount }.toSet())
    }

    // Kunjungi Hari Ini

    @Test fun visitTodayWhenCheckedHoursOpenToday() =
        assertTrue(isVisitToday(PreviewData.place("A", PlaceCategory.Museum), friday))

    @Test fun noVisitTodayWhenClosedDay() {
        val base = PreviewData.place("B", PlaceCategory.Museum)
        val closedFriday = base.copy(
            hours = base.hours.map {
                if (it.value.weekday == DayOfWeek.FRIDAY) {
                    OfficialFact(OpeningHours(DayOfWeek.FRIDAY, true, null, null), it.sourceUrl, it.checkedAt)
                } else it
            },
        )
        assertFalse(isVisitToday(closedFriday, friday))
    }

    @Test fun noVisitTodayWhenHoursUnchecked() =
        assertFalse(isVisitToday(PreviewData.place("C", PlaceCategory.Museum, withCheck = false), friday))

    @Test fun noVisitTodayDuringCheckedClosure() {
        val base = PreviewData.place("D", PlaceCategory.Museum)
        val closed = base.copy(
            closures = listOf(OfficialFact(Closure(friday, friday.plusDays(1), "Renovasi"), "https://example.org", friday)),
        )
        assertFalse(isVisitToday(closed, friday))
        assertTrue(isVisitToday(closed, friday.plusDays(2)))
    }

    @Test fun noVisitTodayWhenDateUnknown() =
        assertFalse(isVisitToday(PreviewData.place("E", PlaceCategory.Museum), null))

    @Test fun todayIsJakartaDateNotUtc() {
        // 2026-11-20 20:00 UTC is already 21 November 03:00 in Jakarta (UTC+7).
        val clock = Clock.fixed(Instant.parse("2026-11-20T20:00:00Z"), ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 11, 21), todayInJakarta(clock))
    }
}

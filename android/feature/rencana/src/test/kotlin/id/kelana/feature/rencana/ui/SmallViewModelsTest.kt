package id.kelana.feature.rencana.ui

import androidx.lifecycle.SavedStateHandle
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Recurrence
import id.kelana.core.navigation.IZIN_NOTIF_RESULT_KEY
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private suspend fun ReceiveTurbine<PetaRuteUiState>.settled(): PetaRuteUiState {
    var item = awaitItem()
    while (item.isLoading) item = awaitItem()
    return item
}

class PetaRuteViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val places = FakePlaces(
        listOf(
            place("a", lat = -7.800, hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))),
            place("b", PlaceCategory.Museum, lat = -7.801, hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))),
        ),
    )

    @Test
    fun noPlanMeansNoRoute() = runTest {
        PetaRuteViewModel(repo(places), NoRoutes, TestClock).uiState.test {
            val s = settled()
            assertTrue(s.stops.isEmpty())
            assertNull(s.next)
        }
    }

    @Test
    fun nextIsTheStopGoingOnNowOrTheFirstThatHasNotEnded() = runTest {
        // Window from 10:00 with a relaxed pace: a 10:00-11:30, b after the break. Now is 10:40, so "a" is next.
        val trip = PlanBuilder().buildTrip(choices(start = LocalTime.of(10, 0)), places.all, emptyList(), TestToday)
        val cache = FakeCache(trip)
        PetaRuteViewModel(repo(places, cache = cache), NoRoutes, TestClock).uiState.test {
            val s = settled()
            assertEquals(2, s.stops.size)
            assertEquals(s.stops.first(), s.next)
        }
    }

    @Test
    fun afterTheLastStopThereIsNothingNext() = runTest {
        val trip = PlanBuilder().buildTrip(
            choices(start = LocalTime.of(6, 0), end = LocalTime.of(10, 0)),
            places.all, emptyList(), TestToday,
        )
        PetaRuteViewModel(repo(places, cache = FakeCache(trip)), NoRoutes, TestClock).uiState.test {
            val s = settled()
            assertTrue(s.stops.isNotEmpty())
            assertNull(s.next)
        }
    }
}

class PengingatViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val cache = FakeCache()
    private val scheduler = FakeReminderScheduler()

    // The show in these tests: Friday 20 November 2026 at 19.30 (WIB), checked on 20 November. "Now" is 10.40 WIB.
    private val showDay = LocalDate.of(2026, 11, 20)
    private fun show(checked: LocalDate? = showDay, day: LocalDate = showDay) = Event(
        id = "roro",
        placeId = "prambanan",
        placeName = "Prambanan",
        title = "Sendratari Roro Jonggrang",
        description = "",
        category = PlaceCategory.Tari,
        schedule = OfficialFact(EventSchedule(Recurrence.Once(day), LocalTime.of(19, 30), LocalTime.of(21, 0)), "https://example.org", checked),
        ticket = OfficialFact(EventTicket(null, null), null, checked),
    )

    private fun viewModel(
        eventId: String = "roro",
        events: FakeEvents = FakeEvents(listOf(show())),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = PengingatViewModel(repo(FakePlaces(emptyList()), cache = cache), scheduler, events, TestClock, eventId, handle)

    @Test
    fun defaultsToWhenItIsTimeToLeaveWithoutCalendar() {
        val s = viewModel().uiState.value
        assertEquals(ReminderTiming.WhenToLeave, s.timing)
        assertFalse(s.addToCalendar)
        assertFalse(s.saved)
    }

    @Test
    fun saveStoresTheChoiceForTheShowAndSchedulesIt() = runTest {
        val vm = viewModel()
        vm.selectTiming(ReminderTiming.OneHourBefore)
        vm.setAddToCalendar(true)
        vm.save()
        assertTrue(vm.uiState.value.saved)
        val choice = ReminderChoice(ReminderTiming.OneHourBefore, true)
        assertEquals(choice, cache.reminderState.value["roro"])
        assertEquals(listOf("roro" to choice), scheduler.scheduleCalls)
    }

    @Test
    fun saveWithoutAnEventIdDoesNothing() = runTest {
        val vm = viewModel(eventId = "")
        vm.save()
        assertFalse(vm.uiState.value.saved)
        assertTrue(cache.reminderState.value.isEmpty())
        assertTrue(scheduler.scheduleCalls.isEmpty())
    }

    @Test
    fun savingTwiceSchedulesOnce() = runTest {
        val vm = viewModel()
        vm.save()
        vm.save()
        assertEquals(1, scheduler.scheduleCalls.size)
    }

    @Test
    fun routeArgumentIsReadFromTheSavedStateHandle() = runTest {
        val vm = PengingatViewModel(
            repo(FakePlaces(emptyList()), cache = cache),
            scheduler,
            FakeEvents(emptyList()),
            SavedStateHandle(mapOf("eventId" to "e9")),
        )
        vm.save()
        assertEquals(setOf("e9"), cache.reminderState.value.keys)
        assertEquals(listOf("e9"), scheduler.scheduleCalls.map { it.first })
    }

    @Test
    fun calendarOffMeansNoCalendarInsert() = runTest {
        val vm = viewModel()
        vm.save()
        assertNull(vm.uiState.value.calendarInsert)
    }

    @Test
    fun calendarOnGivesTheInsertForTheNextShowTime() = runTest {
        val vm = viewModel()
        vm.setAddToCalendar(true)
        vm.save()
        val insert = vm.uiState.value.calendarInsert!!
        assertEquals("Sendratari Roro Jonggrang", insert.title)
        assertEquals("Prambanan", insert.location)
        assertEquals(Instant.parse("2026-11-20T12:30:00Z").toEpochMilli(), insert.startMillis) // 19.30 WIB
        assertEquals(Instant.parse("2026-11-20T14:00:00Z").toEpochMilli(), insert.endMillis) // 21.00 WIB
    }

    @Test
    fun calendarInsertSkipsAShowThatHasAlreadyHappenedOrHasNoCheckedTime() = runTest {
        val past = viewModel(events = FakeEvents(listOf(show(day = showDay.minusDays(1)))))
        past.setAddToCalendar(true)
        past.save()
        assertTrue(past.uiState.value.saved)
        assertNull(past.uiState.value.calendarInsert)

        val unchecked = viewModel(events = FakeEvents(listOf(show(checked = null))))
        unchecked.setAddToCalendar(true)
        unchecked.save()
        assertNull(unchecked.uiState.value.calendarInsert)
    }

    @Test
    fun saveGoesOnWhenTheNotificationPermissionSheetHasClosed() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle = handle)
        assertFalse(vm.uiState.value.saved)
        handle[IZIN_NOTIF_RESULT_KEY] = true // what the Izin notifikasi sheet sets when it closes
        assertTrue(vm.uiState.value.saved)
        assertEquals(1, scheduler.scheduleCalls.size)
        assertFalse(handle.get<Boolean>(IZIN_NOTIF_RESULT_KEY)!!)
    }
}

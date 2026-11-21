package id.kelana.feature.rencana.ui

import androidx.lifecycle.SavedStateHandle
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
        PetaRuteViewModel(repo(places), TestClock).uiState.test {
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
        PetaRuteViewModel(repo(places, cache = cache), TestClock).uiState.test {
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
        PetaRuteViewModel(repo(places, cache = FakeCache(trip)), TestClock).uiState.test {
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
    private fun viewModel(eventId: String = "roro") = PengingatViewModel(repo(FakePlaces(emptyList()), cache = cache), eventId)

    @Test
    fun defaultsToWhenItIsTimeToLeaveWithoutCalendar() {
        val s = viewModel().uiState.value
        assertEquals(ReminderTiming.WhenToLeave, s.timing)
        assertFalse(s.addToCalendar)
        assertFalse(s.saved)
    }

    @Test
    fun saveStoresTheChoiceForTheShow() = runTest {
        val vm = viewModel()
        vm.selectTiming(ReminderTiming.OneHourBefore)
        vm.setAddToCalendar(true)
        vm.save()
        assertTrue(vm.uiState.value.saved)
        assertEquals(ReminderChoice(ReminderTiming.OneHourBefore, true), cache.reminderState.value["roro"])
    }

    @Test
    fun saveWithoutAnEventIdDoesNothing() = runTest {
        val vm = viewModel(eventId = "")
        vm.save()
        assertFalse(vm.uiState.value.saved)
        assertTrue(cache.reminderState.value.isEmpty())
    }

    @Test
    fun routeArgumentIsReadFromTheSavedStateHandle() = runTest {
        val vm = PengingatViewModel(repo(FakePlaces(emptyList()), cache = cache), SavedStateHandle(mapOf("eventId" to "e9")))
        vm.save()
        assertEquals(setOf("e9"), cache.reminderState.value.keys)
    }
}

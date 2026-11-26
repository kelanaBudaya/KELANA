package id.kelana.feature.rencana.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.model.Closure
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

private suspend fun ReceiveTurbine<TempatTutupUiState>.settled(): TempatTutupUiState {
    var item = awaitItem()
    while (item.isLoading) item = awaitItem()
    return item
}

class TempatTutupViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val allDay = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))

    private fun world(withSpare: Boolean): Triple<FakePlaces, FakeCache, TripPlan> {
        val base = listOf(
            place("keraton", PlaceCategory.Lainnya, lat = -7.8053, hours = allDay),
            place("tamansari", PlaceCategory.Lainnya, lat = -7.8100, hours = allDay),
        )
        val places = FakePlaces(base + if (withSpare) listOf(place("puro", PlaceCategory.Lainnya, lat = -7.8007, hours = allDay)) else emptyList())
        // The plan was made when only keraton and tamansari existed (two stops, relaxed pace allows three).
        val trip = PlanBuilder().buildTrip(choices(setOf(Interest.Keraton), pace = Pace.Moderate), base, emptyList(), TestToday)
        return Triple(places, FakeCache(trip), trip)
    }

    private fun closeKeraton(places: FakePlaces) {
        val closure = OfficialFact(Closure(TestToday, TestToday, "Acara keraton"), "https://example.org/c", TestToday)
        places.all = places.all.map { if (it.id == "keraton") it.copy(closures = listOf(closure)) else it }
    }

    private fun viewModel(places: FakePlaces, cache: FakeCache, placeId: String = "keraton") =
        TempatTutupViewModel(repo(places, cache = cache), placeId, TestToday)

    @Test
    fun closedPlaceLoadsWithItsReasonAndAReplacement() = runTest {
        val (places, cache) = world(withSpare = true).let { it.first to it.second }
        closeKeraton(places)
        viewModel(places, cache).uiState.test {
            val s = settled()
            assertEquals("keraton", s.stop?.placeId)
            assertTrue(s.availability is Availability.TemporarilyClosed)
            assertEquals("Acara keraton", (s.availability as Availability.TemporarilyClosed).closure.value.reason)
            assertEquals("puro", s.replacement?.placeId)
            assertEquals(ClosedResolution.ReplaceWithOther, s.selected)
        }
    }

    @Test
    fun withoutAReplacementRemoveIsPreselectedAndReplaceCannotBeChosen() = runTest {
        val (places, cache) = world(withSpare = false).let { it.first to it.second }
        closeKeraton(places)
        viewModel(places, cache).uiState.test {
            val vm = settled()
            assertNull(vm.replacement)
            assertEquals(ClosedResolution.Remove, vm.selected)
        }
        val model = viewModel(places, cache)
        model.uiState.test {
            assertEquals(ClosedResolution.Remove, settled().selected)
            model.select(ClosedResolution.ReplaceWithOther)
            expectNoEvents()
            model.select(ClosedResolution.MoveToOtherDay)
            assertEquals(ClosedResolution.MoveToOtherDay, awaitItem().selected)
        }
    }

    @Test
    fun applyingTheReplacementSwapsTheStopInTheCachedPlan() = runTest {
        val (places, cache, _) = world(withSpare = true)
        closeKeraton(places)
        val vm = viewModel(places, cache)
        vm.uiState.test {
            settled()
            vm.apply()
            val done = expectMostRecentItem()
            assertEquals(TempatTutupOutcome.Done, done.outcome)
        }
        val stops = cache.tripState.value!!.days.single().stops.map { it.placeId }
        assertFalse("keraton" in stops)
        assertTrue("puro" in stops)
        vm.consumeOutcome()
        assertNull(vm.uiState.value.outcome)
    }

    @Test
    fun removingTakesTheStopOutAndKeepsTheOthers() = runTest {
        val (places, cache, original) = world(withSpare = false)
        closeKeraton(places)
        val vm = viewModel(places, cache)
        vm.uiState.test {
            settled()
            vm.select(ClosedResolution.Remove)
            vm.apply()
            assertEquals(TempatTutupOutcome.Done, expectMostRecentItem().outcome)
        }
        val stops = cache.tripState.value!!.days.single().stops.map { it.placeId }
        assertEquals(original.days.single().stops.map { it.placeId } - "keraton", stops)
    }

    @Test
    fun movingToAnotherDayLeavesThePlanAloneAndOpensTheWizard() = runTest {
        val (places, cache, original) = world(withSpare = true)
        closeKeraton(places)
        val vm = viewModel(places, cache)
        vm.uiState.test {
            settled()
            vm.select(ClosedResolution.MoveToOtherDay)
            vm.apply()
            assertEquals(TempatTutupOutcome.GoToWizard, expectMostRecentItem().outcome)
        }
        assertEquals(original, cache.tripState.value)
    }

    @Test
    fun withoutSignalApplyStillWorks() = runTest {
        // The view model has no connectivity input any more: re-planning runs from cached data.
        val (places, cache, _) = world(withSpare = true)
        closeKeraton(places)
        val vm = viewModel(places, cache)
        vm.uiState.test {
            settled()
            vm.apply()
            assertEquals(TempatTutupOutcome.Done, expectMostRecentItem().outcome)
        }
        assertTrue("puro" in cache.tripState.value!!.days.single().stops.map { it.placeId })
    }

    @Test
    fun applyFailureSetsApplyError() = runTest {
        val (places, cache, original) = world(withSpare = true)
        closeKeraton(places)
        val vm = viewModel(places, cache)
        vm.uiState.test {
            settled()
            places.failing = true
            vm.apply()
            val s = expectMostRecentItem()
            assertTrue(s.applyError)
            assertFalse(s.isApplying)
            assertNull(s.outcome)
        }
        assertEquals(original, cache.tripState.value)
        // A second try after the cause is gone clears the error.
        places.failing = false
        vm.apply()
        assertFalse(vm.uiState.value.applyError)
        assertEquals(TempatTutupOutcome.Done, vm.uiState.value.outcome)
    }

    @Test
    fun loadFailureSetsLoadErrorAndRetryLoadRecovers() = runTest {
        val (places, cache, _) = world(withSpare = true)
        closeKeraton(places)
        places.failing = true
        val vm = viewModel(places, cache)
        vm.uiState.test {
            val s = settled()
            assertTrue(s.loadError)
            assertNull(s.stop)
            places.failing = false
            vm.retryLoad()
            var next = awaitItem()
            while (next.isLoading || next.loadError) next = awaitItem()
            assertEquals("keraton", next.stop?.placeId)
            assertFalse(next.loadError)
        }
    }

    @Test
    fun aPlaceThatIsNotClosedIsNotFound() = runTest {
        val (places, cache, _) = world(withSpare = true)
        viewModel(places, cache).uiState.test {
            val s = settled()
            assertTrue(s.notFound)
            assertNull(s.stop)
        }
    }

    @Test
    fun routeArgumentsAreReadFromTheSavedStateHandle() = runTest {
        val (places, cache, _) = world(withSpare = true)
        closeKeraton(places)
        val handle = SavedStateHandle(mapOf("placeId" to "keraton", "date" to "2026-11-20"))
        val vm = TempatTutupViewModel(repo(places, cache = cache), handle)
        vm.uiState.test {
            assertEquals("keraton", settled().stop?.placeId)
        }
        assertEquals(TestToday, parseIsoDate("2026-11-20"))
        assertNull(parseIsoDate("nope"))
        assertNotNull(parseIsoDate("2026-11-21"))
    }
}

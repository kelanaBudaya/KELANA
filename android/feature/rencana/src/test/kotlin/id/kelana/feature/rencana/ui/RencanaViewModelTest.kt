package id.kelana.feature.rencana.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.model.Closure
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanCache
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import id.kelana.feature.rencana.domain.show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

private suspend fun ReceiveTurbine<RencanaUiState>.settled(): RencanaUiState {
    var item = awaitItem()
    while (item.isLoading) item = awaitItem()
    return item
}

class RencanaViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val places = FakePlaces(
        listOf(
            place("m1", hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))),
            place("pram", PlaceCategory.Candi, lat = -7.752, lng = 110.491, hours = hoursFacts(LocalTime.of(6, 30), LocalTime.of(17, 30))),
        ),
    )
    private val events = FakeEvents(listOf(show("roro", "pram")))

    private fun trip(): TripPlan = PlanBuilder().buildTrip(
        choices(setOf(Interest.Museum, Interest.Pentas), companion = TravelGroup.Family, pace = Pace.Moderate),
        places.all, listOf(show("roro", "pram")), builtOn = TestToday,
    )

    private fun viewModel(
        cache: FakeCache,
        connectivity: FakeConnectivity = FakeConnectivity(),
        backpack: FakeBackpack = FakeBackpack(),
    ) = RencanaViewModel(repo(places, events, cache), events, connectivity, places, backpack, TestClock)

    @Test
    fun backpackListsAddedPlacesInOrderAndSkipsUnknownIds() = runTest {
        viewModel(FakeCache(), backpack = FakeBackpack(listOf("pram", "gone", "m1"))).uiState.test {
            val state = settled()
            assertEquals(listOf("pram", "m1"), state.backpack.map { it.id })
        }
    }

    @Test
    fun removeFromBackpackDropsThePlace() = runTest {
        val backpack = FakeBackpack(listOf("pram", "m1"))
        val model = viewModel(FakeCache(), backpack = backpack)
        model.uiState.test {
            settled()
            model.removeFromBackpack("pram")
            var state = awaitItem()
            while (state.backpack.size != 1) state = awaitItem()
            assertEquals(listOf("m1"), state.backpack.map { it.id })
            assertEquals(listOf("m1"), backpack.ids.value)
        }
    }

    @Test
    fun noPlanIsTheEmptyStateAndOffersTonightsShow() = runTest {
        viewModel(FakeCache()).uiState.test {
            val state = settled()
            assertNull(state.plan)
            assertEquals("roro", state.tonight?.event?.id)
            assertTrue(state.closedStops.isEmpty())
        }
    }

    @Test
    fun cachedPlanOfTodayIsShownWithItsCompanion() = runTest {
        viewModel(FakeCache(trip())).uiState.test {
            val state = settled()
            assertEquals(TestToday, state.plan?.date)
            assertTrue(state.isToday)
            assertEquals(TravelGroup.Family, state.companion)
            assertTrue(state.plan!!.stops.any { it.isEvent })
        }
    }

    @Test
    fun cachedPlanStaysReadableOffline() = runTest {
        val connectivity = FakeConnectivity(online = false)
        viewModel(FakeCache(trip()), connectivity).uiState.test {
            val state = settled()
            assertTrue(state.isOffline)
            assertNotNull(state.plan)
            assertTrue(state.plan!!.stops.isNotEmpty())
        }
    }

    @Test
    fun goingOfflineKeepsThePlanAndFlipsTheFlag() = runTest {
        val connectivity = FakeConnectivity(online = true)
        viewModel(FakeCache(trip()), connectivity).uiState.test {
            assertFalse(settled().isOffline)
            connectivity.state.value = false
            val offline = awaitItem()
            assertTrue(offline.isOffline)
            assertNotNull(offline.plan)
        }
    }

    @Test
    fun aPlaceThatClosedAfterPlanningShowsUpAsClosedStop() = runTest {
        val cache = FakeCache(trip())
        val closure = OfficialFact(Closure(TestToday, TestToday, "Acara"), "https://example.org/c", TestToday)
        places.all = places.all.map { if (it.id == "m1") it.copy(closures = listOf(closure)) else it }
        viewModel(cache).uiState.test {
            val state = settled()
            assertEquals(listOf("m1"), state.closedStops.map { it.stop.placeId })
            assertTrue(state.closedStops.single().availability is Availability.TemporarilyClosed)
        }
    }

    @Test
    fun closedPlacesAreOfferedOfflineFromCachedFacts() = runTest {
        val cache = FakeCache(trip())
        val closure = OfficialFact(Closure(TestToday, TestToday, "Acara"), null, TestToday)
        places.all = places.all.map { if (it.id == "m1") it.copy(closures = listOf(closure)) else it }
        viewModel(cache, FakeConnectivity(online = false)).uiState.test {
            val state = settled()
            assertTrue(state.isOffline)
            assertEquals(listOf("m1"), state.closedStops.map { it.stop.placeId })
        }
    }

    @Test
    fun closedStopsFailureIsNotFatal() = runTest {
        val cache = FakeCache(trip())
        places.failing = true
        viewModel(cache).uiState.test {
            val state = settled()
            assertFalse(state.loadError)
            assertNotNull(state.plan)
            assertTrue(state.closedStops.isEmpty())
        }
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() = runTest {
        var fail = true
        val inner = FakeCache(trip())
        val cache = object : PlanCache by inner {
            override val trip: Flow<TripPlan?> = flow {
                if (fail) throw IllegalStateException("cache unreadable")
                emitAll(inner.trip)
            }
        }
        val model = RencanaViewModel(repo(places, events, cache), events, FakeConnectivity(), places, FakeBackpack(), TestClock)
        model.uiState.test {
            val failed = settled()
            assertTrue(failed.loadError)
            assertNull(failed.plan)
            fail = false
            model.retry()
            var state = awaitItem()
            while (state.loadError) state = awaitItem()
            assertFalse(state.loadError)
            assertNotNull(state.plan)
        }
    }

    @Test
    fun currentKeyIsTheStopGoingOnNow() = runTest {
        // Window from 10:00: the museum visit runs 10:00-11:30 and "now" is 10:40.
        val trip = PlanBuilder().buildTrip(choices(start = LocalTime.of(10, 0)), places.all, emptyList(), TestToday)
        viewModel(FakeCache(trip)).uiState.test {
            val state = settled()
            assertEquals("m1", state.currentKey)
        }
    }

    @Test
    fun nextPlannedDayIsShownWhenTodayHasNoPlan() = runTest {
        val tomorrow = choices().copy(startDate = TestToday.plusDays(1), endDate = TestToday.plusDays(1))
        val trip = PlanBuilder().buildTrip(tomorrow, places.all, emptyList(), TestToday)
        viewModel(FakeCache(trip)).uiState.test {
            val state = settled()
            assertEquals(TestToday.plusDays(1), state.plan?.date)
            assertFalse(state.isToday)
            assertNull(state.currentKey)
        }
    }

    @Test
    fun aTripThatIsAlreadyOverIsTheEmptyState() = runTest {
        val past = choices().copy(startDate = TestToday.minusDays(3), endDate = TestToday.minusDays(3))
        val trip = PlanBuilder().buildTrip(past, places.all, emptyList(), TestToday.minusDays(5))
        viewModel(FakeCache(trip)).uiState.test {
            assertNull(settled().plan)
        }
    }

    // ---- Day picker ----

    private fun multiDay(start: java.time.LocalDate, days: Long = 3): TripPlan =
        PlanBuilder().buildTrip(choices().copy(startDate = start, endDate = start.plusDays(days - 1)), places.all, emptyList(), TestToday)

    private fun viewModel(cache: FakeCache, handle: androidx.lifecycle.SavedStateHandle) =
        RencanaViewModel(repo(places, events, cache), events, FakeConnectivity(), places, FakeBackpack(), TestClock, handle)

    @Test
    fun multiDayTripDefaultsToTodayAndOffersEveryDay() = runTest {
        val trip = multiDay(TestToday.minusDays(1))
        viewModel(FakeCache(trip)).uiState.test {
            val state = settled()
            assertEquals(TestToday, state.plan?.date)
            assertTrue(state.showDayPicker)
            assertEquals(trip.days.map { it.date }, state.days.map { it.date })
            assertEquals(trip.days.map { it.stops.size }, state.days.map { it.stopCount })
            assertTrue(state.isDefaultDay)
            assertEquals(2, state.dayNumber)
        }
    }

    @Test
    fun defaultIsTheNextDayWhenTheTripHasNotStarted() = runTest {
        viewModel(FakeCache(multiDay(TestToday.plusDays(2)))).uiState.test {
            val state = settled()
            assertEquals(TestToday.plusDays(2), state.plan?.date)
            assertTrue(state.isDefaultDay)
        }
    }

    @Test
    fun selectingAnotherDayChangesThePlanAndTheHeaderFlag() = runTest {
        val trip = multiDay(TestToday)
        val model = viewModel(FakeCache(trip))
        model.uiState.test {
            assertEquals(TestToday, settled().plan?.date)
            model.selectDay(TestToday.plusDays(2))
            var state = awaitItem()
            while (state.plan?.date != TestToday.plusDays(2)) state = awaitItem()
            assertEquals(trip.days[2].stops, state.plan?.stops)
            assertFalse(state.isDefaultDay)
            assertFalse(state.isToday)
            assertEquals(3, state.dayNumber)
            assertNull(state.currentKey)
        }
    }

    @Test
    fun selectedDaySurvivesANewViewModelWithTheSameSavedState() = runTest {
        val handle = androidx.lifecycle.SavedStateHandle()
        val cache = FakeCache(multiDay(TestToday))
        viewModel(cache, handle).selectDay(TestToday.plusDays(1))
        viewModel(cache, handle).uiState.test {
            assertEquals(TestToday.plusDays(1), settled().plan?.date)
        }
    }

    @Test
    fun aSelectedDayThatIsNotInTheTripFallsBackToTheDefault() = runTest {
        val handle = androidx.lifecycle.SavedStateHandle(mapOf("selected_date" to "2030-01-01"))
        viewModel(FakeCache(multiDay(TestToday)), handle).uiState.test {
            assertEquals(TestToday, settled().plan?.date)
        }
    }

    @Test
    fun aMultiDayTripThatIsOverDefaultsToItsFirstDay() = runTest {
        viewModel(FakeCache(multiDay(TestToday.minusDays(5)))).uiState.test {
            val state = settled()
            assertEquals(TestToday.minusDays(5), state.plan?.date)
            assertTrue(state.showDayPicker)
        }
    }

    @Test
    fun singleDayTripShowsNoPicker() = runTest {
        viewModel(FakeCache(trip())).uiState.test {
            val state = settled()
            assertFalse(state.showDayPicker)
            assertEquals(1, state.days.size)
            assertTrue(state.isDefaultDay)
        }
    }

    @Test
    fun closedCheckFollowsTheSelectedDay() = runTest {
        val model = viewModel(FakeCache(multiDay(TestToday)))
        model.uiState.test {
            settled()
            model.selectDay(TestToday.plusDays(1))
            var state = awaitItem()
            while (state.plan?.date != TestToday.plusDays(1)) state = awaitItem()
            val ids = state.plan!!.stops.map { it.placeId }.toSet()
            assertTrue(state.closedStops.all { it.stop.placeId in ids })
        }
    }
}

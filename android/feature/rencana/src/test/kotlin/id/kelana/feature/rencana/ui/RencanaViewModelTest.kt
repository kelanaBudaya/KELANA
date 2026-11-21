package id.kelana.feature.rencana.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.model.Closure
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import id.kelana.feature.rencana.domain.show
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

    private fun viewModel(cache: FakeCache, connectivity: FakeConnectivity = FakeConnectivity()) =
        RencanaViewModel(repo(places, events, cache, connectivity), events, connectivity, TestClock)

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
    fun closedPlacesAreNotOfferedOfflineBecauseReplanningNeedsTheServer() = runTest {
        val closure = OfficialFact(Closure(TestToday, TestToday, "Acara"), null, TestToday)
        places.all = places.all.map { if (it.id == "m1") it.copy(closures = listOf(closure)) else it }
        viewModel(FakeCache(trip()), FakeConnectivity(online = false)).uiState.test {
            assertTrue(settled().closedStops.isEmpty())
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
}

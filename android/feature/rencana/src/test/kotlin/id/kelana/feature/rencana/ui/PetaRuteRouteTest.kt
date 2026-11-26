package id.kelana.feature.rencana.ui

import app.cash.turbine.test
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanStop
import id.kelana.core.model.RouteLeg
import id.kelana.core.model.RoutePoint
import id.kelana.core.model.RouteRepository
import id.kelana.core.model.Travel
import id.kelana.core.model.TravelMode
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

/** Offline or not configured: every request fails. */
internal object NoRoutes : RouteRepository {
    override suspend fun route(mode: TravelMode, points: List<RoutePoint>): List<RouteLeg>? = null
}

/** Answers once [gate] opens; each leg bends through a midpoint so the road path differs from the straight line. */
private class GatedRoutes(val gate: CompletableDeferred<Unit> = CompletableDeferred()) : RouteRepository {
    val calls = mutableListOf<Pair<TravelMode, List<RoutePoint>>>()

    override suspend fun route(mode: TravelMode, points: List<RoutePoint>): List<RouteLeg>? {
        calls += mode to points
        gate.await()
        return points.zipWithNext { a, b -> RouteLeg(60, 100, listOf(a, RoutePoint(a.lat, b.lng), b)) }
    }
}

class PetaRuteRouteTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val places = FakePlaces(
        listOf(
            place("a", lat = -7.800, hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))),
            place("b", PlaceCategory.Museum, lat = -7.801, lng = 110.37, hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))),
        ),
    )
    private val trip = PlanBuilder().buildTrip(choices(start = LocalTime.of(10, 0)), places.all, emptyList(), TestToday)

    @Test
    fun straightLineFirstThenTheRoadRoute() = runTest {
        val routes = GatedRoutes()
        PetaRuteViewModel(repo(places, cache = FakeCache(trip)), routes, TestClock).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(2, s.stops.size)
            assertEquals(s.stops.map { RoutePoint(it.lat, it.lng) }, s.routePath)
            routes.gate.complete(Unit)
            val road = awaitItem()
            assertEquals(3, road.routePath.size)
            assertEquals(s.routePath.first(), road.routePath.first())
            assertEquals(s.routePath.last(), road.routePath.last())
            assertEquals(1, routes.calls.size)
        }
    }

    @Test
    fun failureKeepsTheStraightLine() = runTest {
        PetaRuteViewModel(repo(places, cache = FakeCache(trip)), NoRoutes, TestClock).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(2, s.routePath.size)
            expectNoEvents()
        }
    }

    @Test
    fun noPlanMeansNoPath() = runTest {
        PetaRuteViewModel(repo(places), GatedRoutes(), TestClock).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertTrue(s.routePath.isEmpty())
        }
    }

    private fun stop(mode: TravelMode?) = trip.days.first().stops.first().copy(travel = mode?.let { Travel(it, 5) })

    @Test
    fun consecutiveLegsWithTheSameModeShareOneRequest() {
        val stops: List<PlanStop> = listOf(stop(null), stop(TravelMode.Car), stop(TravelMode.Transit), stop(TravelMode.Walk), stop(TravelMode.Walk))
        assertEquals(
            listOf(LegGroup(TravelMode.Car, 0, 1), LegGroup(TravelMode.Transit, 1, 2), LegGroup(TravelMode.Walk, 2, 4)),
            legGroups(stops),
        )
        assertTrue(legGroups(listOf(stop(null))).isEmpty())
        assertEquals(listOf(LegGroup(TravelMode.Car, 0, 2)), legGroups(listOf(stop(null), stop(null), stop(TravelMode.Car))))
    }

    @Test
    fun joiningLegsDropsTheRepeatedJunctionPoint() {
        val a = RoutePoint(0.0, 0.0)
        val b = RoutePoint(1.0, 1.0)
        val c = RoutePoint(2.0, 2.0)
        val m = RoutePoint(0.5, 0.5)
        assertEquals(listOf(a, m, b, c), joinLegs(listOf(RouteLeg(1, 1, listOf(a, m, b)), RouteLeg(1, 1, listOf(b, c)))))
    }
}

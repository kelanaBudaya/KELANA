package id.kelana.feature.rencana.data

import id.kelana.core.model.Closure
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlaceCategory
import id.kelana.feature.rencana.domain.BuildResult
import id.kelana.feature.rencana.domain.ReplanOutcome
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import id.kelana.feature.rencana.ui.FakeCache
import id.kelana.feature.rencana.ui.FakeConnectivity
import id.kelana.feature.rencana.ui.FakeEvents
import id.kelana.feature.rencana.ui.FakePlaces
import id.kelana.feature.rencana.ui.TestToday
import id.kelana.feature.rencana.ui.repo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class LocalPlanRepositoryTest {
    private val open = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))
    private val places = FakePlaces(
        listOf(place("a", lat = -7.800, hours = open), place("b", lat = -7.801, hours = open)),
    )
    private val connectivity = FakeConnectivity()
    private val cache = FakeCache()
    private val plans = repo(places, FakeEvents(emptyList()), cache, connectivity)

    @Test
    fun buildOnlineSavesThePlanForOfflineReading() = runTest {
        val result = plans.build(choices())
        assertTrue(result is BuildResult.Built)
        connectivity.state.value = false
        // Reading needs no signal.
        val cached = plans.trip.first()
        assertNotNull(cached)
        assertEquals(listOf("a", "b"), cached!!.days.single().stops.map { it.placeId })
    }

    @Test
    fun buildOfflineAnswersOfflineAndKeepsTheOldPlan() = runTest {
        plans.build(choices())
        val before = cache.tripState.value
        connectivity.state.value = false
        assertEquals(BuildResult.Offline, plans.build(choices(setOf(Interest.Candi))))
        assertEquals(before, cache.tripState.value)
    }

    @Test
    fun nothingThatFitsIsReportedAndNotSaved() = runTest {
        assertEquals(BuildResult.NothingFits, plans.build(choices(setOf(Interest.Topeng))))
        assertNull(cache.tripState.value)
    }

    @Test
    fun replanOfflineAnswersOfflineAndChangesNothing() = runTest {
        plans.build(choices())
        val before = cache.tripState.value
        connectivity.state.value = false
        assertEquals(ReplanOutcome.Offline, plans.applyReplan(TestToday, "a", replace = true))
        assertEquals(before, cache.tripState.value)
    }

    @Test
    fun closedStopsAndPreviewWorkOfflineFromCachedData() = runTest {
        plans.build(choices(pace = id.kelana.core.model.Pace.Moderate))
        val closure = OfficialFact(Closure(TestToday, TestToday, "x"), null, TestToday)
        places.all = places.all.map { if (it.id == "a") it.copy(closures = listOf(closure)) else it } +
            place("c", PlaceCategory.Museum, lat = -7.802, hours = open)
        connectivity.state.value = false
        assertEquals(listOf("a"), plans.closedStops(TestToday).map { it.stop.placeId })
        assertEquals("c", plans.previewReplacement(TestToday, "a")?.placeId)
    }

    @Test
    fun replanOfAnUnknownStopIsNotFound() = runTest {
        plans.build(choices())
        assertEquals(ReplanOutcome.NotFound, plans.applyReplan(TestToday, "zzz", replace = false))
        assertEquals(ReplanOutcome.NotFound, plans.applyReplan(TestToday.plusDays(1), "a", replace = false))
    }
}

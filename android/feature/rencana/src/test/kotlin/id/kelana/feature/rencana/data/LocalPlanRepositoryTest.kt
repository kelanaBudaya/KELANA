package id.kelana.feature.rencana.data

import id.kelana.core.model.Closure
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.feature.rencana.domain.PlanBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import id.kelana.feature.rencana.domain.BuildResult
import id.kelana.feature.rencana.domain.ReplanOutcome
import id.kelana.feature.rencana.domain.choices
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import id.kelana.feature.rencana.ui.FakeCache
import id.kelana.feature.rencana.ui.TestClock
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

@OptIn(ExperimentalCoroutinesApi::class)
class LocalPlanRepositoryTest {
    private val open = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0))
    private val places = FakePlaces(
        listOf(place("a", lat = -7.800, hours = open), place("b", lat = -7.801, hours = open)),
    )
    private val cache = FakeCache()
    private val plans = repo(places, FakeEvents(emptyList()), cache)

    @Test
    fun buildOnlineSavesThePlanForOfflineReading() = runTest {
        val result = plans.build(choices())
        assertTrue(result is BuildResult.Built)
        // Reading needs no signal.
        val cached = plans.trip.first()
        assertNotNull(cached)
        assertEquals(listOf("a", "b"), cached!!.days.single().stops.map { it.placeId })
    }

    @Test
    fun buildOfflineUsesCachedDataAndSavesThePlan() = runTest {
        // The repository no longer looks at connectivity: the cached places are enough.
        val result = plans.build(choices(setOf(Interest.Museum)))
        assertTrue(result is BuildResult.Built)
        assertEquals(result.let { (it as BuildResult.Built).trip }, cache.tripState.value)
    }

    @Test
    fun buildWithNoPlacesAnswersNoData() = runTest {
        val empty = repo(FakePlaces(emptyList()), FakeEvents(emptyList()), cache)
        assertEquals(BuildResult.NoData, empty.build(choices()))
        assertNull(cache.tripState.value)
    }

    @Test
    fun buildWaitsTenSecondsForPlacesThatNeverArrive() = runTest {
        val hanging = object : PlaceRepository by places {
            override fun places(): Flow<List<Place>> = flow {
                emit(emptyList())
                awaitCancellation()
            }
        }
        val hangingRepo = LocalPlanRepository(hanging, FakeEvents(emptyList()), cache, PlanBuilder(), TestClock)
        assertEquals(BuildResult.NoData, hangingRepo.build(choices()))
        assertEquals(10_000L, currentTime)
    }

    @Test
    fun buildUsesPlacesThatArriveAfterTheEmptyCache() = runTest {
        val late = object : PlaceRepository by places {
            override fun places(): Flow<List<Place>> = flow {
                emit(emptyList())
                delay(2_000)
                emit(places.all)
            }
        }
        val lateRepo = LocalPlanRepository(late, FakeEvents(emptyList()), cache, PlanBuilder(), TestClock)
        assertTrue(lateRepo.build(choices()) is BuildResult.Built)
    }

    @Test
    fun buildThatThrowsAnswersFailedAndKeepsOldPlan() = runTest {
        plans.build(choices())
        val before = cache.tripState.value
        places.failing = true
        assertEquals(BuildResult.Failed, plans.build(choices(setOf(Interest.Candi))))
        assertEquals(before, cache.tripState.value)
    }

    @Test
    fun nothingThatFitsIsReportedAndNotSaved() = runTest {
        assertEquals(BuildResult.NothingFits, plans.build(choices(setOf(Interest.Topeng))))
        assertNull(cache.tripState.value)
    }

    @Test
    fun replanOfflineAppliesFromCachedData() = runTest {
        plans.build(choices())
        val applied = plans.applyReplan(TestToday, "a", replace = false)
        assertTrue(applied is ReplanOutcome.Applied)
        assertEquals(listOf("b"), cache.tripState.value!!.days.single().stops.map { it.placeId })
    }

    @Test
    fun replanThatThrowsAnswersFailed() = runTest {
        plans.build(choices())
        val before = cache.tripState.value
        places.failing = true
        assertEquals(ReplanOutcome.Failed, plans.applyReplan(TestToday, "a", replace = true))
        assertEquals(before, cache.tripState.value)
    }

    @Test
    fun closedStopsAndPreviewWorkOfflineFromCachedData() = runTest {
        plans.build(choices(pace = id.kelana.core.model.Pace.Moderate))
        val closure = OfficialFact(Closure(TestToday, TestToday, "x"), null, TestToday)
        places.all = places.all.map { if (it.id == "a") it.copy(closures = listOf(closure)) else it } +
            place("c", PlaceCategory.Museum, lat = -7.802, hours = open)
        assertEquals(listOf("a"), plans.closedStops(TestToday).map { it.stop.placeId })
        assertEquals("c", plans.previewReplacement(TestToday, "a")?.placeId)
    }

    @Test
    fun replanOfAnUnknownStopIsNotFound() = runTest {
        plans.build(choices())
        assertEquals(ReplanOutcome.NotFound, plans.applyReplan(TestToday, "zzz", replace = false))
        assertEquals(ReplanOutcome.NotFound, plans.applyReplan(TestToday.plusDays(1), "a", replace = false))
    }

    @Test
    fun previewReplacementDoesNotReturnAPlaceOfAnotherDay() = runTest {
        val day2 = TestToday.plusDays(1)
        val four = FakePlaces(
            listOf(
                place("a", lat = -7.800, hours = open),
                place("b", lat = -7.801, hours = open),
                place("c", lat = -7.802, hours = open),
                place("d", lat = -7.803, hours = open),
            ),
        )
        val repo = repo(four, FakeEvents(emptyList()), cache)
        val ch = choices(pace = id.kelana.core.model.Pace.Relaxed).copy(endDate = day2)
        val built = repo.build(ch) as BuildResult.Built
        assertEquals(listOf(3, 1), built.trip.days.map { it.stops.size })
        val closed = built.trip.days[1].stops.single().placeId
        val closure = OfficialFact(Closure(day2, day2, "x"), null, TestToday)
        four.all = four.all.map { if (it.id == closed) it.copy(closures = listOf(closure)) else it }
        // The three other places are all planned on day 1, so there is nothing left to offer on day 2.
        assertNull(repo.previewReplacement(day2, closed))
    }
}

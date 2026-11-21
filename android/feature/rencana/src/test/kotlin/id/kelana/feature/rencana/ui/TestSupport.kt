package id.kelana.feature.rencana.ui

import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.PlanCache
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.data.LocalPlanRepository
import id.kelana.feature.rencana.domain.PlanBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Friday 20 November 2026, 10:40 in Jakarta. */
internal val TestZone: ZoneId = ZoneId.of("Asia/Jakarta")
internal val TestClock: Clock = Clock.fixed(Instant.parse("2026-11-20T03:40:00Z"), TestZone)
internal val TestToday: LocalDate = LocalDate.of(2026, 11, 20)

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

internal class FakeConnectivity(online: Boolean = true) : ConnectivityMonitor {
    val state = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = state
    override fun isOnlineNow(): Boolean = state.value
}

internal class FakeCache(initial: TripPlan? = null) : PlanCache {
    val tripState = MutableStateFlow(initial)
    val reminderState = MutableStateFlow<Map<String, ReminderChoice>>(emptyMap())
    override val trip: Flow<TripPlan?> = tripState
    override val reminders: Flow<Map<String, ReminderChoice>> = reminderState
    override suspend fun save(trip: TripPlan) {
        tripState.value = trip
    }

    override suspend fun clear() {
        tripState.value = null
    }

    override suspend fun saveReminder(eventId: String, choice: ReminderChoice) {
        reminderState.update { it + (eventId to choice) }
    }
}

internal class FakePlaces(var all: List<Place>) : PlaceRepository {
    override fun places(): Flow<List<Place>> = flowOf(all)
    override fun place(id: String): Flow<Place?> = flowOf(all.firstOrNull { it.id == id })
    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(all.filter { it.category == category })
    override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
}

internal class FakeEvents(private val all: List<Event>) : EventRepository {
    override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = eventsBetween(date, date)

    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        flowOf(all.flatMap { e -> e.occurrencesBetween(from, to).map { EventOccurrence(e, it) } })

    override fun event(id: String): Flow<Event?> = flowOf(all.firstOrNull { it.id == id })
}

/** The real on-device repository over fakes, so view model tests exercise the real planner. */
internal fun repo(
    places: FakePlaces,
    events: FakeEvents = FakeEvents(emptyList()),
    cache: FakeCache = FakeCache(),
    connectivity: FakeConnectivity = FakeConnectivity(),
) = LocalPlanRepository(places, events, cache, connectivity, PlanBuilder(), TestClock)

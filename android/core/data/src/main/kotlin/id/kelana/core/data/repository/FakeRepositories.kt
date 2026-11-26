package id.kelana.core.data.repository

import id.kelana.core.data.sample.SampleData
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.Region
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.ReportDraft
import id.kelana.core.model.ReportRepository
import id.kelana.core.model.ReportResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/*
 * In-memory fakes backed by SampleData (sample data, not verified). No network.
 * Replace the bindings in di/RepositoryModule.kt to switch to the Supabase-backed implementations.
 */

@Singleton
class FakePlaceRepository @Inject constructor() : PlaceRepository {
    private val all: List<Place> = SampleData.places

    override fun places(): Flow<List<Place>> = flowOf(all)

    override fun place(id: String): Flow<Place?> = flowOf(all.firstOrNull { it.id == id })

    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> =
        flowOf(all.filter { it.category == category })

    override fun search(query: String): Flow<List<Place>> {
        return flowOf(all.searchPlaces(query))
    }
}

@Singleton
class FakeEventRepository @Inject constructor() : EventRepository {
    private val all: List<Event> = SampleData.events

    override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = eventsBetween(date, date)

    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        flowOf(expandOccurrences(all, from, to))

    override fun event(id: String): Flow<Event?> = flowOf(all.firstOrNull { it.id == id })

    override fun eventsAt(placeId: String): Flow<List<Event>> = flowOf(all.filter { it.placeId == placeId })
}

@Singleton
class FakeRegionRepository @Inject constructor() : RegionRepository {
    private val all: List<Region> = SampleData.regions.sortedBy { it.sortOrder }
    private val selected = MutableStateFlow<String?>(null)

    override fun regions(): Flow<List<Region>> = flowOf(all)

    override val selectedRegionId: Flow<String?> = selected

    override suspend fun select(regionId: String) {
        if (all.any { it.id == regionId && it.isAvailable }) selected.value = regionId
    }
}

/** Guests (and a session that is still loading) get NeedsLogin. Nothing is stored. */
@Singleton
class FakeReportRepository @Inject constructor(
    private val session: SessionRepository,
) : ReportRepository {
    override suspend fun submit(report: ReportDraft): ReportResult {
        val state = session.sessionState.first { it != SessionState.Loading }
        return if (state is SessionState.SignedIn) ReportResult.Success else ReportResult.NeedsLogin
    }
}

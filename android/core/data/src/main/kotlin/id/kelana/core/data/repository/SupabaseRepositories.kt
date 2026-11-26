@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package id.kelana.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.kelana.core.data.remote.KelanaRemoteDataSource
import id.kelana.core.data.remote.ReportInsertDto
import id.kelana.core.data.remote.toCancelledDays
import id.kelana.core.data.remote.toEntity
import id.kelana.core.data.remote.hostRegionCode
import id.kelana.core.data.remote.toEvent
import id.kelana.core.data.remote.toEventClosureEntity
import id.kelana.core.data.remote.toFactEntities
import id.kelana.core.data.remote.toModel
import id.kelana.core.data.remote.toPlaces
import id.kelana.core.database.dao.EventDao
import id.kelana.core.database.dao.PlaceDao
import id.kelana.core.database.dao.RegionDao
import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Supabase-backed repositories. Every Flow emits the Room cache first, then refreshes from the network and emits
 * again if the cache changed. On a network failure (or when Supabase is not configured) the cache is kept as is,
 * so `checkedAt` is never touched. The cache holds public content only: no location, no visitor content.
 */

private class PlaceRows(val places: List<PlaceEntity>, val facts: List<PlaceFactEntity>)

@Singleton
class SupabasePlaceRepository internal constructor(
    private val remote: KelanaRemoteDataSource,
    private val dao: PlaceDao,
    private val regions: RegionRepository,
    private val gate: RefreshGate,
) : PlaceRepository {

    @Inject
    constructor(remote: KelanaRemoteDataSource, dao: PlaceDao, regions: RegionRepository) :
        this(remote, dao, regions, RefreshGate())

    private val all: Flow<List<Place>> = cacheThenNetwork(
        cache = combine(dao.observePlaces(), dao.observeFacts()) { p, f -> PlaceRows(p, f) },
        hasData = { it.places.isNotEmpty() },
        gate = gate,
        refresh = ::refresh,
        map = { toPlaces(it.places, it.facts) },
    ).distinctUntilChanged()

    /** The one owner of the region invariant: every list below is scoped to the selected region (null = all). */
    private val scoped: Flow<List<Place>> =
        regions.selectedRegionId.flatMapLatest { selected ->
            all.map { list -> list.inRegion(selected) { it.regionId } }
        }.distinctUntilChanged()

    override fun places(): Flow<List<Place>> = scoped

    /** Not scoped: a place resolves by id from any region (deep links, passport of earlier visits). */
    override fun place(id: String): Flow<Place?> = all.map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> =
        scoped.map { list -> list.filter { it.category == category } }

    override fun search(query: String): Flow<List<Place>> = scoped.map { it.searchPlaces(query) }

    /** Fetches everything first and writes once, so a failure halfway leaves the old cache untouched. */
    private suspend fun refresh() {
        val payload = remote.fetchPlaces()
        dao.replaceAll(payload.places.mapNotNull { it.toEntity() }, payload.toFactEntities())
    }
}

private class EventRows(val events: List<EventEntity>, val closures: List<EventClosureEntity>)

@Singleton
class SupabaseEventRepository internal constructor(
    private val remote: KelanaRemoteDataSource,
    private val dao: EventDao,
    private val regions: RegionRepository,
    private val gate: RefreshGate,
) : EventRepository {

    @Inject
    constructor(remote: KelanaRemoteDataSource, dao: EventDao, regions: RegionRepository) :
        this(remote, dao, regions, RefreshGate())

    private val rows: Flow<EventRows> = cacheThenNetwork(
        cache = combine(dao.observeEvents(), dao.observeClosures()) { e, c -> EventRows(e, c) },
        hasData = { it.events.isNotEmpty() },
        gate = gate,
        refresh = ::refresh,
        map = { it },
    )

    override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = eventsBetween(date, date)

    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        // flatMapLatest, not combine: combine would conflate the cache and network emissions.
        regions.selectedRegionId.flatMapLatest { selected ->
            rows.map { r ->
                val events = r.events.inRegion(selected) { it.hostRegionCode() }.mapNotNull { it.toEvent() }
                expandOccurrences(events, from, to, toCancelledDays(r.closures))
            }
        }.distinctUntilChanged()

    override fun event(id: String): Flow<Event?> =
        rows.map { r -> r.events.firstOrNull { it.id == id }?.toEvent() }.distinctUntilChanged()

    override fun eventsAt(placeId: String): Flow<List<Event>> =
        rows.map { r -> r.events.mapNotNull { it.toEvent() }.filter { it.placeId == placeId } }.distinctUntilChanged()

    private suspend fun refresh() {
        val payload = remote.fetchEvents()
        dao.replaceAll(
            payload.events.map { it.toEntity() },
            payload.closures.mapIndexed { i, c -> c.toEventClosureEntity(i) },
        )
    }
}

@Singleton
class SupabaseRegionRepository internal constructor(
    private val remote: KelanaRemoteDataSource,
    private val dao: RegionDao,
    private val dataStore: DataStore<Preferences>,
    private val gate: RefreshGate,
) : RegionRepository {

    @Inject
    constructor(remote: KelanaRemoteDataSource, dao: RegionDao, dataStore: DataStore<Preferences>) :
        this(remote, dao, dataStore, RefreshGate())

    override fun regions(): Flow<List<Region>> = cacheThenNetwork(
        cache = dao.observeAll(),
        hasData = { it.isNotEmpty() },
        gate = gate,
        refresh = ::refresh,
        map = { rows -> rows.map { it.toModel() } },
    ).distinctUntilChanged()

    override val selectedRegionId: Flow<String?> = dataStore.data.map { it[KEY_SELECTED] }.distinctUntilChanged()

    /** Only an available region is stored. Without a cached list yet, one refresh is tried before deciding. */
    override suspend fun select(regionId: String) {
        var known = dao.getAll()
        if (known.isEmpty()) {
            gate.run(::refresh)
            known = dao.getAll()
        }
        if (known.any { it.code == regionId && it.status == "available" }) {
            dataStore.edit { it[KEY_SELECTED] = regionId }
        }
    }

    private suspend fun refresh() {
        dao.replaceAll(remote.fetchRegions().map { it.toEntity() })
    }

    private companion object {
        val KEY_SELECTED = stringPreferencesKey("selected_region_id")
    }
}

/**
 * Guests get [ReportResult.NeedsLogin]. A logged-in user inserts into `reports` (RLS requires user_id = self).
 * [ReportDraft] carries no photo yet, so nothing is uploaded to `report-photos`.
 */
@Singleton
class SupabaseReportRepository @Inject constructor(
    private val session: SessionRepository,
    private val remote: KelanaRemoteDataSource,
) : ReportRepository {

    override suspend fun submit(report: ReportDraft): ReportResult {
        val state = session.sessionState.first { it != SessionState.Loading }
        if (state !is SessionState.SignedIn) return ReportResult.NeedsLogin
        return try {
            remote.insertReport(
                ReportInsertDto(
                    userId = state.user.id,
                    placeId = report.placeId,
                    reason = report.reason.key,
                    details = report.details?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DETAILS),
                ),
            )
            ReportResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ReportResult.Failed
        }
    }

    private companion object {
        const val MAX_DETAILS = 1000
    }
}

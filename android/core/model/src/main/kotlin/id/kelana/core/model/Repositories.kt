package id.kelana.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface PlaceRepository {
    /** All places, in passport order. */
    fun places(): Flow<List<Place>>

    /** Null when no place has this id. */
    fun place(id: String): Flow<Place?>

    fun placesByCategory(category: PlaceCategory): Flow<List<Place>>

    /** Case-insensitive match on name, address and description. A blank query returns an empty list. */
    fun search(query: String): Flow<List<Place>>
}

interface EventRepository {
    /** Events taking place on [date], ordered by start time. */
    fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>>

    /** Events taking place in `[from, to]` inclusive, ordered by date then start time. */
    fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>>

    /** Null when no event has this id. */
    fun event(id: String): Flow<Event?>

    /**
     * Events hosted at the place [placeId], any recurrence (past one-time events included: the caller filters by
     * date). The default derives them from [eventsBetween] over the next year; implementations backed by a store
     * should override it with a direct query.
     */
    fun eventsAt(placeId: String): Flow<List<Event>> {
        val from = LocalDate.now()
        return eventsBetween(from, from.plusDays(365)).map { list ->
            list.filter { it.event.placeId == placeId }.map { it.event }.distinctBy { it.id }
        }
    }
}

interface RegionRepository {
    fun regions(): Flow<List<Region>>

    /** The region picked in Pilih daerah, or null before the choice. PROVISIONAL: the fake keeps it in memory only. */
    val selectedRegionId: Flow<String?>

    /** Only an available region can be selected; others are ignored. */
    suspend fun select(regionId: String)
}

interface ReportRepository {
    /** Guests get [ReportResult.NeedsLogin]. */
    suspend fun submit(report: ReportDraft): ReportResult
}

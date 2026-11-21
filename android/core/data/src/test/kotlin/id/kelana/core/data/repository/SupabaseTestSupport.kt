package id.kelana.core.data.repository

import id.kelana.core.data.remote.ClosureDto
import id.kelana.core.data.remote.EventDto
import id.kelana.core.data.remote.EventsPayload
import id.kelana.core.data.remote.KelanaRemoteDataSource
import id.kelana.core.data.remote.OpeningHoursDto
import id.kelana.core.data.remote.PlaceDto
import id.kelana.core.data.remote.PlaceNameDto
import id.kelana.core.data.remote.PlacesPayload
import id.kelana.core.data.remote.RegionDto
import id.kelana.core.data.remote.ReportInsertDto
import id.kelana.core.data.remote.TicketPriceDto
import id.kelana.core.database.dao.EventDao
import id.kelana.core.database.dao.PlaceDao
import id.kelana.core.database.dao.RegionDao
import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
import id.kelana.core.database.entity.RegionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** Fake network: no real connection in unit tests. Set [fail] to simulate being offline. */
class FakeRemote : KelanaRemoteDataSource {
    var fail: Boolean = false
    var regions: List<RegionDto> = emptyList()
    var places: PlacesPayload = PlacesPayload(emptyList(), emptyList(), emptyList(), emptyList())
    var events: EventsPayload = EventsPayload(emptyList(), emptyList())
    val reports = mutableListOf<ReportInsertDto>()
    var fetchCount = 0

    private fun check() {
        fetchCount++
        if (fail) throw IOException("offline")
    }

    override suspend fun fetchRegions(): List<RegionDto> { check(); return regions }

    override suspend fun fetchPlaces(): PlacesPayload { check(); return places }

    override suspend fun fetchEvents(): EventsPayload { check(); return events }

    override suspend fun insertReport(report: ReportInsertDto) {
        if (fail) throw IOException("offline")
        reports += report
    }
}

class FakeRegionDao : RegionDao() {
    val rows = MutableStateFlow<List<RegionEntity>>(emptyList())
    override fun observeAll(): Flow<List<RegionEntity>> = rows
    override suspend fun getAll(): List<RegionEntity> = rows.value
    override suspend fun clear() { rows.value = emptyList() }
    override suspend fun insertAll(rows: List<RegionEntity>) { this.rows.value = rows }
    override suspend fun replaceAll(rows: List<RegionEntity>) { this.rows.value = rows }
}

class FakePlaceDao : PlaceDao() {
    val places = MutableStateFlow<List<PlaceEntity>>(emptyList())
    val facts = MutableStateFlow<List<PlaceFactEntity>>(emptyList())
    override fun observePlaces(): Flow<List<PlaceEntity>> = places
    override fun observeFacts(): Flow<List<PlaceFactEntity>> = facts
    override suspend fun clearPlaces() { places.value = emptyList() }
    override suspend fun clearFacts() { facts.value = emptyList() }
    override suspend fun insertPlaces(rows: List<PlaceEntity>) { places.value = rows }
    override suspend fun insertFacts(rows: List<PlaceFactEntity>) { facts.value = rows }
    override suspend fun replaceAll(places: List<PlaceEntity>, facts: List<PlaceFactEntity>) {
        this.places.value = places
        this.facts.value = facts
    }
}

class FakeEventDao : EventDao() {
    val events = MutableStateFlow<List<EventEntity>>(emptyList())
    val closures = MutableStateFlow<List<EventClosureEntity>>(emptyList())
    override fun observeEvents(): Flow<List<EventEntity>> = events
    override fun observeClosures(): Flow<List<EventClosureEntity>> = closures
    override suspend fun clearEvents() { events.value = emptyList() }
    override suspend fun clearClosures() { closures.value = emptyList() }
    override suspend fun insertEvents(rows: List<EventEntity>) { events.value = rows }
    override suspend fun insertClosures(rows: List<EventClosureEntity>) { closures.value = rows }
    override suspend fun replaceAll(events: List<EventEntity>, closures: List<EventClosureEntity>) {
        this.events.value = events
        this.closures.value = closures
    }
}

/** A gate that never skips a refresh, so tests control exactly when the network is hit. */
internal fun testGate() = RefreshGate(minIntervalMs = 0L)

fun placeDto(id: String = "p1", name: String = "Candi Prambanan", order: Int? = 1) = PlaceDto(
    id = id, slug = "slug-$id", name = name, category = "candi", capCode = "YK-01",
    lat = -7.75, lng = 110.49, address = "Jl. Raya", description = "Kompleks candi", coverPath = null, passportOrder = order,
)

fun hoursDto(placeId: String = "p1", weekday: Int = 1, checkedAt: String? = "2026-11-20T03:00:00+00:00") = OpeningHoursDto(
    placeId = placeId, weekday = weekday, opensAt = "08:00:00", closesAt = "17:00:00", isClosed = false,
    sourceUrl = "https://example.test/jam", checkedAt = checkedAt,
)

fun ticketDto(placeId: String = "p1", checkedAt: String? = "2026-11-20T03:00:00+00:00") = TicketPriceDto(
    placeId = placeId, label = "Dewasa", priceIdr = 50000, sellerUrl = "https://example.test/tiket",
    sourceUrl = "https://example.test/harga", checkedAt = checkedAt,
)

fun closureDto(placeId: String = "p1", start: String = "2026-12-01", end: String = "2026-12-01", checkedAt: String? = "2026-11-20T03:00:00+00:00") =
    ClosureDto(placeId = placeId, startsOn = start, endsOn = end, reason = "Upacara", sourceUrl = "https://example.test/tutup", checkedAt = checkedAt)

fun eventDto(
    id: String = "e1",
    recurrence: String = "weekly",
    weekday: Int? = 5,
    date: String? = null,
    placeId: String = "p1",
    start: String = "19:30:00",
) = EventDto(
    id = id, placeId = placeId, title = "Sendratari Ramayana", description = null, category = "tari",
    recurrence = recurrence, eventDate = date, weekday = weekday, startTime = start, endTime = "21:30:00",
    priceFromIdr = 150000, sellerUrl = "https://example.test/beli", sourceUrl = "https://example.test/jadwal",
    checkedAt = "2026-11-20T03:00:00+00:00", performers = listOf("Penari lokal"), arrivalAdviceMin = 30,
    arrivalAdviceMax = 45, tags = listOf("tari", "bukan_tag"), places = PlaceNameDto("Candi Prambanan"),
)

fun regionDto(code: String, status: String, order: Int) = RegionDto(code, "Wilayah $code", status, order)

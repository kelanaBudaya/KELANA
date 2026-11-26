package id.kelana.core.data.remote

import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
import id.kelana.core.database.entity.RegionEntity
import id.kelana.core.model.Closure
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTag
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus
import id.kelana.core.model.TicketPrice
import kotlinx.serialization.json.Json
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** Times in the database are Asia/Jakarta wall-clock; a check timestamp is shown as a Jakarta calendar date. */
internal val JakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")

internal val CacheJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal const val KIND_HOURS = "hours"
internal const val KIND_CLOSURE = "closure"
internal const val KIND_TICKET = "ticket"

/**
 * `checked_at` is a timestamptz. Null, blank or unparseable gives null, which the model renders as
 * "sedang dicek". A date is never invented.
 */
internal fun parseCheckedAt(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(raw.trim()).atZoneSameInstant(JakartaZone).toLocalDate()
    } catch (_: DateTimeException) {
        try {
            LocalDate.parse(raw.trim().take(10))
        } catch (_: DateTimeException) {
            null
        }
    }
}

internal fun parseTime(raw: String?): LocalTime? =
    if (raw.isNullOrBlank()) null else try { LocalTime.parse(raw.trim()) } catch (_: DateTimeException) { null }

internal fun parseDate(raw: String?): LocalDate? =
    if (raw.isNullOrBlank()) null else try { LocalDate.parse(raw.trim()) } catch (_: DateTimeException) { null }

/** ISO weekday: 1 = Senin ... 7 = Minggu. Anything else is null. */
internal fun weekdayOf(n: Int?): DayOfWeek? = n?.takeIf { it in 1..7 }?.let(DayOfWeek::of)

internal fun parseRegionStatus(raw: String): RegionStatus = when (raw) {
    "available" -> RegionStatus.Available
    "upcoming" -> RegionStatus.Upcoming
    else -> RegionStatus.NotOpen
}

// ---------- regions ----------

internal fun RegionDto.toEntity() = RegionEntity(code = code, name = name, status = status, sortOrder = sortOrder)

internal fun RegionEntity.toModel() = Region(
    id = code,
    name = name,
    status = parseRegionStatus(status),
    sortOrder = sortOrder,
)

// ---------- places ----------

/** A place without coordinates is skipped (the column is generated from a required location, so this is defensive). */
internal fun PlaceDto.toEntity(): PlaceEntity? {
    val lat = lat ?: return null
    val lng = lng ?: return null
    return PlaceEntity(
        id = id,
        slug = slug,
        name = name,
        category = category,
        capCode = capCode,
        stampName = stampName,
        lat = lat,
        lng = lng,
        address = address.orEmpty(),
        description = description.orEmpty(),
        coverKey = coverPath?.takeIf { it.isNotBlank() } ?: slug,
        passportOrder = passportOrder,
        regionCode = regions?.code,
    )
}

internal fun PlacesPayload.toFactEntities(): List<PlaceFactEntity> =
    hours.mapIndexed { i, h ->
        PlaceFactEntity("$KIND_HOURS:${h.placeId}:${h.weekday}:$i", h.placeId, KIND_HOURS, CacheJson.encodeToString(OpeningHoursDto.serializer(), h))
    } + closures.mapIndexed { i, c ->
        PlaceFactEntity("$KIND_CLOSURE:${c.placeId}:${c.startsOn}:$i", c.placeId, KIND_CLOSURE, CacheJson.encodeToString(ClosureDto.serializer(), c))
    } + tickets.mapIndexed { i, t ->
        PlaceFactEntity("$KIND_TICKET:${t.placeId}:$i", t.placeId, KIND_TICKET, CacheJson.encodeToString(TicketPriceDto.serializer(), t))
    }

internal fun OpeningHoursDto.toFact(): OfficialFact<OpeningHours>? {
    val day = weekdayOf(weekday) ?: return null
    return OfficialFact(
        value = OpeningHours(
            weekday = day,
            isClosed = isClosed,
            opens = if (isClosed) null else parseTime(opensAt),
            closes = if (isClosed) null else parseTime(closesAt),
            note = note,
        ),
        sourceUrl = sourceUrl,
        checkedAt = parseCheckedAt(checkedAt),
    )
}

internal fun ClosureDto.toFact(): OfficialFact<Closure>? {
    val start = parseDate(startsOn) ?: return null
    val end = parseDate(endsOn) ?: return null
    return OfficialFact(Closure(start, end, reason), sourceUrl, parseCheckedAt(checkedAt))
}

internal fun TicketPriceDto.toFact() = OfficialFact(
    value = TicketPrice(label = label, priceIdr = priceIdr, sellerUrl = sellerUrl, onSiteAvailable = onSiteAvailable),
    sourceUrl = sourceUrl,
    checkedAt = parseCheckedAt(checkedAt),
)

/** Builds places with their facts. Rows with an unreadable payload are skipped, never replaced by guesses. */
internal fun toPlaces(places: List<PlaceEntity>, facts: List<PlaceFactEntity>): List<Place> {
    val byPlace = facts.groupBy { it.placeId }
    return places.map { p ->
        val rows = byPlace[p.id].orEmpty()
        Place(
            id = p.id,
            slug = p.slug,
            name = p.name,
            category = PlaceCategory.fromKey(p.category) ?: PlaceCategory.Lainnya,
            capCode = p.capCode,
            stampName = p.stampName,
            lat = p.lat,
            lng = p.lng,
            address = p.address,
            description = p.description,
            coverKey = p.coverKey,
            regionId = p.regionCode,
            hours = rows.filter { it.kind == KIND_HOURS }
                .mapNotNull { decode(OpeningHoursDto.serializer(), it.json)?.toFact() }
                .sortedBy { it.value.weekday },
            tickets = rows.filter { it.kind == KIND_TICKET }
                .mapNotNull { decode(TicketPriceDto.serializer(), it.json)?.toFact() }
                .sortedBy { it.value.label },
            closures = rows.filter { it.kind == KIND_CLOSURE }
                .mapNotNull { decode(ClosureDto.serializer(), it.json)?.toFact() }
                .sortedBy { it.value.startsOn },
        )
    }
}

private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, raw: String): T? =
    try {
        CacheJson.decodeFromString(serializer, raw)
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

// ---------- events ----------

internal fun EventDto.toEntity() = EventEntity(id, CacheJson.encodeToString(EventDto.serializer(), this))

internal fun ClosureDto.toEventClosureEntity(index: Int) = EventClosureEntity(
    rowKey = "$placeId:$startsOn:$index",
    placeId = placeId,
    json = CacheJson.encodeToString(ClosureDto.serializer(), this),
)

/**
 * Recurrence mapping: `once` needs `event_date`, `weekly` needs an ISO weekday 1..7 (1 = Senin), `daily` needs
 * neither. A row that breaks its own recurrence, or has an unreadable start time, is dropped (null).
 */
internal fun EventDto.toEvent(): Event? {
    val recurrenceValue: Recurrence = when (recurrence) {
        "once" -> parseDate(eventDate)?.let { Recurrence.Once(it) }
        "weekly" -> weekdayOf(weekday)?.let { Recurrence.Weekly(it) }
        "daily" -> Recurrence.Daily
        else -> null
    } ?: return null
    val start = parseTime(startTime) ?: return null
    val checked = parseCheckedAt(checkedAt)
    return Event(
        id = id,
        placeId = placeId,
        placeName = places?.name.orEmpty(),
        title = title,
        description = description.orEmpty(),
        category = PlaceCategory.fromKey(category) ?: PlaceCategory.Lainnya,
        schedule = OfficialFact(
            EventSchedule(recurrenceValue, start, parseTime(endTime)),
            sourceUrl,
            checked,
        ),
        ticket = OfficialFact(EventTicket(priceFromIdr, sellerUrl, onSiteAvailable), sourceUrl, checked),
        performers = performers,
        arrivalAdviceMin = arrivalAdviceMin,
        arrivalAdviceMax = arrivalAdviceMax,
        tags = tags.mapNotNull(EventTag::fromKey).toSet(),
    )
}

internal fun EventEntity.toEvent(): Event? = decode(EventDto.serializer(), json)?.toEvent()

/** Region code of the host place, embedded when the events were fetched. Null when unknown. */
internal fun EventEntity.hostRegionCode(): String? = decode(EventDto.serializer(), json)?.places?.regions?.code

/** Closures per place that count as a cancelled day. A closure without a check date is not trusted, so it is ignored. */
internal fun toCancelledDays(rows: List<EventClosureEntity>): Map<String, List<Closure>> =
    rows.mapNotNull { row ->
        val dto = decode(ClosureDto.serializer(), row.json) ?: return@mapNotNull null
        val fact = dto.toFact() ?: return@mapNotNull null
        if (fact.checkedAt == null) null else dto.placeId to fact.value
    }.groupBy({ it.first }, { it.second })

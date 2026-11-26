package id.kelana.core.data.plan

import id.kelana.core.model.DayPlan
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.PlanStop
import id.kelana.core.model.Recurrence
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.core.model.SkipReason
import id.kelana.core.model.SkippedPlace
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.Travel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TravelMode
import id.kelana.core.model.TripPlan
import id.kelana.core.model.Vehicle
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/*
 * Private storage shape of the cached plan. Dates and times are ISO strings, enums are stored by name. Mapping back
 * throws on anything unknown (bad enum name, bad date); [DataStorePlanCache] turns that into "no plan".
 * Holds places and times only: no location of the user and no personal data.
 */

internal const val PLAN_CACHE_VERSION = 1

@Serializable
internal data class PlanDocumentDto(val version: Int, val trip: TripPlanDto)

@Serializable
internal data class RemindersDocumentDto(val version: Int, val reminders: Map<String, ReminderDto>)

@Serializable
internal data class ReminderDto(val timing: String, val addToCalendar: Boolean)

@Serializable
internal data class FactDto<T>(val value: T, val sourceUrl: String? = null, val checkedAt: String? = null)

@Serializable
internal data class HoursDto(
    val weekday: String,
    val isClosed: Boolean,
    val opens: String? = null,
    val closes: String? = null,
    val note: String? = null,
)

@Serializable
internal data class ScheduleDto(
    /** "once", "weekly" or "daily". */
    val kind: String,
    val date: String? = null,
    val weekday: String? = null,
    val start: String,
    val end: String? = null,
)

@Serializable
internal data class TravelDto(val mode: String, val minutes: Int)

@Serializable
internal data class StopDto(
    val placeId: String,
    val placeName: String,
    val capCode: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val start: String,
    val end: String,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val hours: FactDto<HoursDto>? = null,
    val schedule: FactDto<ScheduleDto>? = null,
    /** The fact itself is optional; inside it the price may be null ("not known"). */
    val price: FactDto<Long?>? = null,
    val travel: TravelDto? = null,
    val arrivalAdviceMin: Int? = null,
    val arrivalAdviceMax: Int? = null,
    val accessUnverified: Boolean = false,
)

@Serializable
internal data class SkippedDto(val placeId: String, val placeName: String, val reason: String, val detail: String? = null)

@Serializable
internal data class DayDto(
    val date: String,
    val stops: List<StopDto>,
    val skipped: List<SkippedDto> = emptyList(),
    val noShowFound: Boolean = false,
)

@Serializable
internal data class ChoicesDto(
    val startDate: String,
    val endDate: String,
    val companion: String,
    val interests: List<String>,
    val windowStart: String,
    val windowEnd: String,
    val pace: String,
    val vehicle: String,
    val needs: List<String>,
)

@Serializable
internal data class TripPlanDto(val choices: ChoicesDto, val days: List<DayDto>, val builtOn: String)

// ---- domain -> dto -------------------------------------------------------------------------------------------

internal fun TripPlan.toDto() = TripPlanDto(choices.toDto(), days.map { it.toDto() }, builtOn.toString())

private fun PlanChoices.toDto() = ChoicesDto(
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    companion = companion.name,
    interests = interests.map { it.name },
    windowStart = windowStart.toString(),
    windowEnd = windowEnd.toString(),
    pace = pace.name,
    vehicle = vehicle.name,
    needs = needs.map { it.name },
)

private fun DayPlan.toDto() = DayDto(
    date = date.toString(),
    stops = stops.map { it.toDto() },
    skipped = skipped.map { SkippedDto(it.placeId, it.placeName, it.reason.name, it.detail) },
    noShowFound = noShowFound,
)

private fun <T, R> OfficialFact<T>.toDto(map: (T) -> R) = FactDto(map(value), sourceUrl, checkedAt?.toString())

private fun PlanStop.toDto() = StopDto(
    placeId = placeId,
    placeName = placeName,
    capCode = capCode,
    category = category.name,
    lat = lat,
    lng = lng,
    start = start.toString(),
    end = end.toString(),
    eventId = eventId,
    eventTitle = eventTitle,
    hours = hours?.toDto { h ->
        HoursDto(h.weekday.name, h.isClosed, h.opens?.toString(), h.closes?.toString(), h.note)
    },
    schedule = schedule?.toDto { s -> s.toDto() },
    price = price?.toDto { it },
    travel = travel?.let { TravelDto(it.mode.name, it.minutes) },
    arrivalAdviceMin = arrivalAdviceMin,
    arrivalAdviceMax = arrivalAdviceMax,
    accessUnverified = accessUnverified,
)

private fun EventSchedule.toDto(): ScheduleDto = when (val r = recurrence) {
    is Recurrence.Once -> ScheduleDto("once", date = r.date.toString(), start = start.toString(), end = end?.toString())
    is Recurrence.Weekly -> ScheduleDto("weekly", weekday = r.weekday.name, start = start.toString(), end = end?.toString())
    Recurrence.Daily -> ScheduleDto("daily", start = start.toString(), end = end?.toString())
}

// ---- dto -> domain (throws on unknown values) ----------------------------------------------------------------

internal fun TripPlanDto.toModel() = TripPlan(choices.toModel(), days.map { it.toModel() }, LocalDate.parse(builtOn))

private fun ChoicesDto.toModel() = PlanChoices(
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    companion = TravelGroup.valueOf(companion),
    interests = interests.map { Interest.valueOf(it) }.toSet(),
    windowStart = LocalTime.parse(windowStart),
    windowEnd = LocalTime.parse(windowEnd),
    pace = Pace.valueOf(pace),
    vehicle = Vehicle.valueOf(vehicle),
    needs = needs.map { SpecialNeed.valueOf(it) }.toSet(),
)

private fun DayDto.toModel() = DayPlan(
    date = LocalDate.parse(date),
    stops = stops.map { it.toModel() },
    skipped = skipped.map { SkippedPlace(it.placeId, it.placeName, SkipReason.valueOf(it.reason), it.detail) },
    noShowFound = noShowFound,
)

private fun <T, R> FactDto<T>.toModel(map: (T) -> R) = OfficialFact(map(value), sourceUrl, checkedAt?.let(LocalDate::parse))

private fun StopDto.toModel() = PlanStop(
    placeId = placeId,
    placeName = placeName,
    capCode = capCode,
    category = PlaceCategory.valueOf(category),
    lat = lat,
    lng = lng,
    start = LocalTime.parse(start),
    end = LocalTime.parse(end),
    eventId = eventId,
    eventTitle = eventTitle,
    hours = hours?.toModel { h ->
        OpeningHours(DayOfWeek.valueOf(h.weekday), h.isClosed, h.opens?.let(LocalTime::parse), h.closes?.let(LocalTime::parse), h.note)
    },
    schedule = schedule?.toModel { it.toModel() },
    price = price?.toModel { it },
    travel = travel?.let { Travel(TravelMode.valueOf(it.mode), it.minutes) },
    arrivalAdviceMin = arrivalAdviceMin,
    arrivalAdviceMax = arrivalAdviceMax,
    accessUnverified = accessUnverified,
)

private fun ScheduleDto.toModel(): EventSchedule {
    val recurrence = when (kind) {
        "once" -> Recurrence.Once(LocalDate.parse(requireNotNull(date)))
        "weekly" -> Recurrence.Weekly(DayOfWeek.valueOf(requireNotNull(weekday)))
        "daily" -> Recurrence.Daily
        else -> error("unknown recurrence $kind")
    }
    return EventSchedule(recurrence, LocalTime.parse(start), end?.let(LocalTime::parse))
}

internal fun Map<String, ReminderChoice>.toDto() =
    RemindersDocumentDto(PLAN_CACHE_VERSION, mapValues { ReminderDto(it.value.timing.name, it.value.addToCalendar) })

/** Entries with an unknown timing are dropped one by one. */
internal fun RemindersDocumentDto.toModel(): Map<String, ReminderChoice> = buildMap {
    for ((id, r) in reminders) {
        val timing = runCatching { ReminderTiming.valueOf(r.timing) }.getOrNull() ?: continue
        put(id, ReminderChoice(timing, r.addToCalendar))
    }
}

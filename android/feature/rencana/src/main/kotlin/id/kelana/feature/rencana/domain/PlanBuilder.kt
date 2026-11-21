package id.kelana.feature.rencana.domain

import id.kelana.core.model.Closure
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.DayPlan
import id.kelana.core.model.Event
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.PlanStop
import id.kelana.core.model.SkipReason
import id.kelana.core.model.SkippedPlace
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.Travel
import id.kelana.core.model.TravelMode
import id.kelana.core.model.TripPlan
import id.kelana.core.model.Vehicle
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Opening state of a place on one date, read only from official facts that carry a check date. */
sealed interface Availability {
    /** Open from [opens] to [closes] according to [hours]. */
    data class Open(val hours: OfficialFact<OpeningHours>, val opens: LocalTime, val closes: LocalTime) : Availability

    /** The weekly hours say closed on this weekday. */
    data object ClosedThatDay : Availability

    /** A published closure period covers the date. */
    data class TemporarilyClosed(val closure: OfficialFact<Closure>) : Availability

    /** No checked hours for this weekday (or hours that cannot be used): the place is never planned. */
    data object Unknown : Availability
}

/** A stop of an existing plan that no longer fits current official facts ([availability] is not [Availability.Open] or its hours moved). */
data class ClosedStop(val stop: PlanStop, val availability: Availability)

/** Result of a live re-plan: the new [plan] and the stop that replaces the closed one, if any ([replacement]). */
data class ReplanResult(val plan: DayPlan, val replacement: PlanStop?)

/**
 * Pure, deterministic day planner. Same input gives the same plan; no clock, no network, no location of the user.
 *
 * ## Rules (in order)
 * 1. **Facts.** Only official facts with a check date are read. A place without a checked hours fact for the
 *    weekday of the date is never planned ([SkipReason.HoursUnknown]); the same for a show without a checked schedule.
 * 2. **Closed places are skipped.** A checked [Closure] covering the date ([SkipReason.TemporarilyClosed], checked
 *    first) or a weekly `isClosed` day ([SkipReason.ClosedThatDay]).
 * 3. **Interests.** A place is a candidate when its category is selected (see [Interest.categories]). [Interest.Pentas]
 *    selects shows instead: at most one show per day, the earliest one that starts and ends inside the time window.
 * 4. **Time window.** Nothing starts before `windowStart` (or `notBefore` when re-planning) and nothing ends after
 *    `windowEnd`. A visit is planned only if the whole visit lies inside the place's opening hours of that day
 *    ("never outside its hours"). A show uses its own checked start time and end (90 minutes when the end is unknown).
 * 5. **Number of stops.** At most `pace.maxStops` per day, the show included.
 * 6. **Order.** The first place is the one that can start earliest (tie: id). Then nearest neighbour by travel
 *    time (tie: earliest start, then id). If a show is planned, a place is chosen only when the traveller can still
 *    reach the show on time after it; the show is always last.
 * 7. **Durations (PROVISIONAL estimates, not official facts).** Visit 90 min for museum, candi and keraton/other,
 *    60 min for kerajinan, topeng, tari and gamelan. Break between stops: `pace.breakMinutes`, plus 15 when the
 *    companion is a family or "more rests" is chosen.
 * 8. **Travel (PROVISIONAL estimates).** Straight-line distance x 1.3. Walk 4.5 km/h; transit 15 km/h plus 10 min wait
 *    (transit only when the stretch is longer than 1.2 km, otherwise walk); private vehicle 20 km/h plus 5 min. Walk
 *    only: a stretch longer than 2.5 km makes the place (or show) ineligible.
 * 9. **Needs.** There is no accessibility fact in the data, so with "wheelchair" or "avoid stairs" every stop is
 *    flagged [PlanStop.accessUnverified] ("access not confirmed by the operator").
 *
 * Everything not planned that matched the interests is listed in [DayPlan.skipped] with the reason.
 */
class PlanBuilder {

    /** One plan per date of the choices (at most [MAX_DAYS]). */
    fun buildTrip(choices: PlanChoices, places: List<Place>, events: List<Event>, builtOn: LocalDate): TripPlan =
        TripPlan(
            choices = choices,
            days = choices.dates.take(MAX_DAYS).map { build(choices, it, places, events) },
            builtOn = builtOn,
        )

    /** Availability of [place] on [date]. */
    fun availability(place: Place, date: LocalDate): Availability {
        place.closures.firstOrNull { it.checkedAt != null && date in it.value }
            ?.let { return Availability.TemporarilyClosed(it) }
        val hours = place.hours.firstOrNull { it.checkedAt != null && it.value.weekday == date.dayOfWeek }
            ?: return Availability.Unknown
        if (hours.value.isClosed) return Availability.ClosedThatDay
        val opens = hours.value.opens ?: return Availability.Unknown
        val closes = hours.value.closes ?: return Availability.Unknown
        if (!closes.isAfter(opens)) return Availability.Unknown
        return Availability.Open(hours, opens, closes)
    }

    /**
     * Plans one day. [excludedPlaceIds] are never planned (and not reported as skipped). [notBefore] moves the start of
     * the window later (live re-plan). [origin] is where the traveller is coming from (the last stop already done);
     * [includeShow] false keeps shows out (a show of the day was already part of the plan).
     */
    fun build(
        choices: PlanChoices,
        date: LocalDate,
        places: List<Place>,
        events: List<Event>,
        excludedPlaceIds: Set<String> = emptySet(),
        notBefore: LocalTime? = null,
        origin: PlanStop? = null,
        includeShow: Boolean = true,
    ): DayPlan {
        val byId = places.associateBy { it.id }
        val windowStart = maxOf(choices.windowStart.minutes(), notBefore?.minutes() ?: 0)
        val windowEnd = choices.windowEnd.minutes()
        val breakMin = choices.pace.breakMinutes +
            if (choices.companion == TravelGroup.Family || SpecialNeed.MoreRests in choices.needs) EXTRA_REST_MINUTES else 0
        val accessFlag = SpecialNeed.Wheelchair in choices.needs || SpecialNeed.AvoidStairs in choices.needs
        val wantsShows = Interest.Pentas in choices.interests && includeShow

        val show = if (wantsShows) pickShow(date, events, byId, windowStart, windowEnd) else null
        val maxPlaces = choices.pace.maxStops - if (show != null) 1 else 0

        val wanted = choices.interests.flatMap { it.categories }.toSet()
        val skipped = mutableListOf<SkippedPlace>()
        val open = mutableListOf<Pair<Place, Availability.Open>>()
        places.filter { it.category in wanted && it.id !in excludedPlaceIds }.sortedBy { it.id }.forEach { place ->
            when (val av = availability(place, date)) {
                is Availability.Open -> open += place to av
                Availability.ClosedThatDay -> skipped += SkippedPlace(place.id, place.name, SkipReason.ClosedThatDay)
                is Availability.TemporarilyClosed ->
                    skipped += SkippedPlace(place.id, place.name, SkipReason.TemporarilyClosed, av.closure.value.reason)
                Availability.Unknown -> skipped += SkippedPlace(place.id, place.name, SkipReason.HoursUnknown)
            }
        }

        val stops = mutableListOf<PlanStop>()
        val remaining = open.toMutableList()
        var cursor = windowStart
        var here: Point? = origin?.let { Point(it.lat, it.lng) }
        val showPoint = show?.let { Point(it.place.lat, it.place.lng) }

        while (stops.size < maxPlaces) {
            val next = remaining.mapNotNull { (place, av) ->
                val point = Point(place.lat, place.lng)
                val travel = here?.let { travelBetween(it, point, choices.vehicle) }
                if (travel != null && travel.walkTooFar) return@mapNotNull null
                val arrive = if (travel == null) cursor else cursor + breakMin + travel.travel.minutes
                val start = maxOf(arrive, av.opens.minutes())
                val end = start + visitMinutes(place.category)
                val fits = end <= av.closes.minutes() && end <= windowEnd
                val toShow = showPoint?.let { travelBetween(point, it, choices.vehicle) }
                val showOk = show == null ||
                    (toShow != null && !toShow.walkTooFar && end + breakMin + toShow.travel.minutes <= show.start)
                if (fits && showOk) Candidate(place, av, travel?.travel, start, end) else null
            }.minWithOrNull(
                compareBy<Candidate>({ it.travel?.minutes ?: 0 }, { it.start }, { it.place.id }),
            ) ?: break
            stops += next.toStop(accessFlag)
            remaining.removeAll { it.first.id == next.place.id }
            cursor = next.end
            here = Point(next.place.lat, next.place.lng)
        }

        var noShow = wantsShows && show == null
        if (show != null) {
            val travel = here?.let { travelBetween(it, showPoint!!, choices.vehicle) }
            if (travel != null && travel.walkTooFar) {
                noShow = true
            } else {
                stops += show.toStop(travel?.travel, accessFlag)
            }
        }

        val planned = stops.filter { !it.isEvent }.map { it.placeId }.toSet()
        remaining.filter { it.first.id !in planned }.forEach { (place, _) ->
            skipped += SkippedPlace(place.id, place.name, SkipReason.NoRoomInDay)
        }
        return DayPlan(date = date, stops = stops, skipped = skipped, noShowFound = noShow)
    }

    /** Stops of [plan] whose place is now closed on the plan date, or whose planned visit no longer fits its hours. */
    fun closedStops(plan: DayPlan, places: List<Place>): List<ClosedStop> {
        val byId = places.associateBy { it.id }
        return plan.stops.filter { !it.isEvent }.mapNotNull { stop ->
            val place = byId[stop.placeId] ?: return@mapNotNull null
            when (val av = availability(place, plan.date)) {
                is Availability.Open ->
                    if (stop.start.minutes() >= av.opens.minutes() && stop.end.minutes() <= av.closes.minutes()) null
                    else ClosedStop(stop, av)
                else -> ClosedStop(stop, av)
            }
        }
    }

    /**
     * Live re-plan after [closedPlaceId] turned out closed. Stops that ended before [now] are kept as they were; the
     * rest of the day is rebuilt without the closed place (and without places already done), starting at [now] from the
     * last finished stop. [ReplanResult.replacement] is the first new place that was not in [current].
     */
    fun replan(
        current: DayPlan,
        choices: PlanChoices,
        closedPlaceId: String,
        places: List<Place>,
        events: List<Event>,
        now: LocalTime? = null,
    ): ReplanResult {
        val done = if (now == null) emptyList() else
            current.stops.filter { it.end.minutes() <= now.minutes() && it.placeId != closedPlaceId }
        val excluded = (done.filter { !it.isEvent }.map { it.placeId } + closedPlaceId).toSet()
        val from = listOfNotNull(now, done.lastOrNull()?.end).maxByOrNull { it.minutes() }
        val rebuilt = build(
            choices = choices,
            date = current.date,
            places = places,
            events = events,
            excludedPlaceIds = excluded,
            notBefore = from,
            origin = done.lastOrNull(),
            includeShow = done.none { it.isEvent },
        )
        val oldIds = current.stops.map { it.placeId }.toSet()
        val replacement = rebuilt.stops.firstOrNull { !it.isEvent && it.placeId !in oldIds }
        return ReplanResult(
            plan = rebuilt.copy(stops = done + rebuilt.stops, skipped = rebuilt.skipped),
            replacement = replacement,
        )
    }

    /** Takes [placeId] out of [plan] and re-estimates the travel into the stop that followed it. Times do not move. */
    fun removeStop(plan: DayPlan, placeId: String, vehicle: Vehicle): DayPlan {
        val result = mutableListOf<PlanStop>()
        var removedJustBefore = false
        for (stop in plan.stops) {
            if (!stop.isEvent && stop.placeId == placeId) {
                removedJustBefore = true
                continue
            }
            val fixed = if (removedJustBefore) {
                val prev = result.lastOrNull()
                stop.copy(travel = prev?.let { travelBetween(Point(it.lat, it.lng), Point(stop.lat, stop.lng), vehicle).travel })
            } else {
                stop
            }
            result += fixed
            removedJustBefore = false
        }
        return plan.copy(stops = result)
    }

    // ---- internals ----

    private class Point(val lat: Double, val lng: Double)

    private class Hop(val travel: Travel, val walkTooFar: Boolean)

    private class Candidate(
        val place: Place,
        val availability: Availability.Open,
        val travel: Travel?,
        val start: Int,
        val end: Int,
    ) {
        fun toStop(accessFlag: Boolean) = PlanStop(
            placeId = place.id,
            placeName = place.name,
            capCode = place.capCode,
            category = place.category,
            lat = place.lat,
            lng = place.lng,
            start = start.toTime(),
            end = end.toTime(),
            hours = availability.hours,
            price = place.tickets.firstOrNull()?.let { OfficialFact(it.value.priceIdr, it.sourceUrl, it.checkedAt) },
            travel = travel,
            accessUnverified = accessFlag,
        )
    }

    private class Show(val event: Event, val place: Place, val start: Int, val end: Int) {
        fun toStop(travel: Travel?, accessFlag: Boolean) = PlanStop(
            placeId = place.id,
            placeName = place.name,
            capCode = place.capCode,
            category = event.category,
            lat = place.lat,
            lng = place.lng,
            start = start.toTime(),
            end = end.toTime(),
            eventId = event.id,
            eventTitle = event.title,
            schedule = event.schedule,
            price = OfficialFact(event.ticket.value.priceFromIdr, event.ticket.sourceUrl, event.ticket.checkedAt),
            travel = travel,
            arrivalAdviceMin = event.arrivalAdviceMin,
            arrivalAdviceMax = event.arrivalAdviceMax,
            accessUnverified = accessFlag,
        )
    }

    private fun pickShow(
        date: LocalDate,
        events: List<Event>,
        byId: Map<String, Place>,
        windowStart: Int,
        windowEnd: Int,
    ): Show? = events
        .filter { it.schedule.checkedAt != null && it.occursOn(date) }
        .mapNotNull { event ->
            val place = byId[event.placeId] ?: return@mapNotNull null
            val start = event.schedule.value.start.minutes()
            val end = event.schedule.value.end?.minutes() ?: (start + UNKNOWN_SHOW_MINUTES)
            if (end > start && start >= windowStart && end <= windowEnd) Show(event, place, start, end) else null
        }
        .minWithOrNull(compareBy<Show>({ it.start }, { it.event.id }))

    private fun travelBetween(from: Point, to: Point, vehicle: Vehicle): Hop {
        val km = haversineKm(from, to) * ROAD_FACTOR
        if (km < 0.05) return Hop(Travel(TravelMode.Walk, 0), false)
        val mode = when (vehicle) {
            Vehicle.WalkOnly -> TravelMode.Walk
            Vehicle.PrivateVehicle -> TravelMode.Car
            Vehicle.TransitAndWalk -> if (km <= TRANSIT_MIN_KM) TravelMode.Walk else TravelMode.Transit
        }
        val minutes = when (mode) {
            TravelMode.Walk -> km / WALK_KMH * 60
            TravelMode.Transit -> km / TRANSIT_KMH * 60 + TRANSIT_WAIT_MIN
            TravelMode.Car -> km / CAR_KMH * 60 + CAR_PARKING_MIN
        }
        return Hop(Travel(mode, ceil(minutes).toInt().coerceAtLeast(1)), vehicle == Vehicle.WalkOnly && km > WALK_ONLY_MAX_KM)
    }

    private fun haversineKm(a: Point, b: Point): Double {
        val dLat = (b.lat - a.lat).rad()
        val dLng = (b.lng - a.lng).rad()
        val h = sin(dLat / 2).let { it * it } + cos(a.lat.rad()) * cos(b.lat.rad()) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_KM * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun Double.rad() = this * PI / 180

    private fun visitMinutes(category: PlaceCategory): Int = when (category) {
        PlaceCategory.Museum, PlaceCategory.Candi, PlaceCategory.Lainnya -> 90
        PlaceCategory.Kerajinan, PlaceCategory.Topeng, PlaceCategory.Tari, PlaceCategory.Gamelan -> 60
    }

    companion object {
        /** A trip covers at most this many days; later dates are ignored. */
        const val MAX_DAYS = 7
        private const val EXTRA_REST_MINUTES = 15
        private const val UNKNOWN_SHOW_MINUTES = 90
        private const val ROAD_FACTOR = 1.3
        private const val WALK_KMH = 4.5
        private const val TRANSIT_KMH = 15.0
        private const val TRANSIT_WAIT_MIN = 10
        private const val CAR_KMH = 20.0
        private const val CAR_PARKING_MIN = 5
        private const val TRANSIT_MIN_KM = 1.2
        private const val WALK_ONLY_MAX_KM = 2.5
        private const val EARTH_KM = 6371.0
    }
}

internal fun LocalTime.minutes(): Int = hour * 60 + minute

internal fun Int.toTime(): LocalTime = LocalTime.of(this / 60, this % 60)

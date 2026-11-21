package id.kelana.feature.rencana.data

import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.Event
import id.kelana.core.model.EventRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.PlanCache
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.PlanStop
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.domain.BuildResult
import id.kelana.feature.rencana.domain.ClosedStop
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.PlanRepository
import id.kelana.feature.rencana.domain.ReplanOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROVISIONAL "fake" planner: runs [PlanBuilder] on the device over the place and event repositories, with no server
 * planner yet. Building and re-planning answer `Offline` without signal; reading the cached plan never needs signal.
 * [clock] decides "today" and "now" (a re-plan on today keeps the stops that already ended).
 */
@Singleton
class LocalPlanRepository(
    private val places: PlaceRepository,
    private val events: EventRepository,
    private val cache: PlanCache,
    private val connectivity: ConnectivityMonitor,
    private val builder: PlanBuilder,
    private val clock: Clock,
) : PlanRepository {

    @Inject
    constructor(
        places: PlaceRepository,
        events: EventRepository,
        cache: PlanCache,
        connectivity: ConnectivityMonitor,
    ) : this(places, events, cache, connectivity, PlanBuilder(), Clock.system(ZoneId.of("Asia/Jakarta")))

    override val trip: Flow<TripPlan?> = cache.trip
    override val reminders: Flow<Map<String, ReminderChoice>> = cache.reminders

    override suspend fun build(choices: PlanChoices): BuildResult {
        if (!connectivity.isOnlineNow()) return BuildResult.Offline
        val allPlaces = places.places().first()
        val allEvents = eventsFor(choices.startDate, choices.endDate)
        val trip = builder.buildTrip(choices, allPlaces, allEvents, builtOn = LocalDate.now(clock))
        if (trip.days.all { it.stops.isEmpty() }) return BuildResult.NothingFits
        cache.save(trip)
        return BuildResult.Built(trip)
    }

    override suspend fun closedStops(date: LocalDate): List<ClosedStop> {
        val day = cache.trip.first()?.days?.firstOrNull { it.date == date } ?: return emptyList()
        return builder.closedStops(day, places.places().first())
    }

    override suspend fun previewReplacement(date: LocalDate, closedPlaceId: String): PlanStop? {
        val trip = cache.trip.first() ?: return null
        val day = trip.days.firstOrNull { it.date == date } ?: return null
        return builder.replan(
            current = day,
            choices = trip.choices,
            closedPlaceId = closedPlaceId,
            places = places.places().first(),
            events = eventsFor(date, date),
            now = nowFor(date),
        ).replacement
    }

    override suspend fun applyReplan(date: LocalDate, closedPlaceId: String, replace: Boolean): ReplanOutcome {
        if (!connectivity.isOnlineNow()) return ReplanOutcome.Offline
        val trip = cache.trip.first() ?: return ReplanOutcome.NotFound
        val day = trip.days.firstOrNull { it.date == date } ?: return ReplanOutcome.NotFound
        if (day.stops.none { it.placeId == closedPlaceId && !it.isEvent }) return ReplanOutcome.NotFound
        val (newDay, replacement) = if (replace) {
            val result = builder.replan(
                current = day,
                choices = trip.choices,
                closedPlaceId = closedPlaceId,
                places = places.places().first(),
                events = eventsFor(date, date),
                now = nowFor(date),
            )
            result.plan to result.replacement
        } else {
            builder.removeStop(day, closedPlaceId, trip.choices.vehicle) to null
        }
        val updated = trip.copy(days = trip.days.map { if (it.date == date) newDay else it })
        cache.save(updated)
        return ReplanOutcome.Applied(updated, replacement)
    }

    override suspend fun saveReminder(eventId: String, choice: ReminderChoice) = cache.saveReminder(eventId, choice)

    private suspend fun eventsFor(from: LocalDate, to: LocalDate): List<Event> =
        events.eventsBetween(from, to).first().map { it.event }.distinctBy { it.id }

    private fun nowFor(date: LocalDate): LocalTime? = if (date == LocalDate.now(clock)) LocalTime.now(clock) else null
}

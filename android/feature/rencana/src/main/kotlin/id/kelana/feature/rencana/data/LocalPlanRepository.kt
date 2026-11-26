package id.kelana.feature.rencana.data

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROVISIONAL "fake" planner: runs [PlanBuilder] on the device over the place and event repositories, with no server
 * planner yet. Building and re-planning work offline from the on-device cache of places and events (no signal needed).
 * [clock] decides "today" and "now" (a re-plan on today keeps the stops that already ended).
 */
@Singleton
class LocalPlanRepository(
    private val places: PlaceRepository,
    private val events: EventRepository,
    private val cache: PlanCache,
    private val builder: PlanBuilder,
    private val clock: Clock,
) : PlanRepository {

    @Inject
    constructor(
        places: PlaceRepository,
        events: EventRepository,
        cache: PlanCache,
    ) : this(places, events, cache, PlanBuilder(), Clock.system(ZoneId.of("Asia/Jakarta")))

    override val trip: Flow<TripPlan?> = cache.trip
    override val reminders: Flow<Map<String, ReminderChoice>> = cache.reminders

    override suspend fun build(choices: PlanChoices): BuildResult {
        return try {
            // The place repository emits its cache first: an empty first emission means nothing is cached yet, so
            // give a refresh a moment to arrive before answering NoData.
            val allPlaces = withTimeoutOrNull(PLACES_WAIT_MS) { places.places().firstOrNull { it.isNotEmpty() } }
                ?: return BuildResult.NoData
            val allEvents = eventsFor(choices.startDate, choices.endDate)
            val trip = builder.buildTrip(choices, allPlaces, allEvents, builtOn = LocalDate.now(clock))
            if (trip.days.all { it.stops.isEmpty() }) return BuildResult.NothingFits
            cache.save(trip)
            BuildResult.Built(trip)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            BuildResult.Failed
        }
    }

    override suspend fun closedStops(date: LocalDate): List<ClosedStop> {
        val day = cache.trip.first()?.days?.firstOrNull { it.date == date } ?: return emptyList()
        return builder.closedStops(day, places.places().first())
    }

    override suspend fun previewReplacement(date: LocalDate, closedPlaceId: String): PlanStop? {
        val trip = cache.trip.first() ?: return null
        val day = trip.days.firstOrNull { it.date == date } ?: return null
        val otherDays = stopIdsOfOtherDays(trip, date)
        return builder.replan(
            current = day,
            choices = trip.choices,
            closedPlaceId = closedPlaceId,
            places = places.places().first(),
            events = eventsFor(date, date),
            now = nowFor(date),
            excludedPlaceIds = otherDays.first,
            excludedEventIds = otherDays.second,
        ).replacement
    }

    override suspend fun applyReplan(date: LocalDate, closedPlaceId: String, replace: Boolean): ReplanOutcome {
        return try {
            replan(date, closedPlaceId, replace)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ReplanOutcome.Failed
        }
    }

    private suspend fun replan(date: LocalDate, closedPlaceId: String, replace: Boolean): ReplanOutcome {
        val trip = cache.trip.first() ?: return ReplanOutcome.NotFound
        val day = trip.days.firstOrNull { it.date == date } ?: return ReplanOutcome.NotFound
        if (day.stops.none { it.placeId == closedPlaceId && !it.isEvent }) return ReplanOutcome.NotFound
        val (newDay, replacement) = if (replace) {
            val otherDays = stopIdsOfOtherDays(trip, date)
            val result = builder.replan(
                current = day,
                choices = trip.choices,
                closedPlaceId = closedPlaceId,
                places = places.places().first(),
                events = eventsFor(date, date),
                now = nowFor(date),
                excludedPlaceIds = otherDays.first,
                excludedEventIds = otherDays.second,
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

    /** Place ids and event ids planned on the other days of [trip]: a re-plan must not repeat them. */
    private fun stopIdsOfOtherDays(trip: TripPlan, date: LocalDate): Pair<Set<String>, Set<String>> {
        val stops = trip.days.filter { it.date != date }.flatMap { it.stops }
        return stops.filter { !it.isEvent }.map { it.placeId }.toSet() to stops.mapNotNull { it.eventId }.toSet()
    }

    private companion object {
        /** How long [build] waits for the first non-empty place list before answering `NoData`. */
        const val PLACES_WAIT_MS = 10_000L
    }

    private fun nowFor(date: LocalDate): LocalTime? = if (date == LocalDate.now(clock)) LocalTime.now(clock) else null
}

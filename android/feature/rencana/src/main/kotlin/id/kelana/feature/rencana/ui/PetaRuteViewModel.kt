package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlanStop
import id.kelana.core.model.RouteLeg
import id.kelana.core.model.RoutePoint
import id.kelana.core.model.RouteRepository
import id.kelana.core.model.TravelMode
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class PetaRuteUiState(
    val isLoading: Boolean = true,
    /** Stops of the shown day in order; empty when there is no plan. */
    val stops: List<PlanStop> = emptyList(),
    /** The stop to go to next ("BERIKUTNYA"): the one going on now, else the first that has not started. */
    val next: PlanStop? = null,
    /**
     * The line to draw: first the straight line through the stops, then (when the route server answers) the road
     * route. Empty with fewer than two stops.
     */
    val routePath: List<RoutePoint> = emptyList(),
)

/** Route view of the plan of today (or the next planned day). Reads the cached plan, so it works offline. */
@HiltViewModel
class PetaRuteViewModel(
    plans: PlanRepository,
    private val routes: RouteRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(plans: PlanRepository, routes: RouteRepository) : this(plans, routes, Clock.system(ZoneId.of("Asia/Jakarta")))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PetaRuteUiState> = plans.trip.map { trip ->
        val today = LocalDate.now(clock)
        val now = LocalTime.now(clock)
        val day = trip?.days?.firstOrNull { it.date == today } ?: trip?.days?.firstOrNull { it.date.isAfter(today) }
        val stops = day?.stops.orEmpty()
        val next = if (day?.date == today) stops.firstOrNull { it.end > now } else stops.firstOrNull()
        PetaRuteUiState(isLoading = false, stops = stops, next = next)
    }.transformLatest { state ->
        val straight = state.stops.map { RoutePoint(it.lat, it.lng) }
        emit(state.copy(routePath = if (straight.size >= 2) straight else emptyList()))
        if (straight.size >= 2) {
            val road = roadPath(state.stops)
            if (road != null) emit(state.copy(routePath = road))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetaRuteUiState())

    /** The road path through [stops], or null when no leg could be routed (the straight line stays). */
    private suspend fun roadPath(stops: List<PlanStop>): List<RoutePoint>? {
        val points = stops.map { RoutePoint(it.lat, it.lng) }
        var anyRouted = false
        val legs = ArrayList<RouteLeg>(points.size - 1)
        for (group in legGroups(stops)) {
            val groupPoints = points.subList(group.first, group.last + 1)
            val routed = routes.route(group.mode, groupPoints)
            if (routed != null) anyRouted = true
            legs += routed ?: groupPoints.zipWithNext { a, b -> RouteLeg(0, 0, listOf(a, b)) }
        }
        return if (anyRouted) joinLegs(legs) else null
    }
}

/** Consecutive legs travelled the same way: [first]..[last] are indexes into the stops (inclusive). */
internal data class LegGroup(val mode: TravelMode, val first: Int, val last: Int)

/**
 * Mode of the leg that arrives at each stop (the plan stores travel on the arriving stop); a missing mode counts as
 * a car. Consecutive legs with the same mode form one group, so one request covers them.
 */
internal fun legGroups(stops: List<PlanStop>): List<LegGroup> {
    val groups = ArrayList<LegGroup>()
    for (i in 1 until stops.size) {
        val mode = stops[i].travel?.mode ?: TravelMode.Car
        val last = groups.lastOrNull()
        if (last != null && last.mode == mode) groups[groups.lastIndex] = last.copy(last = i) else groups += LegGroup(mode, i - 1, i)
    }
    return groups
}

/** Joins leg paths into one line; the first point of each next leg repeats the previous end and is dropped. */
internal fun joinLegs(legs: List<RouteLeg>): List<RoutePoint> {
    val out = ArrayList<RoutePoint>()
    legs.forEachIndexed { i, leg -> out += if (i == 0) leg.path else leg.path.drop(1) }
    return out
}

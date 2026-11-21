package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.DayPlan
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.ReminderChoice
import id.kelana.feature.rencana.domain.ClosedStop
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class RencanaUiState(
    val isLoading: Boolean = true,
    val today: LocalDate,
    val now: LocalTime,
    val isOffline: Boolean = false,
    /** The plan shown: today's day, or the next planned day when nothing is planned for today. Null means empty state. */
    val plan: DayPlan? = null,
    val companion: TravelGroup? = null,
    /** Stops that current official facts say are closed. Always empty offline (re-planning needs the server). */
    val closedStops: List<ClosedStop> = emptyList(),
    val reminders: Map<String, ReminderChoice> = emptyMap(),
    /** First show of today that starts in the evening, for the empty state card. */
    val tonight: EventOccurrence? = null,
) {
    val isToday: Boolean get() = plan?.date == today

    /** Key of the stop that is going on right now (only for today's plan). */
    val currentKey: String?
        get() = if (!isToday) null else plan?.stops?.firstOrNull { now >= it.start && now < it.end }?.key
}

/**
 * Rencana tab. "Today" and "now" come from [clock] (read when the state is built, not ticking). The cached plan is
 * read without signal; the closed-place check reads cached official facts and is skipped offline.
 */
@HiltViewModel
class RencanaViewModel(
    private val plans: PlanRepository,
    private val events: EventRepository,
    private val connectivity: ConnectivityMonitor,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(plans: PlanRepository, events: EventRepository, connectivity: ConnectivityMonitor) :
        this(plans, events, connectivity, Clock.system(ZoneId.of("Asia/Jakarta")))

    val uiState: StateFlow<RencanaUiState> = combine(
        plans.trip,
        connectivity.isOnline,
        plans.reminders,
        tonightFlow(),
    ) { trip, online, reminders, tonight ->
        val today = LocalDate.now(clock)
        val day = trip?.days?.firstOrNull { it.date == today } ?: trip?.days?.firstOrNull { it.date.isAfter(today) }
        RencanaUiState(
            isLoading = false,
            today = today,
            now = LocalTime.now(clock),
            isOffline = !online,
            plan = day,
            companion = trip?.choices?.companion,
            closedStops = if (day != null && online) plans.closedStops(day.date) else emptyList(),
            reminders = reminders,
            tonight = tonight,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RencanaUiState(today = LocalDate.now(clock), now = LocalTime.now(clock)),
    )

    private fun tonightFlow(): Flow<EventOccurrence?> {
        val today = LocalDate.now(clock)
        return events.eventsOn(today).map { list ->
            list.firstOrNull { it.event.schedule.checkedAt != null && it.event.schedule.value.start >= EveningStart }
        }
    }

    private companion object {
        val EveningStart: LocalTime = LocalTime.of(17, 0)
    }
}

package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.DayPlan
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.PlanBackpackRepository
import id.kelana.core.model.ReminderChoice
import id.kelana.feature.rencana.domain.ClosedStop
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** One day chip of the day picker: the date and how many stops that day has (0 = empty day). */
@Immutable
data class DayOption(val date: LocalDate, val stopCount: Int)

@Immutable
data class RencanaUiState(
    val isLoading: Boolean = true,
    val today: LocalDate,
    val now: LocalTime,
    val isOffline: Boolean = false,
    /** The plan shown: today's day, or the next planned day when nothing is planned for today. Null means empty state. */
    val plan: DayPlan? = null,
    val companion: TravelGroup? = null,
    /** Stops that current official facts say are closed (cached facts, so also offline). */
    val closedStops: List<ClosedStop> = emptyList(),
    val reminders: Map<String, ReminderChoice> = emptyMap(),
    /** First show of today that starts in the evening, for the empty state card. */
    val tonight: EventOccurrence? = null,
    /** Places added with "Masuk rencana", oldest first. Shown in the empty state in place of "Ranselmu masih kosong". */
    val backpack: List<Place> = emptyList(),
    /** Loading the plan failed unexpectedly: the tab shows an error with retry. */
    val loadError: Boolean = false,
    /** Every day of the trip, in order. The picker is shown only when there is more than one ([showDayPicker]). */
    val days: List<DayOption> = emptyList(),
    /** True when [plan] is the day the tab opens on (today, the next planned day, or the first day of a finished trip). */
    val isDefaultDay: Boolean = true,
) {
    val showDayPicker: Boolean get() = days.size > 1

    /** 1-based position of [plan] in the trip, or null when it is not part of [days]. */
    val dayNumber: Int? get() = days.indexOfFirst { it.date == plan?.date }.takeIf { it >= 0 }?.plus(1)

    val isToday: Boolean get() = plan?.date == today

    /** Key of the stop that is going on right now (only for today's plan). */
    val currentKey: String?
        get() = if (!isToday) null else plan?.stops?.firstOrNull { now >= it.start && now < it.end }?.key
}

/**
 * Rencana tab. "Today" and "now" come from [clock] (read when the state is built, not ticking). The cached plan and
 * the closed-place check (cached official facts) work without signal; [RencanaUiState.isOffline] only drives a
 * non-blocking status banner. An upstream failure sets [RencanaUiState.loadError]; [retry] subscribes again.
 */
@HiltViewModel
class RencanaViewModel(
    private val plans: PlanRepository,
    private val events: EventRepository,
    private val connectivity: ConnectivityMonitor,
    private val places: PlaceRepository,
    private val backpackStore: PlanBackpackRepository,
    private val clock: Clock,
    /** Keeps the selected day across configuration change and process death. */
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    @Inject
    constructor(
        plans: PlanRepository,
        events: EventRepository,
        connectivity: ConnectivityMonitor,
        places: PlaceRepository,
        backpackStore: PlanBackpackRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(plans, events, connectivity, places, backpackStore, Clock.system(ZoneId.of("Asia/Jakarta")), savedStateHandle)

    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RencanaUiState> = retryTrigger.flatMapLatest {
        combine(
            combine(plans.trip, savedState.getStateFlow<String?>(SelectedDateKey, null)) { trip, selected -> trip to selected },
            connectivity.isOnline,
            plans.reminders,
            tonightFlow(),
            backpackFlow(),
        ) { (trip, selected), online, reminders, tonight, backpack ->
            val today = LocalDate.now(clock)
            val default = defaultDay(trip, today)
            val day = trip?.days?.firstOrNull { it.date.toString() == selected } ?: default
            RencanaUiState(
                isLoading = false,
                today = today,
                now = LocalTime.now(clock),
                isOffline = !online,
                plan = day,
                companion = trip?.choices?.companion,
                closedStops = if (day != null) closedStopsOrEmpty(day.date) else emptyList(),
                reminders = reminders,
                tonight = tonight,
                backpack = backpack,
                days = trip?.days.orEmpty().map { DayOption(it.date, it.stops.size) },
                isDefaultDay = day?.date == default?.date,
            )
        }.catch { e ->
            if (e is CancellationException) throw e
            emit(RencanaUiState(isLoading = false, today = LocalDate.now(clock), now = LocalTime.now(clock), loadError = true))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RencanaUiState(today = LocalDate.now(clock), now = LocalTime.now(clock)),
    )

    /** A chip of the day picker was tapped: the whole tab follows that day. */
    fun selectDay(date: LocalDate) {
        savedState[SelectedDateKey] = date.toString()
    }

    /**
     * The day the tab opens on: today, else the next planned day. When every day of a multi-day trip is past, the first
     * day (so the picker still has something to show); a finished single-day trip stays the empty state.
     */
    private fun defaultDay(trip: id.kelana.core.model.TripPlan?, today: LocalDate): DayPlan? {
        val days = trip?.days.orEmpty()
        return days.firstOrNull { it.date == today }
            ?: days.firstOrNull { it.date.isAfter(today) }
            ?: if (days.size > 1) days.first() else null
    }

    /** "Coba lagi" on the load error: subscribes to the plan again. */
    fun retry() {
        retryTrigger.value += 1
    }

    /** A failing closed-place check is not fatal: the tab just shows no closed prompt. */
    private suspend fun closedStopsOrEmpty(date: LocalDate): List<ClosedStop> = try {
        plans.closedStops(date)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    /** Added places in the order they were added; an id whose place is not known (any more) is skipped. */
    private fun backpackFlow(): Flow<List<Place>> =
        combine(backpackStore.placeIds(), places.places()) { ids, all ->
            val byId = all.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }.catch { e ->
            if (e is CancellationException) throw e
            emit(emptyList())
        }

    fun removeFromBackpack(placeId: String) {
        viewModelScope.launch {
            try {
                backpackStore.removePlace(placeId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Local write failed: the list keeps showing the stored state.
            }
        }
    }

    private fun tonightFlow(): Flow<EventOccurrence?> {
        val today = LocalDate.now(clock)
        return events.eventsOn(today).map { list ->
            list.firstOrNull { it.event.schedule.checkedAt != null && it.event.schedule.value.start >= EveningStart }
        }
    }

    private companion object {
        const val SelectedDateKey = "selected_date"
        val EveningStart: LocalTime = LocalTime.of(17, 0)
    }
}

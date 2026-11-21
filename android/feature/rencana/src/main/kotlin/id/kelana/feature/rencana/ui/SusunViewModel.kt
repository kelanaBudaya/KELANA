package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.Interest
import id.kelana.core.model.Pace
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.Vehicle
import id.kelana.feature.rencana.domain.BuildResult
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** What happened to the last build request. */
enum class BuildOutcome { Built, NothingFits, Offline }

@Immutable
data class SusunUiState(
    val today: LocalDate,
    val visibleMonth: YearMonth,
    val startDate: LocalDate,
    /** Null for a single day. */
    val endDate: LocalDate? = null,
    val companion: TravelGroup? = null,
    val interests: Set<Interest> = emptySet(),
    val windowStart: LocalTime = DefaultStart,
    val windowEnd: LocalTime = DefaultEnd,
    val pace: Pace = Pace.Relaxed,
    val vehicle: Vehicle = Vehicle.TransitAndWalk,
    val needs: Set<SpecialNeed> = emptySet(),
    val isBuilding: Boolean = false,
    /** Index of the checklist item shown as running on the Menyusun screen. */
    val stage: Int = 0,
    val outcome: BuildOutcome? = null,
) {
    val lastDate: LocalDate get() = endDate ?: startDate
    val dayCount: Int get() = (ChronoUnit.DAYS.between(startDate, lastDate) + 1).toInt()

    /** 1/6: always valid once a date is picked (dates before today cannot be picked). */
    val canContinueTanggal: Boolean get() = !startDate.isBefore(today)
    val canContinueTeman: Boolean get() = companion != null
    val canContinueMinat: Boolean get() = interests.isNotEmpty()
    val canContinueWaktu: Boolean get() = minutesBetween(windowStart, windowEnd) >= MinWindowMinutes
    val canSubmit: Boolean get() = canContinueTanggal && canContinueTeman && canContinueMinat && canContinueWaktu && !isBuilding

    val canGoPreviousMonth: Boolean get() = visibleMonth.isAfter(YearMonth.from(today))

    /** Which optional checklist items of the Menyusun screen apply to the answers. */
    val showsShowStage: Boolean get() = Interest.Pentas in interests
    val showsRestStage: Boolean get() = companion == TravelGroup.Family || SpecialNeed.MoreRests in needs
    val showsAccessStage: Boolean get() = SpecialNeed.Wheelchair in needs || SpecialNeed.AvoidStairs in needs

    /** Choices to build with, or null while an answer is missing or invalid. */
    fun toChoices(): PlanChoices? {
        val who = companion ?: return null
        if (!(canContinueTanggal && canContinueMinat && canContinueWaktu)) return null
        return PlanChoices(
            startDate = startDate,
            endDate = lastDate,
            companion = who,
            interests = interests,
            windowStart = windowStart,
            windowEnd = windowEnd,
            pace = pace,
            vehicle = vehicle,
            needs = needs,
        )
    }

    companion object {
        val DefaultStart: LocalTime = LocalTime.of(8, 0)
        val DefaultEnd: LocalTime = LocalTime.of(21, 0)
        const val MinWindowMinutes = 120
        const val StepMinutes = 30
        val EarliestStart: LocalTime = LocalTime.of(5, 0)
        val LatestEnd: LocalTime = LocalTime.of(23, 30)

        private fun minutesBetween(a: LocalTime, b: LocalTime): Int = (b.toSecondOfDay() - a.toSecondOfDay()) / 60
    }
}

/**
 * Holds the six answers of the planner and validates each step. One instance is shared by all wizard screens (scoped
 * to the nested graph). Building runs here; the screens only react to [SusunUiState.outcome].
 *
 * [stageDelayMs] is the pause between the checklist items of the Menyusun screen. PROVISIONAL: the on-device planner
 * answers at once, so the stages are paced for readability only; a server planner would drive them for real.
 */
@HiltViewModel
class SusunViewModel(
    private val plans: PlanRepository,
    private val connectivity: ConnectivityMonitor,
    clock: Clock,
    private val stageDelayMs: Long,
) : ViewModel() {

    @Inject
    constructor(plans: PlanRepository, connectivity: ConnectivityMonitor) :
        this(plans, connectivity, Clock.system(ZoneId.of("Asia/Jakarta")), DEFAULT_STAGE_DELAY_MS)

    private val _state = MutableStateFlow(
        LocalDate.now(clock).let { today ->
            SusunUiState(today = today, visibleMonth = YearMonth.from(today), startDate = today)
        },
    )
    val uiState: StateFlow<SusunUiState> = _state.asStateFlow()

    private var buildJob: Job? = null

    // ---- 1/6 date ----

    /**
     * First tap sets the start, a second later tap sets the end (a range of at most [PlanBuilder.MAX_DAYS] days). A tap
     * on or before the start, or after a complete range, starts a new selection. Past dates and dates too far from the
     * start restart the selection from the tapped date. Past dates are ignored.
     */
    fun pickDate(date: LocalDate) = _state.update { s ->
        when {
            date.isBefore(s.today) -> s
            s.endDate != null || !date.isAfter(s.startDate) -> s.copy(startDate = date, endDate = null)
            ChronoUnit.DAYS.between(s.startDate, date) >= PlanBuilder.MAX_DAYS -> s.copy(startDate = date, endDate = null)
            else -> s.copy(endDate = date)
        }
    }

    fun showNextMonth() = _state.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }

    fun showPreviousMonth() = _state.update { if (it.canGoPreviousMonth) it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) else it }

    // ---- 2/6 companion ----

    fun selectCompanion(companion: TravelGroup) = _state.update { it.copy(companion = companion) }

    // ---- 3/6 interests ----

    fun toggleInterest(interest: Interest) = _state.update {
        it.copy(interests = if (interest in it.interests) it.interests - interest else it.interests + interest)
    }

    // ---- 4/6 time window and pace ----

    /** Moves the start by [steps] x 30 minutes, keeping at least [SusunUiState.MinWindowMinutes] before the end. */
    fun shiftStart(steps: Int) = _state.update { s ->
        val moved = minutesOf(s.windowStart) + steps * SusunUiState.StepMinutes
        val ok = moved >= minutesOf(SusunUiState.EarliestStart) &&
            moved <= minutesOf(s.windowEnd) - SusunUiState.MinWindowMinutes
        if (ok) s.copy(windowStart = timeOf(moved)) else s
    }

    /** Moves the end by [steps] x 30 minutes, keeping at least [SusunUiState.MinWindowMinutes] after the start. */
    fun shiftEnd(steps: Int) = _state.update { s ->
        val moved = minutesOf(s.windowEnd) + steps * SusunUiState.StepMinutes
        val ok = moved <= minutesOf(SusunUiState.LatestEnd) &&
            moved >= minutesOf(s.windowStart) + SusunUiState.MinWindowMinutes
        if (ok) s.copy(windowEnd = timeOf(moved)) else s
    }

    fun selectPace(pace: Pace) = _state.update { it.copy(pace = pace) }

    // ---- 5/6 vehicle ----

    fun selectVehicle(vehicle: Vehicle) = _state.update { it.copy(vehicle = vehicle) }

    // ---- 6/6 needs ----

    fun toggleNeed(need: SpecialNeed) = _state.update {
        it.copy(needs = if (need in it.needs) it.needs - need else it.needs + need)
    }

    /** "Tidak ada" clears every need. */
    fun clearNeeds() = _state.update { it.copy(needs = emptySet()) }

    // ---- build ----

    /** Whether building may start. Without signal the caller shows the offline screen instead of Menyusun. */
    fun canBuildNow(): Boolean = connectivity.isOnlineNow()

    /** Builds the plan from the answers. Safe to call again while a build runs (ignored). */
    fun startBuild() {
        if (buildJob?.isActive == true) return
        val choices = _state.value.toChoices() ?: return
        _state.update { it.copy(isBuilding = true, stage = 0, outcome = null) }
        buildJob = viewModelScope.launch {
            val stages = stageCount(_state.value)
            for (i in 1 until stages) {
                if (stageDelayMs > 0) delay(stageDelayMs)
                _state.update { it.copy(stage = i) }
            }
            val result = plans.build(choices)
            _state.update {
                it.copy(
                    isBuilding = false,
                    stage = stages,
                    outcome = when (result) {
                        is BuildResult.Built -> BuildOutcome.Built
                        BuildResult.NothingFits -> BuildOutcome.NothingFits
                        BuildResult.Offline -> BuildOutcome.Offline
                    },
                )
            }
        }
    }

    /** The screen handled [SusunUiState.outcome]. */
    fun consumeOutcome() = _state.update { it.copy(outcome = null) }

    /** Number of checklist items of the Menyusun screen for the current answers (opening hours and travel time are always there). */
    fun stageCountNow(): Int = stageCount(_state.value)

    private fun stageCount(s: SusunUiState): Int = 2 + (if (s.showsRestStage) 1 else 0) + (if (s.showsAccessStage) 1 else 0)

    private fun minutesOf(t: LocalTime): Int = t.hour * 60 + t.minute

    private fun timeOf(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

    private companion object {
        const val DEFAULT_STAGE_DELAY_MS = 450L
    }
}

package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.ConnectivityMonitor
import id.kelana.core.model.PlanStop
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.PlanRepository
import id.kelana.feature.rencana.domain.ReplanOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** "Mau diapakan?" options of the closed-place screen. */
enum class ClosedResolution { ReplaceWithOther, MoveToOtherDay, Remove }

/** What the screen does next. */
enum class TempatTutupOutcome { Done, GoToWizard, GoOffline }

@Immutable
data class TempatTutupUiState(
    val isLoading: Boolean = true,
    /** The place is not (or no longer) closed in the plan: the screen just leaves. */
    val notFound: Boolean = false,
    val isOffline: Boolean = false,
    val stop: PlanStop? = null,
    val availability: Availability? = null,
    /** What the builder offers instead, or null when nothing else fits. */
    val replacement: PlanStop? = null,
    val selected: ClosedResolution = ClosedResolution.Remove,
    val isApplying: Boolean = false,
    val outcome: TempatTutupOutcome? = null,
)

/**
 * Live re-plan prompt for one closed place of the plan of [date]. Offline, the screen shows the offline state: re-planning
 * needs the server (AGENTS.md). Detection and the preview read cached facts only.
 */
@HiltViewModel
class TempatTutupViewModel(
    private val plans: PlanRepository,
    connectivity: ConnectivityMonitor,
    private val placeId: String,
    private val date: LocalDate?,
) : ViewModel() {

    /** Route arguments are stored in the handle under the field names of `TempatTutupRoute`. */
    @Inject
    constructor(plans: PlanRepository, connectivity: ConnectivityMonitor, savedStateHandle: SavedStateHandle) :
        this(
            plans,
            connectivity,
            savedStateHandle.get<String>("placeId").orEmpty(),
            parseIsoDate(savedStateHandle.get<String>("date")),
        )

    private val local = MutableStateFlow(TempatTutupUiState())

    val uiState: StateFlow<TempatTutupUiState> = combine(local, connectivity.isOnline) { state, online ->
        state.copy(isOffline = !online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TempatTutupUiState())

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val day = date
        val closed = day?.let { plans.closedStops(it).firstOrNull { c -> c.stop.placeId == placeId } }
        if (day == null || closed == null) {
            local.update { it.copy(isLoading = false, notFound = true) }
            return
        }
        val replacement = plans.previewReplacement(day, placeId)
        local.update {
            it.copy(
                isLoading = false,
                stop = closed.stop,
                availability = closed.availability,
                replacement = replacement,
                selected = if (replacement != null) ClosedResolution.ReplaceWithOther else ClosedResolution.Remove,
            )
        }
    }

    fun select(resolution: ClosedResolution) = local.update {
        // Replace is only selectable when the builder found something.
        if (resolution == ClosedResolution.ReplaceWithOther && it.replacement == null) it else it.copy(selected = resolution)
    }

    /** "Terapkan". */
    fun apply() {
        val state = local.value
        val day = date ?: return
        if (state.isApplying || state.stop == null) return
        if (state.selected == ClosedResolution.MoveToOtherDay) {
            local.update { it.copy(outcome = TempatTutupOutcome.GoToWizard) }
            return
        }
        local.update { it.copy(isApplying = true) }
        viewModelScope.launch {
            val result = plans.applyReplan(day, placeId, replace = state.selected == ClosedResolution.ReplaceWithOther)
            local.update {
                it.copy(
                    isApplying = false,
                    outcome = when (result) {
                        is ReplanOutcome.Applied, ReplanOutcome.NotFound -> TempatTutupOutcome.Done
                        ReplanOutcome.Offline -> TempatTutupOutcome.GoOffline
                    },
                )
            }
        }
    }

    fun consumeOutcome() = local.update { it.copy(outcome = null) }
}

/** ISO date from a route argument, or null when missing or malformed. */
internal fun parseIsoDate(value: String?): LocalDate? =
    if (value == null) null else runCatching { LocalDate.parse(value) }.getOrNull()

package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlanStop
import id.kelana.feature.rencana.domain.Availability
import id.kelana.feature.rencana.domain.PlanRepository
import id.kelana.feature.rencana.domain.ReplanOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** "Mau diapakan?" options of the closed-place screen. */
enum class ClosedResolution { ReplaceWithOther, MoveToOtherDay, Remove }

/** What the screen does next. */
enum class TempatTutupOutcome { Done, GoToWizard }

@Immutable
data class TempatTutupUiState(
    val isLoading: Boolean = true,
    /** The place is not (or no longer) closed in the plan: the screen just leaves. */
    val notFound: Boolean = false,
    /** Loading the closed stop or its replacement threw: the screen shows an error with retry. */
    val loadError: Boolean = false,
    /** Applying threw or failed: the screen shows an inline banner and keeps the apply button enabled. */
    val applyError: Boolean = false,
    val stop: PlanStop? = null,
    val availability: Availability? = null,
    /** What the builder offers instead, or null when nothing else fits. */
    val replacement: PlanStop? = null,
    val selected: ClosedResolution = ClosedResolution.Remove,
    val isApplying: Boolean = false,
    val outcome: TempatTutupOutcome? = null,
)

/**
 * Live re-plan prompt for one closed place of the plan of [date]. Detection, the preview and applying all work offline
 * from cached data (AGENTS.md); a thrown error shows [TempatTutupUiState.loadError] or [TempatTutupUiState.applyError].
 */
@HiltViewModel
class TempatTutupViewModel(
    private val plans: PlanRepository,
    private val placeId: String,
    private val date: LocalDate?,
) : ViewModel() {

    /** Route arguments are stored in the handle under the field names of `TempatTutupRoute`. */
    @Inject
    constructor(plans: PlanRepository, savedStateHandle: SavedStateHandle) :
        this(
            plans,
            savedStateHandle.get<String>("placeId").orEmpty(),
            parseIsoDate(savedStateHandle.get<String>("date")),
        )

    private val local = MutableStateFlow(TempatTutupUiState())
    val uiState: StateFlow<TempatTutupUiState> = local.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    /** "Coba lagi" on the load error. */
    fun retryLoad() {
        local.update { it.copy(isLoading = true, loadError = false) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val day = date
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            local.update { it.copy(isLoading = false, loadError = true) }
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
        local.update { it.copy(isApplying = true, applyError = false) }
        viewModelScope.launch {
            val result = try {
                plans.applyReplan(day, placeId, replace = state.selected == ClosedResolution.ReplaceWithOther)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ReplanOutcome.Failed
            }
            local.update {
                when (result) {
                    is ReplanOutcome.Applied, ReplanOutcome.NotFound ->
                        it.copy(isApplying = false, outcome = TempatTutupOutcome.Done)
                    ReplanOutcome.Failed -> it.copy(isApplying = false, applyError = true)
                }
            }
        }
    }

    fun consumeOutcome() = local.update { it.copy(outcome = null) }
}

/** ISO date from a route argument, or null when missing or malformed. */
internal fun parseIsoDate(value: String?): LocalDate? =
    if (value == null) null else runCatching { LocalDate.parse(value) }.getOrNull()

package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PengingatUiState(
    val timing: ReminderTiming = ReminderTiming.WhenToLeave,
    val addToCalendar: Boolean = false,
    val saved: Boolean = false,
)

/**
 * "Kapan kami ingatkan?" for one show. PROVISIONAL: the choice is stored through the plan cache only. Scheduling the
 * notification and writing to the phone calendar belongs to the notifikasi feature and is not wired here.
 */
@HiltViewModel
class PengingatViewModel(
    private val plans: PlanRepository,
    private val eventId: String,
) : ViewModel() {

    /** The route argument is stored in the handle under the field name of `PengingatRoute`. */
    @Inject
    constructor(plans: PlanRepository, savedStateHandle: SavedStateHandle) :
        this(plans, savedStateHandle.get<String>("eventId").orEmpty())

    private val _state = MutableStateFlow(PengingatUiState())
    val uiState: StateFlow<PengingatUiState> = _state.asStateFlow()

    fun selectTiming(timing: ReminderTiming) = _state.update { it.copy(timing = timing) }

    fun setAddToCalendar(value: Boolean) = _state.update { it.copy(addToCalendar = value) }

    /** "Simpan". */
    fun save() {
        val s = _state.value
        if (eventId.isEmpty() || s.saved) return
        viewModelScope.launch {
            plans.saveReminder(eventId, ReminderChoice(s.timing, s.addToCalendar))
            _state.update { it.copy(saved = true) }
        }
    }
}

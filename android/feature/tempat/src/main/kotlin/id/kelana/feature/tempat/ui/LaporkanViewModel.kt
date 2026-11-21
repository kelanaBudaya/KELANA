package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.ReportDraft
import id.kelana.core.model.ReportReason
import id.kelana.core.model.ReportRepository
import id.kelana.core.model.ReportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The four reasons shown in the form, in display order. [ReportReason.Other] is not offered. */
val LaporkanReasons: List<ReportReason> = listOf(
    ReportReason.HoursOrPriceChanged,
    ReportReason.PlaceClosed,
    ReportReason.CeremonyShownWithoutPermission,
    ReportReason.InappropriateMedia,
)

const val MAX_DETAILS_LENGTH = 1000

data class LaporkanUiState(
    val reason: ReportReason? = null,
    val details: String = "",
    val submitting: Boolean = false,
    val failed: Boolean = false,
) {
    /** The submit button stays disabled until a reason is chosen. */
    val canSubmit: Boolean get() = reason != null && !submitting
}

/** One-off outcomes the screen reacts to. */
sealed interface LaporkanEvent {
    /** Signed in and sent: show thanks and go back. */
    data object Sent : LaporkanEvent

    /** Guest: open Masuk. */
    data object NeedsLogin : LaporkanEvent
}

@HiltViewModel
class LaporkanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()

    private val _uiState = MutableStateFlow(LaporkanUiState())
    val uiState: StateFlow<LaporkanUiState> = _uiState.asStateFlow()

    private val _events = Channel<LaporkanEvent>(Channel.BUFFERED)
    val events: Flow<LaporkanEvent> = _events.receiveAsFlow()

    fun selectReason(reason: ReportReason) = _uiState.update { it.copy(reason = reason, failed = false) }

    fun setDetails(text: String) = _uiState.update { it.copy(details = text.take(MAX_DETAILS_LENGTH)) }

    fun submit() {
        val current = _uiState.value
        val reason = current.reason ?: return
        if (current.submitting) return
        _uiState.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            val result = try {
                reports.submit(ReportDraft(placeId, reason, current.details.trim().ifEmpty { null }))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ReportResult.Failed
            }
            _uiState.update { it.copy(submitting = false, failed = result == ReportResult.Failed) }
            when (result) {
                ReportResult.Success -> _events.send(LaporkanEvent.Sent)
                ReportResult.NeedsLogin -> _events.send(LaporkanEvent.NeedsLogin)
                ReportResult.Failed -> Unit
            }
        }
    }
}

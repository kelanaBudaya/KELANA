package id.kelana.feature.akun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.ACCOUNT_DELETION_CONFIRM_WORD
import id.kelana.core.model.AccountDeletionRepository
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HapusAkunError { Network, Failed, NotConfigured, NotSignedIn }

/**
 * Hapus akun (HapusAkun.dc.html). [summary] counts are null until loaded or when unreadable; the screen then drops
 * the number. The confirmation is compared after trimming spaces and is case-sensitive: only `HAPUS` (as the artifact
 * label says, and as the server checks) enables the button; `hapus` does not.
 */
data class HapusAkunUiState(
    val summary: AccountDeletionSummary = AccountDeletionSummary(),
    val confirmation: String = "",
    val isDeleting: Boolean = false,
    val error: HapusAkunError? = null,
) {
    val isConfirmed: Boolean get() = confirmation.trim() == ACCOUNT_DELETION_CONFIRM_WORD
    val canDelete: Boolean get() = isConfirmed && !isDeleting
}

sealed interface HapusAkunEvent {
    /** The account is gone and the device is signed out: the app goes to Jelajah as a guest. */
    data object Deleted : HapusAkunEvent
}

@HiltViewModel
class HapusAkunViewModel @Inject constructor(
    private val repository: AccountDeletionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HapusAkunUiState())
    val state: StateFlow<HapusAkunUiState> = _state.asStateFlow()

    private val _events = Channel<HapusAkunEvent>(Channel.BUFFERED)
    val events: Flow<HapusAkunEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val summary = repository.summary()
            _state.update { it.copy(summary = summary) }
        }
    }

    fun onConfirmationChange(text: String) {
        if (_state.value.isDeleting) return
        _state.update { it.copy(confirmation = text.take(MAX_CONFIRMATION_LENGTH), error = null) }
    }

    fun onDelete() {
        if (!_state.value.canDelete) return
        _state.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            when (repository.deleteAccount()) {
                AccountDeletionResult.Deleted -> _events.trySend(HapusAkunEvent.Deleted)
                AccountDeletionResult.NetworkError -> fail(HapusAkunError.Network)
                AccountDeletionResult.Failed -> fail(HapusAkunError.Failed)
                AccountDeletionResult.NotConfigured -> fail(HapusAkunError.NotConfigured)
                AccountDeletionResult.NotSignedIn -> fail(HapusAkunError.NotSignedIn)
            }
        }
    }

    private fun fail(error: HapusAkunError) {
        // The typed word stays, so a retry is one tap.
        _state.update { it.copy(isDeleting = false, error = error) }
    }

    private companion object {
        const val MAX_CONFIRMATION_LENGTH = 20
    }
}

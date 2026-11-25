package id.kelana.feature.akun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.ACCOUNT_DELETION_CONFIRM_WORD
import id.kelana.core.model.AccountDeletionRepository
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.core.model.AccountExportRepository
import id.kelana.core.model.AccountExportResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class HapusAkunError { Network, Failed, NotConfigured, NotSignedIn }

/** "Unduh dataku dulu": idle, exporting (fetching, choosing the file, writing), saved, or error with retry. */
enum class DataExportState { Idle, Exporting, Saved, Error }

enum class DataExportError { Network, Failed, NotConfigured, NotSignedIn, SaveFailed }

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
    val export: DataExportState = DataExportState.Idle,
    val exportError: DataExportError? = null,
) {
    val isConfirmed: Boolean get() = confirmation.trim() == ACCOUNT_DELETION_CONFIRM_WORD
    val isExporting: Boolean get() = export == DataExportState.Exporting

    /** One request at a time: no deleting while the export is running. */
    val canDelete: Boolean get() = isConfirmed && !isDeleting && !isExporting
    val canExport: Boolean get() = !isDeleting && !isExporting
}

sealed interface HapusAkunEvent {
    /** The account is gone and the device is signed out: the app goes to Jelajah as a guest. */
    data object Deleted : HapusAkunEvent

    /** The export is ready: let the user create the file (Storage Access Framework) under [fileName]. */
    data class ChooseExportFile(val fileName: String) : HapusAkunEvent
}

@HiltViewModel
class HapusAkunViewModel internal constructor(
    private val repository: AccountDeletionRepository,
    private val exportRepository: AccountExportRepository,
    private val clock: Clock,
    private val zone: () -> ZoneId,
) : ViewModel() {

    @Inject constructor(
        repository: AccountDeletionRepository,
        exportRepository: AccountExportRepository,
        clock: Clock,
    ) : this(repository, exportRepository, clock, { ZoneId.systemDefault() })

    private val _state = MutableStateFlow(HapusAkunUiState())
    val state: StateFlow<HapusAkunUiState> = _state.asStateFlow()

    private val _events = Channel<HapusAkunEvent>(Channel.BUFFERED)
    val events: Flow<HapusAkunEvent> = _events.receiveAsFlow()

    /** The fetched document while the user picks the file. Memory only, dropped once written or cancelled. */
    private var pendingDocument: String? = null

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
            // Not cancellable: once the server deleted the account, leaving the screen must not stop the clean-up.
            val result = withContext(NonCancellable) {
                repository.deleteAccount()
            }
            when (result) {
                AccountDeletionResult.Deleted -> {
                    pendingDocument = null
                    _events.trySend(HapusAkunEvent.Deleted)
                }
                AccountDeletionResult.NetworkError -> fail(HapusAkunError.Network)
                AccountDeletionResult.Failed -> fail(HapusAkunError.Failed)
                AccountDeletionResult.NotConfigured -> fail(HapusAkunError.NotConfigured)
                AccountDeletionResult.NotSignedIn -> fail(HapusAkunError.NotSignedIn)
            }
        }
    }

    /** Starts (or retries) the export: fetch first, so a file is only created once there is data to put in it. */
    fun onExport() {
        if (!_state.value.canExport) return
        _state.update { it.copy(export = DataExportState.Exporting, exportError = null) }
        viewModelScope.launch {
            when (val result = exportRepository.export()) {
                is AccountExportResult.Ready -> {
                    pendingDocument = result.document
                    _events.trySend(HapusAkunEvent.ChooseExportFile(exportFileName()))
                }
                AccountExportResult.NetworkError -> exportFailed(DataExportError.Network)
                AccountExportResult.Failed -> exportFailed(DataExportError.Failed)
                AccountExportResult.NotConfigured -> exportFailed(DataExportError.NotConfigured)
                AccountExportResult.NotSignedIn -> exportFailed(DataExportError.NotSignedIn)
            }
        }
    }

    /** The file the user created, or null when they closed the picker (back to idle, nothing kept). */
    fun onExportDestination(destination: String?) {
        val document = pendingDocument
        pendingDocument = null
        if (destination == null) {
            _state.update { it.copy(export = DataExportState.Idle, exportError = null) }
            return
        }
        // No document (for example the app was restarted while the picker was open): ask for a retry.
        if (document == null) {
            exportFailed(DataExportError.Failed)
            return
        }
        viewModelScope.launch {
            if (exportRepository.save(destination, document)) {
                _state.update { it.copy(export = DataExportState.Saved, exportError = null) }
            } else {
                exportFailed(DataExportError.SaveFailed)
            }
        }
    }

    /** No app can create documents on this device. */
    fun onExportPickerUnavailable() {
        pendingDocument = null
        exportFailed(DataExportError.SaveFailed)
    }

    private fun exportFileName(): String =
        "kelana-dataku-${LocalDate.now(clock.withZone(zone())).format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"

    private fun exportFailed(error: DataExportError) {
        _state.update { it.copy(export = DataExportState.Error, exportError = error) }
    }

    private fun fail(error: HapusAkunError) {
        // The typed word stays, so a retry is one tap.
        _state.update { it.copy(isDeleting = false, error = error) }
    }

    private companion object {
        const val MAX_CONFIRMATION_LENGTH = 20
    }
}

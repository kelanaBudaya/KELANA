package id.kelana.feature.notifikasi.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.domain.NotificationSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PengaturanNotifUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val isLoaded: Boolean = false,
)

/** Pengaturan notifikasi: three switches, each saved at once. */
@HiltViewModel
class PengaturanNotifViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<PengaturanNotifUiState> = repository.settings
        .map { PengaturanNotifUiState(settings = it, isLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PengaturanNotifUiState())

    fun setShowReminders(value: Boolean) = save { it.copy(showReminders = value) }

    fun setStampConfirmations(value: Boolean) = save { it.copy(stampConfirmations = value) }

    fun setQuietHours(value: Boolean) = save { it.copy(quietHours = value) }

    private fun save(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}

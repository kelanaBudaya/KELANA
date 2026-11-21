package id.kelana.core.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes the current session to any screen without depending on :feature:akun. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    repository: SessionRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = repository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}

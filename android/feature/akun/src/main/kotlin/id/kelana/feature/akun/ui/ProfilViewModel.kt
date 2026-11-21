package id.kelana.feature.akun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Profil (artifact 8.2). [user] is null for a guest or while the session is still loading. */
data class ProfilUiState(
    val isLoading: Boolean = true,
    val user: SessionUser? = null,
    val isSigningOut: Boolean = false,
) {
    val isGuest: Boolean get() = !isLoading && user == null

    /** Real name from the account, falling back to the email. Never invented. */
    val name: String? get() = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email?.takeIf { it.isNotBlank() }

    /** The email line under the name; hidden when it would repeat the name or does not exist. */
    val email: String? get() = user?.email?.takeIf { it.isNotBlank() && it != name }

    /** Up to two initials from the name (or the email); empty when the account has neither. */
    val initials: String
        get() = name.orEmpty().split(' ', '@', '.').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
}

sealed interface ProfilEvent {
    /** The user is signed out: the app goes back to Jelajah. */
    data object SignedOut : ProfilEvent
}

@HiltViewModel
class ProfilViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfilUiState())
    val state: StateFlow<ProfilUiState> = _state.asStateFlow()

    private val _events = Channel<ProfilEvent>(Channel.BUFFERED)
    val events: Flow<ProfilEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            session.sessionState.collect { sessionState ->
                _state.update { current ->
                    // While signing out the session flips to Guest; keep showing the profile until we navigate away.
                    if (current.isSigningOut) return@update current
                    when (sessionState) {
                        SessionState.Loading -> current.copy(isLoading = true, user = null)
                        SessionState.Guest -> current.copy(isLoading = false, user = null)
                        is SessionState.SignedIn -> current.copy(isLoading = false, user = sessionState.user)
                    }
                }
            }
        }
    }

    fun onSignOut() {
        if (_state.value.isSigningOut || _state.value.user == null) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            session.signOut()
            _events.trySend(ProfilEvent.SignedOut)
        }
    }
}

package id.kelana.feature.akun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
import id.kelana.core.model.AuthConfig
import id.kelana.core.model.AuthResult
import id.kelana.core.model.SessionRepository
import id.kelana.feature.akun.domain.GoogleTokenResult
import id.kelana.feature.akun.domain.Nonce
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class MasukError { NotConfigured, Cancelled, NoGoogleAccount, Network, Generic }

data class MasukUiState(
    val isLoading: Boolean = false,
    val error: MasukError? = null,
)

sealed interface MasukEvent {
    /** The screen must ask Credential Manager for an ID token, then call [MasukViewModel.onGoogleResult]. */
    data class RequestGoogleToken(val hashedNonce: String, val webClientId: String) : MasukEvent

    data object SignedIn : MasukEvent

    data object ContinueAsGuest : MasukEvent
}

@HiltViewModel
class MasukViewModel internal constructor(
    private val sessionRepository: SessionRepository,
    private val authConfig: AuthConfig,
    private val appLanguage: AppLanguageRepository,
    private val nonceGenerator: () -> String,
) : ViewModel() {

    @javax.inject.Inject
    constructor(
        sessionRepository: SessionRepository,
        authConfig: AuthConfig,
        appLanguage: AppLanguageRepository,
    ) : this(sessionRepository, authConfig, appLanguage, { Nonce.generateRaw() })

    private val _state = MutableStateFlow(MasukUiState())
    val state: StateFlow<MasukUiState> = _state.asStateFlow()

    /** Language choice, kept apart from [state] because [state] is replaced whole on every login step. */
    val language: StateFlow<AppLanguage> =
        appLanguage.language.stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.ID)

    fun onLanguageSelected(language: AppLanguage) = appLanguage.set(language)

    private val _events = Channel<MasukEvent>(Channel.BUFFERED)
    val events: Flow<MasukEvent> = _events.receiveAsFlow()

    private var pendingNonce: String? = null

    fun onGoogleClick() {
        if (_state.value.isLoading) return
        if (!authConfig.isSupabaseConfigured || !authConfig.isGoogleConfigured) {
            _state.value = MasukUiState(error = MasukError.NotConfigured)
            return
        }
        val raw = nonceGenerator()
        pendingNonce = raw
        _state.value = MasukUiState(isLoading = true)
        _events.trySend(MasukEvent.RequestGoogleToken(Nonce.sha256Hex(raw), authConfig.googleWebClientId))
    }

    fun onGoogleResult(result: GoogleTokenResult) {
        when (result) {
            is GoogleTokenResult.Token -> exchange(result.idToken)
            GoogleTokenResult.Cancelled -> fail(MasukError.Cancelled)
            GoogleTokenResult.NoAccount -> fail(MasukError.NoGoogleAccount)
            GoogleTokenResult.Failure -> fail(MasukError.Generic)
        }
    }

    fun onGuestClick() {
        _events.trySend(MasukEvent.ContinueAsGuest)
    }

    private fun exchange(idToken: String) {
        val nonce = pendingNonce
        pendingNonce = null
        if (nonce == null) {
            fail(MasukError.Generic)
            return
        }
        viewModelScope.launch {
            when (sessionRepository.signInWithGoogle(idToken, nonce)) {
                AuthResult.Success -> {
                    _state.value = MasukUiState()
                    _events.trySend(MasukEvent.SignedIn)
                }
                AuthResult.NotConfigured -> fail(MasukError.NotConfigured)
                AuthResult.NetworkError -> fail(MasukError.Network)
                AuthResult.Failed -> fail(MasukError.Generic)
            }
        }
    }

    private fun fail(error: MasukError) {
        pendingNonce = null
        _state.value = MasukUiState(error = error)
    }
}

package id.kelana.feature.akun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.AuthValidation
import id.kelana.core.model.EmailAuthRepository
import id.kelana.core.model.EmailAuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasukEmailUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: EmailAuthError? = null,
    val passwordError: EmailAuthError? = null,
    val formError: EmailAuthError? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !isLoading
}

sealed interface MasukEmailEvent {
    data object SignedIn : MasukEmailEvent
}

@HiltViewModel
class MasukEmailViewModel @Inject constructor(
    private val emailAuth: EmailAuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MasukEmailUiState())
    val state: StateFlow<MasukEmailUiState> = _state.asStateFlow()

    private val _events = Channel<MasukEmailEvent>(Channel.BUFFERED)
    val events: Flow<MasukEmailEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun onTogglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        val email = current.email.trim()
        // Sign-in only checks the email shape; password strength rules apply at sign-up, not here.
        if (!AuthValidation.isValidEmail(email)) {
            _state.update { it.copy(emailError = EmailAuthError.InvalidEmail) }
            return
        }
        if (current.password.isEmpty()) {
            _state.update { it.copy(passwordError = EmailAuthError.PasswordRequired) }
            return
        }
        _state.update { it.copy(isLoading = true, emailError = null, passwordError = null, formError = null) }
        viewModelScope.launch {
            when (val result = emailAuth.signIn(email, current.password)) {
                EmailAuthResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.trySend(MasukEmailEvent.SignedIn)
                }
                is EmailAuthResult.Failure -> {
                    val error = result.reason.toUiError()
                    _state.update { it.withError(error).copy(isLoading = false) }
                }
            }
        }
    }

    private fun MasukEmailUiState.withError(error: EmailAuthError): MasukEmailUiState = when (error.slot()) {
        ErrorSlot.Email -> copy(emailError = error)
        ErrorSlot.Password -> copy(passwordError = error)
        ErrorSlot.Form -> copy(formError = error)
    }
}

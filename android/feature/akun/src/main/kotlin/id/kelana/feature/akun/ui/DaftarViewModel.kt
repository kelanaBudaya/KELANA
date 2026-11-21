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

data class DaftarUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: EmailAuthError? = null,
    val passwordError: EmailAuthError? = null,
    val confirmError: EmailAuthError? = null,
    val formError: EmailAuthError? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotEmpty() && confirmPassword.isNotEmpty() && !isLoading
}

sealed interface DaftarEvent {
    /** Sign-up creates an active account and a session at once, so this ends the flow like Google sign-in. */
    data object SignedIn : DaftarEvent
}

@HiltViewModel
class DaftarViewModel @Inject constructor(
    private val emailAuth: EmailAuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DaftarUiState())
    val state: StateFlow<DaftarUiState> = _state.asStateFlow()

    private val _events = Channel<DaftarEvent>(Channel.BUFFERED)
    val events: Flow<DaftarEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, confirmError = null, formError = null) }

    fun onConfirmChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmError = null, formError = null) }

    fun onTogglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        val email = current.email.trim()
        val emailError = if (AuthValidation.isValidEmail(email)) null else EmailAuthError.InvalidEmail
        val passwordError = if (AuthValidation.isValidPassword(current.password)) null else EmailAuthError.WeakPassword
        val confirmError = if (passwordError == null && current.confirmPassword != current.password) {
            EmailAuthError.PasswordMismatch
        } else {
            null
        }
        if (emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError)
            }
            return
        }
        _state.update {
            it.copy(isLoading = true, emailError = null, passwordError = null, confirmError = null, formError = null)
        }
        viewModelScope.launch {
            when (val result = emailAuth.signUp(email, current.password)) {
                EmailAuthResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.trySend(DaftarEvent.SignedIn)
                }
                is EmailAuthResult.Failure -> {
                    val error = result.reason.toUiError()
                    _state.update { it.withError(error).copy(isLoading = false) }
                }
            }
        }
    }

    private fun DaftarUiState.withError(error: EmailAuthError): DaftarUiState = when (error.slot()) {
        ErrorSlot.Email -> copy(emailError = error)
        ErrorSlot.Password -> copy(passwordError = error)
        ErrorSlot.Form -> copy(formError = error)
    }
}

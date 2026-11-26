package id.kelana.core.model

/**
 * Email + password auth. Sign-up creates an active account at once (email confirmation is off in the project).
 * A successful call lands in [SessionRepository.sessionState] exactly like Google sign-in; this interface does
 * not expose the session itself.
 */
interface EmailAuthRepository {
    /** [displayName] is the optional nickname, stored as user metadata `full_name`; null or blank = not set. */
    suspend fun signUp(email: String, password: String, displayName: String? = null): EmailAuthResult

    suspend fun signIn(email: String, password: String): EmailAuthResult
}

sealed interface EmailAuthResult {
    data object Success : EmailAuthResult

    data class Failure(val reason: EmailAuthFailure) : EmailAuthResult
}

/** Typed failures. The UI maps each to a string; the data layer never produces user-facing text. */
sealed interface EmailAuthFailure {
    data object InvalidEmail : EmailAuthFailure
    data object WeakPassword : EmailAuthFailure
    data object EmailAlreadyRegistered : EmailAuthFailure

    /** Wrong password or unknown email: deliberately not distinguished. */
    data object WrongCredentials : EmailAuthFailure

    /** Sign-up worked but returned no session: the project still requires email confirmation (misconfiguration). */
    data object ConfirmationRequired : EmailAuthFailure

    data class RateLimited(val retryAfterSeconds: Int?) : EmailAuthFailure
    data object NetworkError : EmailAuthFailure
    data object NotConfigured : EmailAuthFailure
    data object Unknown : EmailAuthFailure
}

package id.kelana.core.data.auth

import id.kelana.core.model.EmailAuthFailure

/** Which call failed. Supabase reuses some error codes across calls, so the mapping needs the context. */
enum class EmailAuthOperation { SignUp, SignIn }

/** Library-neutral description of what went wrong, so the mapping below is pure and testable. */
sealed interface RawAuthError {
    data object NotConfigured : RawAuthError
    data object Network : RawAuthError

    /** An error response from Supabase Auth. [code] is the raw `error_code` string (may be null or unknown). */
    data class Rest(val code: String?, val status: Int?, val retryAfterSeconds: Int?) : RawAuthError
    data object Other : RawAuthError
}

/** The one place where Supabase error codes become typed failures. */
fun mapEmailAuthError(error: RawAuthError, op: EmailAuthOperation): EmailAuthFailure = when (error) {
    RawAuthError.NotConfigured -> EmailAuthFailure.NotConfigured
    RawAuthError.Network -> EmailAuthFailure.NetworkError
    RawAuthError.Other -> EmailAuthFailure.Unknown
    is RawAuthError.Rest -> mapRest(error, op)
}

private fun mapRest(e: RawAuthError.Rest, op: EmailAuthOperation): EmailAuthFailure = when {
    e.code == "over_email_send_rate_limit" || e.code == "over_request_rate_limit" || e.status == 429 ->
        EmailAuthFailure.RateLimited(e.retryAfterSeconds)
    op == EmailAuthOperation.SignUp && e.code == "weak_password" -> EmailAuthFailure.WeakPassword
    op == EmailAuthOperation.SignUp && (e.code == "email_exists" || e.code == "user_already_exists") ->
        EmailAuthFailure.EmailAlreadyRegistered
    op == EmailAuthOperation.SignUp && e.code == "validation_failed" -> EmailAuthFailure.InvalidEmail
    // Unknown email and wrong password both come back as invalid_credentials; an unconfirmed account is
    // treated the same so nothing about the account leaks.
    op == EmailAuthOperation.SignIn && (e.code == "invalid_credentials" || e.code == "email_not_confirmed") ->
        EmailAuthFailure.WrongCredentials
    else -> EmailAuthFailure.Unknown
}

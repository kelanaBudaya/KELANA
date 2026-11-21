package id.kelana.feature.akun.ui

import androidx.annotation.StringRes
import id.kelana.core.model.EmailAuthFailure
import id.kelana.feature.akun.R

/** Every message the email screens can show. Mapped to strings in [messageRes]; the data layer never has text. */
enum class EmailAuthError {
    InvalidEmail,
    PasswordRequired,
    WeakPassword,
    PasswordMismatch,
    EmailAlreadyRegistered,
    WrongCredentials,
    ConfirmationRequired,
    RateLimited,
    Network,
    NotConfigured,
    Unknown,
}

internal fun EmailAuthFailure.toUiError(): EmailAuthError = when (this) {
    EmailAuthFailure.InvalidEmail -> EmailAuthError.InvalidEmail
    EmailAuthFailure.WeakPassword -> EmailAuthError.WeakPassword
    EmailAuthFailure.EmailAlreadyRegistered -> EmailAuthError.EmailAlreadyRegistered
    EmailAuthFailure.WrongCredentials -> EmailAuthError.WrongCredentials
    EmailAuthFailure.ConfirmationRequired -> EmailAuthError.ConfirmationRequired
    is EmailAuthFailure.RateLimited -> EmailAuthError.RateLimited
    EmailAuthFailure.NetworkError -> EmailAuthError.Network
    EmailAuthFailure.NotConfigured -> EmailAuthError.NotConfigured
    EmailAuthFailure.Unknown -> EmailAuthError.Unknown
}

@StringRes
internal fun EmailAuthError.messageRes(): Int = when (this) {
    EmailAuthError.InvalidEmail -> R.string.email_error_invalid_email
    EmailAuthError.PasswordRequired -> R.string.email_error_password_required
    EmailAuthError.WeakPassword -> R.string.email_error_weak_password
    EmailAuthError.PasswordMismatch -> R.string.email_error_password_mismatch
    EmailAuthError.EmailAlreadyRegistered -> R.string.email_error_already_registered
    EmailAuthError.WrongCredentials -> R.string.email_error_wrong_credentials
    EmailAuthError.ConfirmationRequired -> R.string.email_error_confirmation_required
    EmailAuthError.RateLimited -> R.string.email_error_rate_limited
    EmailAuthError.Network -> R.string.email_error_network
    EmailAuthError.NotConfigured -> R.string.email_error_not_configured
    EmailAuthError.Unknown -> R.string.email_error_generic
}

/** Where an error from the server is shown: under a field, or as a message above the button. */
internal sealed interface ErrorSlot {
    data object Email : ErrorSlot
    data object Password : ErrorSlot
    data object Form : ErrorSlot
}

internal fun EmailAuthError.slot(): ErrorSlot = when (this) {
    EmailAuthError.InvalidEmail, EmailAuthError.EmailAlreadyRegistered -> ErrorSlot.Email
    EmailAuthError.PasswordRequired, EmailAuthError.WeakPassword, EmailAuthError.PasswordMismatch -> ErrorSlot.Password
    else -> ErrorSlot.Form
}

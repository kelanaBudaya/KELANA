package id.kelana.feature.akun.domain

/** Result of asking Credential Manager for a Google ID token. */
sealed interface GoogleTokenResult {
    data class Token(val idToken: String) : GoogleTokenResult

    /** The user closed the account picker. */
    data object Cancelled : GoogleTokenResult

    /** No Google account on the device. */
    data object NoAccount : GoogleTokenResult

    data object Failure : GoogleTokenResult
}

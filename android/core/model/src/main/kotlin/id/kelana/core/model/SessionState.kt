package id.kelana.core.model

/** Who is using the app right now. Guests can browse; most actions need [SignedIn]. */
sealed interface SessionState {
    /** The stored session is still being read. */
    data object Loading : SessionState

    /** Browsing without an account. */
    data object Guest : SessionState

    data class SignedIn(val user: SessionUser) : SessionState
}

data class SessionUser(
    val id: String,
    val email: String?,
    val displayName: String?,
)

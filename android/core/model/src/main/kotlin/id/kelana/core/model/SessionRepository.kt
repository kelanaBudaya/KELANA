package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/** Outcome of a sign-in attempt. */
enum class AuthResult {
    Success,

    /** Supabase url or anon key is not set in this build. */
    NotConfigured,
    NetworkError,
    Failed,
}

interface SessionRepository {
    val sessionState: Flow<SessionState>

    /**
     * Exchanges a Google ID token for a Supabase session.
     * [nonce] is the raw nonce; Google received its SHA-256 hash.
     */
    suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult

    suspend fun signOut()

    /**
     * Stores [name] as the account's display name (user metadata `full_name`). [sessionState] then emits the
     * signed-in user with the new name. The caller validates the name; blank is rejected here as [AuthResult.Failed].
     */
    suspend fun updateDisplayName(name: String): AuthResult
}

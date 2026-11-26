package id.kelana.core.data.auth

/** The two Supabase Auth calls the email flow needs. A fake of this is used in tests; no network there. */
interface EmailAuthApi {
    /** Returns true when the call left a signed-in session, false when the project still wants confirmation. */
    suspend fun signUp(email: String, password: String, displayName: String?): Boolean

    suspend fun signIn(email: String, password: String)
}

/** Lets an [EmailAuthApi] implementation report an already classified error (used by test fakes). */
class AuthApiException(val raw: RawAuthError) : Exception("Auth error: $raw")

/** Thrown by the API when the build has no Supabase url or anon key. */
class AuthNotConfiguredException : Exception("Supabase is not configured")

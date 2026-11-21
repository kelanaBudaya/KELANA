package id.kelana.core.data.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * supabase-kt 3.2.6. signUpWith and signInWith import the session into the client themselves, so
 * [SupabaseSessionRepository.sessionState] picks it up; nothing here duplicates session handling.
 */
@Singleton
class SupabaseEmailAuthApi @Inject constructor(
    private val provider: SupabaseClientProvider,
) : EmailAuthApi {
    private val auth get() = (provider.client ?: throw AuthNotConfiguredException()).auth

    override suspend fun signUp(email: String, password: String): Boolean {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentSessionOrNull() != null
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }
}

/** Reads a thrown exception into the library-neutral form. Cancellation must be rethrown before calling this. */
fun Throwable.toRawAuthError(): RawAuthError = when (this) {
    is AuthNotConfiguredException -> RawAuthError.NotConfigured
    is AuthApiException -> raw
    // AuthRestException and AuthWeakPasswordException are RestException subclasses; error holds the raw code.
    is RestException -> RawAuthError.Rest(
        code = error,
        status = statusCode,
        retryAfterSeconds = response.headers["Retry-After"]?.trim()?.toIntOrNull(),
    )
    is HttpRequestException, is HttpRequestTimeoutException, is IOException -> RawAuthError.Network
    else -> RawAuthError.Other
}

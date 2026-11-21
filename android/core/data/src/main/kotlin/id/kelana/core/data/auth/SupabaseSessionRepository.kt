package id.kelana.core.data.auth

import id.kelana.core.model.AuthResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSessionRepository @Inject constructor(
    private val provider: SupabaseClientProvider,
) : SessionRepository {

    override val sessionState: Flow<SessionState> = flow {
        val client = provider.client
        if (client == null) {
            emit(SessionState.Guest)
        } else {
            emitAll(client.auth.sessionStatus.map { status -> status.toSessionState() })
        }
    }

    override suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult {
        val client = provider.client ?: return AuthResult.NotConfigured
        return try {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = nonce
            }
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (_: HttpRequestException) {
            AuthResult.NetworkError
        } catch (_: IOException) {
            AuthResult.NetworkError
        } catch (_: Exception) {
            AuthResult.Failed
        }
    }

    override suspend fun signOut() {
        val client = provider.client ?: return
        try {
            client.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Signing out must never crash the app.
        }
    }

    private fun SessionStatus.toSessionState(): SessionState = when (this) {
        is SessionStatus.Initializing -> SessionState.Loading
        is SessionStatus.Authenticated -> {
            val user = session.user
            if (user == null) {
                SessionState.Guest
            } else {
                SessionState.SignedIn(
                    SessionUser(
                        id = user.id,
                        email = user.email,
                        displayName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull,
                    ),
                )
            }
        }
        else -> SessionState.Guest
    }
}

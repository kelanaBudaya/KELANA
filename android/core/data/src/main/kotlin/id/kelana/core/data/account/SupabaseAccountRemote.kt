package id.kelana.core.data.account

import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.model.ACCOUNT_DELETION_CONFIRM_WORD
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls `delete-account` with the signed-in user's JWT (the supabase-kt client attaches it). The function takes the
 * user id from that JWT only; nothing here can name another user. The service role key never exists in the app.
 * Not covered by unit tests (no live calls).
 */
@Singleton
class SupabaseAccountRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : AccountRemote {

    private val json = Json { ignoreUnknownKeys = true }

    override val isConfigured: Boolean get() = provider.authConfig.isSupabaseConfigured

    override suspend fun countOwn(table: OwnTable, userId: String): Int? {
        val client = provider.client ?: return null
        return try {
            client.from(table.tableName)
                .select {
                    head = true
                    count(Count.EXACT)
                    filter { eq("user_id", userId) }
                }
                .countOrNull()
                ?.toInt()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteAccount(): DeleteAccountOutcome {
        val client = provider.client ?: return DeleteAccountOutcome.Refused
        return try {
            val response = client.functions.invoke(
                function = FUNCTION_NAME,
                body = DeleteAccountBody(confirm = ACCOUNT_DELETION_CONFIRM_WORD),
            )
            val status = runCatching { json.decodeFromString<DeleteAccountResponseDto>(response.bodyAsText()).status }
                .getOrNull()
            if (response.status.value == 200 && status == "deleted") {
                DeleteAccountOutcome.Deleted
            } else {
                DeleteAccountOutcome.Refused
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: RestException) {
            DeleteAccountOutcome.Refused
        } catch (_: HttpRequestException) {
            DeleteAccountOutcome.Unreachable
        } catch (_: IOException) {
            DeleteAccountOutcome.Unreachable
        } catch (_: Exception) {
            DeleteAccountOutcome.Refused
        }
    }

    override suspend fun clearLocalSession() {
        val client = provider.client ?: return
        try {
            // Not signOut(): that calls the server's logout for a user that no longer exists and may throw before
            // the local session is dropped. clearSession() only deletes the stored session and flips the status.
            client.auth.clearSession()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Must never crash; the session status still ends up NotAuthenticated on next start since the
            // refresh token belongs to a deleted user.
        }
    }

    private companion object {
        const val FUNCTION_NAME = "delete-account"
    }
}

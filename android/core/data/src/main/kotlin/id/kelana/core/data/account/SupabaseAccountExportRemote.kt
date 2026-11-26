package id.kelana.core.data.account

import id.kelana.core.data.auth.SupabaseClientProvider
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls `export-account` with the signed-in user's JWT (the supabase-kt client attaches it). The function takes the
 * user id from that JWT only; nothing here can name another user. Not covered by unit tests (no live calls).
 */
@Singleton
class SupabaseAccountExportRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : AccountExportRemote {

    override val isConfigured: Boolean get() = provider.authConfig.isSupabaseConfigured

    override suspend fun exportAccount(): ExportAccountOutcome {
        val client = provider.client ?: return ExportAccountOutcome.Refused
        return try {
            // The function ignores the body; an empty object keeps the request a plain JSON POST.
            val response = client.functions.invoke(function = FUNCTION_NAME, body = JsonObject(emptyMap()))
            if (response.status.value == 200) {
                ExportAccountOutcome.Exported(response.bodyAsText())
            } else {
                ExportAccountOutcome.Refused
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: RestException) {
            ExportAccountOutcome.Refused
        } catch (_: HttpRequestException) {
            ExportAccountOutcome.Unreachable
        } catch (_: IOException) {
            ExportAccountOutcome.Unreachable
        } catch (_: Exception) {
            ExportAccountOutcome.Refused
        }
    }

    private companion object {
        const val FUNCTION_NAME = "export-account"
    }
}

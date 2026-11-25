package id.kelana.core.data.account

import id.kelana.core.model.AccountExportRepository
import id.kelana.core.model.AccountExportResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Unduh dataku dulu" on top of the `export-account` Edge Function. The server builds the document for the JWT's
 * user; this class asks, then checks that the answer is an export (format and version) of the signed-in user before
 * handing it on. The document is passed through unchanged and is not kept on the device by this class.
 */
@Singleton
class SupabaseAccountExportRepository @Inject constructor(
    private val session: SessionRepository,
    private val remote: AccountExportRemote,
    private val writer: ExportDocumentWriter,
) : AccountExportRepository {

    override suspend fun export(): AccountExportResult {
        val userId = (session.sessionState.first { it !is SessionState.Loading } as? SessionState.SignedIn)?.user?.id
            ?: return AccountExportResult.NotSignedIn
        if (!remote.isConfigured) return AccountExportResult.NotConfigured
        return when (val outcome = remote.exportAccount()) {
            is ExportAccountOutcome.Exported ->
                if (isExportOf(outcome.body, userId)) {
                    AccountExportResult.Ready(outcome.body)
                } else {
                    AccountExportResult.Failed
                }
            ExportAccountOutcome.Unreachable -> AccountExportResult.NetworkError
            ExportAccountOutcome.Refused -> AccountExportResult.Failed
        }
    }

    override suspend fun save(destination: String, document: String): Boolean = try {
        writer.write(destination, document)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private fun isExportOf(body: String, userId: String): Boolean = try {
        val root = json.parseToJsonElement(body).jsonObject
        root.string("format") == EXPORT_FORMAT &&
            root.string("version") == EXPORT_VERSION &&
            (root["account"] as? JsonObject)?.string("id") == userId
    } catch (_: Exception) {
        false
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content

    internal companion object {
        /** Must match `EXPORT_FORMAT` / `EXPORT_VERSION` in supabase/functions/export-account/export.ts. */
        const val EXPORT_FORMAT = "kelana-account-export"
        const val EXPORT_VERSION = "1"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

package id.kelana.core.data.postcard

import id.kelana.core.data.auth.SupabaseClientProvider
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to Supabase with the signed-in user's JWT (the supabase-kt client attaches it). Turns supabase-kt's
 * [RestException] into [PostcardHttpException] so the repository does not depend on the library. Never logs
 * bodies. Not covered by unit tests (no live calls).
 */
@Singleton
class SupabasePostcardRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : PostcardRemote {

    override val isConfigured: Boolean get() = provider.authConfig.isSupabaseConfigured

    override suspend fun upload(path: String, bytes: ByteArray, mimeType: String) {
        val client = provider.client ?: throw PostcardHttpException(0, "not configured")
        try {
            client.storage.from(BUCKET).upload(path, bytes) { upsert = false }
        } catch (e: RestException) {
            throw e.asHttp()
        }
    }

    override suspend fun insert(row: PostcardInsertDto) {
        val client = provider.client ?: throw PostcardHttpException(0, "not configured")
        try {
            client.from("postcards").insert(row)
        } catch (e: RestException) {
            throw e.asHttp()
        }
    }

    override suspend fun deleteFile(path: String) {
        val client = provider.client ?: return
        try {
            client.storage.from(BUCKET).delete(path)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best effort: an orphan file is private to its owner and harmless.
        }
    }

    /** Storage reports an RLS refusal as 400 with a row-level-security message; treat it like the 403 it is. */
    private fun RestException.asHttp(): PostcardHttpException {
        val text = "$error $description"
        val denied = statusCode == 403 || text.contains("row-level security", ignoreCase = true)
        return PostcardHttpException(if (denied) 403 else statusCode)
    }

    private companion object {
        const val BUCKET = "postcards"
    }
}

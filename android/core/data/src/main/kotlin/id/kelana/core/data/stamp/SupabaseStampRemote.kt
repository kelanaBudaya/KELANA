package id.kelana.core.data.stamp

import id.kelana.core.data.auth.SupabaseClientProvider
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls `verify-stamp` with the signed-in user's JWT (the supabase-kt client attaches it) and reads the
 * user's own rows from `stamps`. Never logs the body or the response. Not covered by unit tests (no live calls).
 */
@Singleton
class SupabaseStampRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : StampRemote {

    private val json = Json { ignoreUnknownKeys = true }

    override val isConfigured: Boolean get() = provider.authConfig.isSupabaseConfigured

    override suspend fun fetchOwnStamps(): List<StampDto> {
        val client = provider.client ?: return emptyList()
        return client.from("stamps").select(Columns.raw("place_id,stamped_at")).decodeList<StampDto>()
    }

    override suspend fun verify(body: VerifyStampBody): VerifyOutcome {
        val client = provider.client ?: return VerifyOutcome.Unreachable
        return try {
            val response = client.functions.invoke(function = FUNCTION_NAME, body = body)
            answerOf(response.status.value, response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            // 400 carries a bad_request decision; anything else (401, 5xx) is not an answer.
            if (e.statusCode == 400) {
                VerifyOutcome.Answer(status = "rejected", reason = "bad_request", distanceM = null)
            } else {
                VerifyOutcome.Unreachable
            }
        } catch (_: Exception) {
            VerifyOutcome.Unreachable
        }
    }

    private fun answerOf(httpStatus: Int, text: String): VerifyOutcome {
        if (httpStatus == 401 || httpStatus >= 500) return VerifyOutcome.Unreachable
        return try {
            val dto = json.decodeFromString<VerifyStampResponseDto>(text)
            VerifyOutcome.Answer(dto.status, dto.reason, dto.distanceM)
        } catch (_: Exception) {
            VerifyOutcome.Unreachable
        }
    }

    private companion object {
        const val FUNCTION_NAME = "verify-stamp"
    }
}

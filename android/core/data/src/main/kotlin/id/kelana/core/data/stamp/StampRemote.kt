package id.kelana.core.data.stamp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server side of stamping. Real implementation: [SupabaseStampRemote]. Tests use a fake. */
interface StampRemote {
    /** False when the Supabase url or anon key is missing in this build. */
    val isConfigured: Boolean

    /** The signed-in user's own stamps (RLS). Throws on network failure. */
    suspend fun fetchOwnStamps(): List<StampDto>

    suspend fun verify(body: VerifyStampBody): VerifyOutcome
}

sealed interface VerifyOutcome {
    /** The server decided. [status] is `stamped`, `already_stamped` or `rejected`. */
    data class Answer(val status: String, val reason: String?, val distanceM: Int?) : VerifyOutcome

    /** No usable answer (offline, timeout, 5xx, 401, unreadable body). The request stays queued. */
    data object Unreachable : VerifyOutcome
}

@Serializable
data class StampDto(
    @SerialName("place_id") val placeId: String,
    @SerialName("stamped_at") val stampedAt: String,
)

@Serializable
data class VerifyStampResponseDto(
    val status: String,
    val reason: String? = null,
    val distanceM: Int? = null,
)

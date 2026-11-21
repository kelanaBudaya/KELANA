package id.kelana.core.model

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** A stamp the server confirmed for the signed-in user. Only the server creates these. */
data class Stamp(
    val placeId: String,
    val stampedAt: Instant,
)

/**
 * The one stamp request waiting for signal. Deliberately holds no location: the fix lives only in the
 * local queue row and is never exposed. A pending request expires [STAMP_QUEUE_EXPIRY_MINUTES] minutes
 * after [capturedAt].
 */
data class PendingStamp(
    val placeId: String,
    val capturedAt: Instant,
)

/** Matches the server's MAX_CAPTURE_AGE_MS (exactly 30 minutes is still accepted). */
const val STAMP_QUEUE_EXPIRY_MINUTES: Long = 30

/**
 * A fix the feature read once from `getCurrentLocation()` in the "Ambil cap" handler. This layer never
 * reads location itself. [toString] hides the coordinates so they cannot end up in logs.
 */
class StampRequest(
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
    val isMock: Boolean,
    val capturedAt: Instant,
) {
    override fun toString(): String = "StampRequest(placeId=$placeId, capturedAt=$capturedAt)"
}

/** Reasons the server rejects a stamp, with the exact wire value of `verify-stamp`. */
enum class StampRejectReason(val wire: String) {
    TooFar("too_far"),
    Expired("expired"),
    FutureTimestamp("future_timestamp"),
    LowAccuracy("low_accuracy"),
    MockLocation("mock_location"),
    IntegrityFailed("integrity_failed"),
    PlaceUnknown("place_unknown"),
    RadiusNotSet("radius_not_set"),
    BadRequest("bad_request"),
    ;

    companion object {
        fun fromWire(value: String?): StampRejectReason? = entries.firstOrNull { it.wire == value }
    }
}

sealed interface StampResult {
    /** Server confirmed a new stamp. */
    data object Stamped : StampResult

    /** Server says this account already holds the stamp. */
    data object AlreadyStamped : StampResult

    /** Server decided no. [distanceM] is set only with [StampRejectReason.TooFar]. */
    data class Rejected(val reason: StampRejectReason, val distanceM: Int? = null) : StampResult

    /** The request is queued (no signal yet, or the send did not get an answer): show "menunggu sinyal". */
    data object Queued : StampResult

    /** Guest: stamping needs an account. */
    data object NeedsLogin : StampResult

    /** Could not reach the server or get an integrity token; the request stays queued and is retried. */
    data object NetworkError : StampResult

    /** Supabase (or, in production, Play Integrity) is not configured in this build. Nothing is queued. */
    data object NotConfigured : StampResult

    /** The request was older than 30 minutes and was dropped on the client. Ask the user to try again. */
    data object Expired : StampResult
}

interface StampRepository {
    /** Own stamps (passport), cached for offline. Empty for guests. A stamp appears only after the server confirmed it. */
    fun stamps(): Flow<List<Stamp>>

    /**
     * Sends [request], or queues it when there is no signal. Takes a fix that is already captured.
     * Stores the fix only in the pending row until the server answers.
     */
    suspend fun requestStamp(request: StampRequest): StampResult

    /** The queued request, or null. Drives the "menunggu sinyal" state. */
    fun pending(): Flow<PendingStamp?>

    /** Drops the queued request and its fix. */
    suspend fun cancelPending()
}

package id.kelana.core.data.stamp

import id.kelana.core.database.dao.OwnStampDao
import id.kelana.core.database.dao.PendingStampDao
import id.kelana.core.database.entity.OwnStampEntity
import id.kelana.core.database.entity.PendingStampEntity
import id.kelana.core.model.STAMP_QUEUE_EXPIRY_MINUTES
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.NotificationRepository
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** True when a fix taken at [capturedAtMillis] is older than the 30-minute queue limit at [now]. Exactly 30 minutes is allowed. */
fun isStampExpired(capturedAtMillis: Long, now: Instant): Boolean =
    Duration.between(Instant.ofEpochMilli(capturedAtMillis), now) > Duration.ofMinutes(STAMP_QUEUE_EXPIRY_MINUTES)

/** Offline copy of the user's own stamps; only server-confirmed rows get in. */
@Singleton
class OwnStampsCache @Inject constructor(
    private val remote: StampRemote,
    private val dao: OwnStampDao,
    private val clock: Clock,
) {
    /** Replaces the cache of [userId] with the server list. Best effort: returns false on any failure. */
    suspend fun refresh(userId: String): Boolean {
        if (!remote.isConfigured) return false
        return try {
            val rows = remote.fetchOwnStamps().mapNotNull { dto ->
                val millis = parseServerInstant(dto.stampedAt)?.toEpochMilli() ?: return@mapNotNull null
                OwnStampEntity(userId = userId, placeId = dto.placeId.lowercase(), stampedAtMillis = millis)
            }
            dao.replaceForUser(userId, rows)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /** The server just confirmed [placeId]. Prefer the server row; if the refresh fails, keep a local copy until the next refresh. */
    suspend fun recordConfirmed(userId: String, placeId: String) {
        if (!refresh(userId)) {
            dao.insertIfAbsent(OwnStampEntity(userId, placeId.lowercase(), clock.instant().toEpochMilli()))
        }
    }
}

internal fun parseServerInstant(text: String): Instant? = try {
    java.time.OffsetDateTime.parse(text).toInstant()
} catch (_: Exception) {
    null
}

/**
 * Sends the single pending stamp request. Shared by the repository (direct send when online) and the
 * WorkManager job. Requests the Play Integrity token HERE, at send time, with the nonce of the payload; the
 * token is passed straight to the request and never stored. The pending row (the only copy of the fix) is
 * deleted on ANY server answer, when older than 30 minutes, and when its account is no longer signed in.
 */
@Singleton
class StampSender @Inject constructor(
    private val pendingDao: PendingStampDao,
    private val cache: OwnStampsCache,
    private val remote: StampRemote,
    private val integrity: IntegrityTokenProvider,
    private val session: SessionRepository,
    private val clock: Clock,
    private val notifications: NotificationRepository,
) {
    private val mutex = Mutex()

    /**
     * Result of one send attempt, or null when nothing is pending.
     * [StampResult.NetworkError] means "still queued, try again later"; every other result is final and
     * the row is gone.
     */
    suspend fun sendPending(): StampResult? = mutex.withLock {
        val row = pendingDao.get() ?: return@withLock null

        val state = session.sessionState.first { it !is SessionState.Loading }
        if (state !is SessionState.SignedIn || state.user.id != row.userId) {
            drop()
            return@withLock StampResult.NeedsLogin
        }
        if (isStampExpired(row.capturedAtMillis, clock.instant())) {
            drop()
            return@withLock StampResult.Expired
        }
        if (!remote.isConfigured) {
            drop()
            return@withLock StampResult.NotConfigured
        }
        val payload = payloadOf(row)
        if (payload == null) {
            drop()
            return@withLock StampResult.Rejected(StampRejectReason.BadRequest)
        }

        val token = when (val t = integrity.requestToken(payload.nonce())) {
            is IntegrityTokenResult.Token -> t.value
            IntegrityTokenResult.NotConfigured -> {
                drop()
                return@withLock StampResult.NotConfigured
            }
            IntegrityTokenResult.Unavailable -> return@withLock StampResult.NetworkError
        }

        when (val outcome = remote.verify(payload.toBody(token))) {
            VerifyOutcome.Unreachable -> StampResult.NetworkError
            is VerifyOutcome.Answer -> {
                drop() // the fix is gone as soon as the server has answered, whatever the answer
                answerToResult(outcome, state.user.id, row.placeId)
            }
        }
    }

    private suspend fun answerToResult(answer: VerifyOutcome.Answer, userId: String, placeId: String): StampResult =
        when (answer.status) {
            "stamped" -> {
                cache.recordConfirmed(userId, placeId)
                announceStamp(placeId)
                StampResult.Stamped
            }
            "already_stamped" -> {
                cache.recordConfirmed(userId, placeId)
                StampResult.AlreadyStamped
            }
            else -> StampResult.Rejected(
                reason = StampRejectReason.fromWire(answer.reason) ?: StampRejectReason.BadRequest,
                distanceM = if (answer.reason == StampRejectReason.TooFar.wire) answer.distanceM else null,
            )
        }

    /** Tells the notification center. Only called for a confirmed stamp; a failure here never changes the stamp result. */
    private suspend fun announceStamp(placeId: String) {
        try {
            notifications.post(NotificationEvent.StampConfirmed(placeId))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun drop() = withContext(NonCancellable) { pendingDao.clear() }

    private fun payloadOf(row: PendingStampEntity): StampPayload? = StampPayload.from(
        placeId = row.placeId,
        lat = row.lat,
        lng = row.lng,
        accuracyM = row.accuracyM,
        isMock = row.isMock,
        capturedAt = Instant.ofEpochMilli(row.capturedAtMillis),
    )
}

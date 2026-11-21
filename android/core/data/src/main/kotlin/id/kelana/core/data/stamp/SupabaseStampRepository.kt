package id.kelana.core.data.stamp

import id.kelana.core.database.dao.OwnStampDao
import id.kelana.core.database.dao.PendingStampDao
import id.kelana.core.database.entity.PendingStampEntity
import id.kelana.core.model.PendingStamp
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampRepository
import id.kelana.core.model.StampRequest
import id.kelana.core.model.StampResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stamping on top of Supabase. It never reads location: the feature passes in one captured fix.
 * The fix is kept only in the pending row until the server answers; the server alone decides.
 * Guests get [StampResult.NeedsLogin]. A queued request is sent directly when online and by the
 * WorkManager job (via [StampSendScheduler]) when there is no signal.
 */
@Singleton
class SupabaseStampRepository @Inject constructor(
    private val session: SessionRepository,
    private val remote: StampRemote,
    private val pendingDao: PendingStampDao,
    private val ownStampDao: OwnStampDao,
    private val cache: OwnStampsCache,
    private val sender: StampSender,
    private val scheduler: StampSendScheduler,
    private val network: NetworkMonitor,
    private val clock: Clock,
) : StampRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stamps(): Flow<List<Stamp>> = session.sessionState.flatMapLatest { state ->
        when (state) {
            is SessionState.SignedIn -> ownStamps(state.user.id)
            else -> flowOf(emptyList())
        }
    }

    private fun ownStamps(userId: String): Flow<List<Stamp>> = channelFlow {
        // Refresh from the server in the background; the cache keeps serving offline.
        launch { cache.refresh(userId) }
        ownStampDao.observe(userId).collect { rows ->
            send(rows.map { Stamp(it.placeId, Instant.ofEpochMilli(it.stampedAtMillis)) })
        }
    }

    override fun pending(): Flow<PendingStamp?> = pendingDao.observe().map { row ->
        row?.let { PendingStamp(it.placeId, Instant.ofEpochMilli(it.capturedAtMillis)) }
    }

    override suspend fun requestStamp(request: StampRequest): StampResult {
        val state = session.sessionState.first { it !is SessionState.Loading }
        if (state !is SessionState.SignedIn) return StampResult.NeedsLogin
        if (!remote.isConfigured) return StampResult.NotConfigured

        val capturedAtMillis = request.capturedAt.toEpochMilli()
        if (isStampExpired(capturedAtMillis, clock.instant())) return StampResult.Expired
        // A fix that can never be a valid request is dropped here and never stored.
        StampPayload.from(request) ?: return StampResult.Rejected(StampRejectReason.BadRequest)

        pendingDao.put(
            PendingStampEntity(
                userId = state.user.id,
                placeId = request.placeId.lowercase(),
                lat = request.lat,
                lng = request.lng,
                accuracyM = request.accuracyM,
                isMock = request.isMock,
                capturedAtMillis = capturedAtMillis,
            ),
        )

        if (!network.isOnline()) {
            scheduler.schedule()
            return StampResult.Queued
        }
        val result = sender.sendPending() ?: return StampResult.Queued
        if (result == StampResult.NetworkError) scheduler.schedule()
        return result
    }

    override suspend fun cancelPending() {
        scheduler.cancel()
        pendingDao.clear()
    }
}

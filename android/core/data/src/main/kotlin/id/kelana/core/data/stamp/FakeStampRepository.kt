package id.kelana.core.data.stamp

import id.kelana.core.model.PendingStamp
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRepository
import id.kelana.core.model.StampRequest
import id.kelana.core.model.StampResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for previews and ViewModel tests. [nextResult] is what [requestStamp] answers; a
 * [StampResult.Stamped] answer adds the stamp, [StampResult.Queued] fills [pending]. Holds no location.
 */
class FakeStampRepository(
    initialStamps: List<Stamp> = emptyList(),
    var nextResult: StampResult = StampResult.Stamped,
) : StampRepository {
    private val stampsState = MutableStateFlow(initialStamps)
    private val pendingState = MutableStateFlow<PendingStamp?>(null)

    override fun stamps(): Flow<List<Stamp>> = stampsState

    override fun pending(): Flow<PendingStamp?> = pendingState

    override suspend fun requestStamp(request: StampRequest): StampResult {
        val result = nextResult
        when (result) {
            StampResult.Stamped ->
                stampsState.value = stampsState.value + Stamp(request.placeId, request.capturedAt)
            StampResult.Queued, StampResult.NetworkError ->
                pendingState.value = PendingStamp(request.placeId, request.capturedAt)
            else -> Unit
        }
        return result
    }

    override suspend fun cancelPending() {
        pendingState.value = null
    }
}

package id.kelana.core.data.video

import id.kelana.core.model.VIDEO_MAX_SECONDS_PROVISIONAL
import id.kelana.core.model.VideoUploadFailure
import id.kelana.core.model.VideoUploadRepository
import id.kelana.core.model.VideoUploadRequest
import id.kelana.core.model.VideoUploadUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * FAKE: nothing is sent anywhere. It reports progress and then success, so the Unggah video screen can be built
 * before the video host (Cloudflare Stream or Mux) is chosen. [failWith] makes it end with that failure instead.
 * Bound by Hilt in VideoDataModule until a real resumable upload replaces it.
 */
class FakeVideoUploadRepository @Inject constructor() : VideoUploadRepository {
    var failWith: VideoUploadFailure? = null
    val requests = mutableListOf<VideoUploadRequest>()

    override fun upload(request: VideoUploadRequest): Flow<VideoUploadUpdate> = flow {
        requests += request
        if (request.durationSeconds > VIDEO_MAX_SECONDS_PROVISIONAL) {
            emit(VideoUploadUpdate.Failed(VideoUploadFailure.TooLong))
            return@flow
        }
        for (step in 1..STEPS) {
            delay(STEP_MILLIS)
            failWith?.let {
                emit(VideoUploadUpdate.Failed(it))
                return@flow
            }
            emit(VideoUploadUpdate.Progress(step / STEPS.toFloat()))
        }
        emit(VideoUploadUpdate.Done("fake-video"))
    }

    private companion object {
        const val STEPS = 5
        const val STEP_MILLIS = 400L
    }
}

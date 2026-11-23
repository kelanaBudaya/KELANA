package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/**
 * PROVISIONAL: longest video the app accepts, in seconds. The artifact (UnggahVideoIlustratif) shows `[ISI]`, so the
 * real limit is not decided. The video host (Cloudflare Stream or Mux) is not chosen either. Change only this value.
 */
const val VIDEO_MAX_SECONDS_PROVISIONAL: Int = 60

/** PROVISIONAL: longest caption, in characters (the artifact draws no counter). */
const val VIDEO_CAPTION_MAX: Int = 140

/**
 * What the Unggah video screen sends. [uri] is the content Uri of the video picked with the Photo Picker, as text.
 * [durationSeconds] is read on the device (MediaMetadataRetriever); the server must check it again.
 */
data class VideoUploadRequest(
    val uri: String,
    val placeId: String,
    val caption: String,
    val durationSeconds: Int,
)

/** Why an upload did not finish. */
enum class VideoUploadFailure {
    /** The account holds no stamp for this place (Postgres RLS refused it). */
    NotStamped,

    /** The video is longer than the limit. */
    TooLong,

    /** No signal or the host did not answer. Nothing was stored; the user can try again. */
    Network,

    /** Guest or expired session. */
    NeedsLogin,

    /** No video host is configured in this build. */
    NotConfigured,
}

/** What an upload reports over time. The flow ends after [Done] or [Failed]. */
sealed interface VideoUploadUpdate {
    /** [fraction] is 0.0 to 1.0. */
    data class Progress(val fraction: Float) : VideoUploadUpdate

    /** Stored and live at once (stories are not pre-moderated); [id] is the new `videos.id`. */
    data class Done(val id: String) : VideoUploadUpdate

    data class Failed(val reason: VideoUploadFailure) : VideoUploadUpdate
}

/**
 * Write side of visitor videos. The read side is [StoryRepository]. Shaped so a real resumable upload to the video
 * host can replace the fake one without changing callers. Cancelling the collector cancels the upload.
 */
interface VideoUploadRepository {
    fun upload(request: VideoUploadRequest): Flow<VideoUploadUpdate>
}

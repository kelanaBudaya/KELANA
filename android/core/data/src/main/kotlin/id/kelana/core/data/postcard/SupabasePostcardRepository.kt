package id.kelana.core.data.postcard

import id.kelana.core.model.POSTCARD_MESSAGE_MAX
import id.kelana.core.model.PostcardDraft
import id.kelana.core.model.PostcardPublishResult
import id.kelana.core.model.PostcardRepository
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes a postcard: photo to `postcards/<user_id>/<place_id>/<uuid>.jpg` (the bucket policy requires that
 * prefix), then the row. Guests get NeedsLogin. The stamp rule is enforced by Postgres RLS; a refusal
 * (HTTP 403) becomes NeedsStamp. The client check before this is only a hint.
 */
@Singleton
class SupabasePostcardRepository @Inject constructor(
    private val session: SessionRepository,
    private val remote: PostcardRemote,
) : PostcardRepository {

    override suspend fun publish(draft: PostcardDraft): PostcardPublishResult {
        val state = session.sessionState.first { it !is SessionState.Loading }
        if (state !is SessionState.SignedIn) return PostcardPublishResult.NeedsLogin
        if (!remote.isConfigured) return PostcardPublishResult.NotConfigured
        if (!isValid(draft)) return PostcardPublishResult.Invalid

        val userId = state.user.id.lowercase()
        val placeId = draft.placeId.lowercase()
        val id = UUID.randomUUID().toString()
        val photo = draft.photo
        val photoPath = photo?.let { "$userId/$placeId/${UUID.randomUUID()}.${extensionOf(it.mimeType)}" }
        return try {
            if (photo != null && photoPath != null) remote.upload(photoPath, photo.bytes, photo.mimeType)
            try {
                remote.insert(
                    PostcardInsertDto(
                        id = id,
                        userId = userId,
                        placeId = placeId,
                        message = draft.message.trim(),
                        photoPath = photoPath,
                        rating = draft.rating,
                        cardStyle = draft.style.wire,
                        isPublic = draft.isPublic,
                    ),
                )
            } catch (e: Exception) {
                if (photoPath != null) remote.deleteFile(photoPath)
                throw e
            }
            PostcardPublishResult.Published(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapError(e)
        }
    }

    private fun isValid(draft: PostcardDraft): Boolean {
        val length = draft.message.trim().length
        val photo = draft.photo
        return length in 1..POSTCARD_MESSAGE_MAX &&
            (draft.rating == null || draft.rating in 1..5) &&
            (photo == null || photo.bytes.size <= MAX_PHOTO_BYTES)
    }

    private fun extensionOf(mimeType: String): String = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    companion object {
        /** Bucket limit: 5 MB. */
        const val MAX_PHOTO_BYTES: Int = 5 * 1024 * 1024

        /** Error mapping, kept pure so it can be tested without a network. */
        internal fun mapError(e: Exception): PostcardPublishResult = when {
            e is PostcardHttpException -> when {
                e.status == 401 -> PostcardPublishResult.NeedsLogin
                e.status == 403 -> PostcardPublishResult.NeedsStamp
                e.status in 400..499 -> PostcardPublishResult.Invalid
                else -> PostcardPublishResult.NetworkError
            }
            else -> PostcardPublishResult.NetworkError
        }
    }
}

package id.kelana.core.data.postcard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server side of postcards: a Storage upload to the private bucket `postcards` and an insert into
 * `public.postcards`. Real implementation: [SupabasePostcardRemote]. Tests use a fake.
 *
 * Failures are thrown: [PostcardHttpException] when the server answered with an error status, anything else
 * (IOException, timeout) when there was no usable answer.
 */
interface PostcardRemote {
    /** False when the Supabase url or anon key is missing in this build. */
    val isConfigured: Boolean

    /** Stores [bytes] at [path] (`<user_id>/<place_id>/<file>`) without overwriting. */
    suspend fun upload(path: String, bytes: ByteArray, mimeType: String)

    suspend fun insert(row: PostcardInsertDto)

    /** Best effort clean-up of a file whose row could not be inserted. Never throws. */
    suspend fun deleteFile(path: String)
}

/** The server answered, but with an error. [status] is the HTTP status (RLS refusals are 403). */
class PostcardHttpException(val status: Int, message: String? = null) : Exception(message)

/** Row sent to `public.postcards`. Every field is always sent; the id is generated on the device. */
@Serializable
data class PostcardInsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("place_id") val placeId: String,
    val message: String,
    @SerialName("photo_path") val photoPath: String?,
    val rating: Int?,
    @SerialName("card_style") val cardStyle: String,
    @SerialName("is_public") val isPublic: Boolean,
)

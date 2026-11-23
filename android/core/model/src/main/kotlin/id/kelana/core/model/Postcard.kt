package id.kelana.core.model

/** Card style picked on Tulis postcard (7.1, "Gaya kartu"). [wire] is the `postcards.card_style` value. */
enum class PostcardStyle(val wire: String) {
    Jurnal("jurnal"),
    Cap("cap"),
    Peta("peta"),
    ;

    companion object {
        fun fromWire(value: String?): PostcardStyle? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * A photo picked for a postcard, already read and compressed by the feature (JPEG, WebP or PNG; the bucket
 * accepts at most 5 MB). [toString] hides the bytes so they never end up in logs.
 */
class PostcardPhoto(val bytes: ByteArray, val mimeType: String) {
    override fun toString(): String = "PostcardPhoto(${bytes.size} bytes, $mimeType)"
}

/**
 * What Tulis postcard sends. [message] is 1 to [POSTCARD_MESSAGE_MAX] characters after trimming.
 * [rating] is 1 to 5 or null (never defaulted). [isPublic] is the "Tampilkan di halaman <tempat>" switch:
 * false keeps the postcard visible to its author only (it can still be shared outside the app).
 */
data class PostcardDraft(
    val placeId: String,
    val message: String,
    val photo: PostcardPhoto?,
    val rating: Int?,
    val style: PostcardStyle,
    val isPublic: Boolean,
)

/** Matches the `postcards.message` check constraint. */
const val POSTCARD_MESSAGE_MAX: Int = 500

sealed interface PostcardPublishResult {
    /** Stored; [id] is the new `postcards.id`. */
    data class Published(val id: String) : PostcardPublishResult

    /** Guest: posting needs an account. */
    data object NeedsLogin : PostcardPublishResult

    /** The account holds no stamp for this place (Postgres RLS refused the insert). */
    data object NeedsStamp : PostcardPublishResult

    /** The draft breaks a rule the server also checks (empty or long message, rating out of range, photo too big). */
    data object Invalid : PostcardPublishResult

    /** No signal or the server did not answer. Nothing was stored; the user can try again. */
    data object NetworkError : PostcardPublishResult

    /** Supabase is not configured in this build. */
    data object NotConfigured : PostcardPublishResult
}

/** Write side of postcards. The read side is [StoryRepository]. */
interface PostcardRepository {
    /** Uploads the photo (if any) to `postcards/<user_id>/<place_id>/<file>`, then inserts the row. */
    suspend fun publish(draft: PostcardDraft): PostcardPublishResult
}

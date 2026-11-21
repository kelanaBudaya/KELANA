package id.kelana.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * A visitor story: a postcard or a video, posted by an account that holds a stamp for [placeId].
 * Stories go live immediately (no pre-moderation). Only signed-in users may read them.
 */
sealed interface Story {
    val id: String
    val placeId: String
    val placeName: String

    /** Display name of the author. Blank when the profile has none; the UI then shows a generic label. */
    val authorName: String
    val createdAt: Instant

    /** Stamp code of the place (for example "YK-02"), when the server provides it. */
    val stampCode: String?
}

/**
 * A photo postcard. [photoPath] is the object path in the private `postcards` bucket (needs a signed URL),
 * not a public URL. [rating] is 1 to 5 when the author gave stars, otherwise null (never invented).
 */
data class PostcardStory(
    override val id: String,
    override val placeId: String,
    override val placeName: String,
    override val authorName: String,
    override val createdAt: Instant,
    override val stampCode: String? = null,
    val photoPath: String? = null,
    val message: String? = null,
    val rating: Int? = null,
    val visitedOn: LocalDate? = null,
) : Story

/** A visitor video. [videoPath] and [thumbnailPath] are storage paths, resolved to playable URLs by the data layer. */
data class VideoStory(
    override val id: String,
    override val placeId: String,
    override val placeName: String,
    override val authorName: String,
    override val createdAt: Instant,
    override val stampCode: String? = null,
    val videoPath: String? = null,
    val thumbnailPath: String? = null,
    val caption: String? = null,
    val durationSeconds: Int? = null,
) : Story

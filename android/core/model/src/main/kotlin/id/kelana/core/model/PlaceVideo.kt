package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/**
 * A curated visitor video of a place, hosted on TikTok and embedded in Detail tempat.
 * [tiktokVideoId] is digits only; [tiktokUrl] is the public link used as the "Buka di TikTok" fallback.
 */
data class PlaceVideo(
    val id: String,
    val placeId: String,
    val tiktokUrl: String,
    val tiktokVideoId: String,
    val caption: String?,
)

interface PlaceVideoRepository {
    /**
     * Published videos of [placeId] in display order. Empty when there are none. Guests may read.
     * A failing flow (network, not configured) must be shown as a local error, never break the screen.
     */
    fun videos(placeId: String): Flow<List<PlaceVideo>>
}

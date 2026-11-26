package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/**
 * An open-licensed photo of a place (Wikimedia Commons CC0/PD/CC BY/CC BY-SA), shown in the gallery.
 * [url] is the full image, [thumbUrl] the small one. [attributionText] is always shown with the photo;
 * [sourcePageUrl] is the Commons page ("Lihat sumber").
 */
data class PlacePhoto(
    val id: String,
    val placeId: String,
    val url: String,
    val thumbUrl: String,
    val width: Int,
    val height: Int,
    val caption: String?,
    val attributionText: String,
    val licenseCode: String,
    val licenseUrl: String,
    val sourcePageUrl: String,
)

interface PlacePhotoRepository {
    /**
     * Published photos of [placeId] in display order. Empty when there are none. Guests may read.
     * A failing flow (network, not configured) must be shown as a local error, never break the screen.
     */
    fun photos(placeId: String): Flow<List<PlacePhoto>>
}

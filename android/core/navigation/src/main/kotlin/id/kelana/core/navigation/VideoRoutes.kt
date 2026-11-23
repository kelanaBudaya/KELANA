package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/** Unggah video (7.3), opened from the "Video" button on Tulis postcard ([PostcardRoute]). Needs a signed-in user with a stamp for [placeId]. */
@Serializable data class UnggahVideoRoute(val placeId: String)

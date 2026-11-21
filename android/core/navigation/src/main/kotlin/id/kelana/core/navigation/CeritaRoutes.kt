package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/** Detail of a visitor postcard (Cerita tab). Needs a signed-in user. */
@Serializable data class CeritaPostcardRoute(val storyId: String)

/** Detail of a visitor video (Cerita tab). Needs a signed-in user. */
@Serializable data class CeritaVideoRoute(val storyId: String)

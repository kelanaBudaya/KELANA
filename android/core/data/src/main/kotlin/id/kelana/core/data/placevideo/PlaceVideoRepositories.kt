package id.kelana.core.data.placevideo

import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.data.remote.NotConfiguredException
import id.kelana.core.model.PlaceVideo
import id.kelana.core.model.PlaceVideoRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PlaceVideoDto(
    val id: String,
    @SerialName("place_id") val placeId: String,
    @SerialName("tiktok_url") val tiktokUrl: String,
    @SerialName("tiktok_video_id") val tiktokVideoId: String,
    val caption: String? = null,
)

/** Remote read of `place_videos`. RLS returns published rows only; no write path exists in the app. */
interface PlaceVideoRemote {
    suspend fun fetch(placeId: String): List<PlaceVideoDto>
}

@Singleton
class SupabasePlaceVideoRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : PlaceVideoRemote {
    override suspend fun fetch(placeId: String): List<PlaceVideoDto> {
        val client = provider.client ?: throw NotConfiguredException()
        return client.from("place_videos")
            .select(Columns.raw("id,place_id,tiktok_url,tiktok_video_id,caption")) {
                filter { eq("place_id", placeId) }
                order("sort_order", Order.ASCENDING)
                order("created_at", Order.ASCENDING)
            }
            .decodeList<PlaceVideoDto>()
    }
}

private val DIGITS = Regex("^[0-9]+$")

/** Drops rows the embed could not use safely: non-numeric id, or a link that is not https. */
internal fun PlaceVideoDto.toModelOrNull(): PlaceVideo? {
    if (!DIGITS.matches(tiktokVideoId) || !tiktokUrl.startsWith("https://")) return null
    return PlaceVideo(id, placeId, tiktokUrl, tiktokVideoId, caption?.takeIf { it.isNotBlank() })
}

/** One request per call; exceptions propagate to the collector so the UI shows an error state for this section only. */
@Singleton
class SupabasePlaceVideoRepository @Inject constructor(
    private val remote: PlaceVideoRemote,
) : PlaceVideoRepository {
    override fun videos(placeId: String): Flow<List<PlaceVideo>> = flow {
        emit(remote.fetch(placeId).mapNotNull { it.toModelOrNull() })
    }
}

/** In-memory stand-in for previews and tests; not bound by Hilt. */
class FakePlaceVideoRepository(initial: List<PlaceVideo> = emptyList()) : PlaceVideoRepository {
    private val all = MutableStateFlow(initial)
    override fun videos(placeId: String): Flow<List<PlaceVideo>> = all.map { list -> list.filter { it.placeId == placeId } }
}

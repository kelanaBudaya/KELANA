package id.kelana.core.data.placephoto

import id.kelana.core.data.BuildConfig
import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.data.remote.NotConfiguredException
import id.kelana.core.model.PlaceCoverRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Only the columns needed to pick a cover; same validity columns as [PlacePhotoDto]. */
@Serializable
data class PlaceCoverRowDto(
    @SerialName("place_id") val placeId: String,
    @SerialName("thumb_path") val thumbPath: String,
    @SerialName("attribution_text") val attributionText: String,
    @SerialName("license_url") val licenseUrl: String,
    @SerialName("source_page_url") val sourcePageUrl: String,
)

/** One request for all published photos' cover columns (a few rows, not full galleries). */
interface PlaceCoverRemote {
    /** Rows already ordered by `sort_order`, then `created_at`. */
    suspend fun fetchRows(): List<PlaceCoverRowDto>
}

@Singleton
class SupabasePlaceCoverRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : PlaceCoverRemote {
    override suspend fun fetchRows(): List<PlaceCoverRowDto> {
        val client = provider.client ?: throw NotConfiguredException()
        return client.from("place_photos")
            .select(Columns.raw("place_id,thumb_path,attribution_text,license_url,source_page_url")) {
                filter { eq("is_published", true) }
                order("sort_order", Order.ASCENDING)
                order("created_at", Order.ASCENDING)
            }
            .decodeList<PlaceCoverRowDto>()
    }
}

/**
 * First usable row per place, in the order given. Rows the gallery would drop (no attribution, non-https link,
 * blank path) are skipped, so a card never shows a photo the detail gallery would not.
 */
internal fun selectCovers(rows: List<PlaceCoverRowDto>, supabaseUrl: String): Map<String, String> {
    if (supabaseUrl.isBlank()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (row in rows) {
        if (row.placeId in out) continue
        if (row.attributionText.isBlank() || row.thumbPath.isBlank()) continue
        if (!row.sourcePageUrl.startsWith("https://") || !row.licenseUrl.startsWith("https://")) continue
        val url = photoPublicUrl(supabaseUrl, row.thumbPath)
        if (url.startsWith("https://")) out[row.placeId] = url
    }
    return out
}

/** In-memory only (no Room): images are cached by Coil, and the map is cheap to refetch (one small request). */
@Singleton
class SupabasePlaceCoverRepository internal constructor(
    private val remote: PlaceCoverRemote,
    private val supabaseUrl: String,
) : PlaceCoverRepository {
    @Inject
    constructor(remote: PlaceCoverRemote) : this(remote, BuildConfig.SUPABASE_URL)

    private val cache = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun covers(): Flow<Map<String, String>> = flow {
        emit(cache.value)
        try {
            val fresh = selectCovers(remote.fetchRows(), supabaseUrl)
            cache.value = fresh
            emit(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or not configured: cards keep their illustrations.
        }
    }
}

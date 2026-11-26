package id.kelana.core.data.placephoto

import id.kelana.core.data.BuildConfig
import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.data.auth.normalizeSupabaseUrl
import id.kelana.core.data.remote.NotConfiguredException
import id.kelana.core.data.repository.RefreshGate
import id.kelana.core.database.dao.PlacePhotoDao
import id.kelana.core.database.entity.PlacePhotoEntity
import id.kelana.core.model.PlacePhoto
import id.kelana.core.model.PlacePhotoRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PlacePhotoDto(
    val id: String,
    @SerialName("place_id") val placeId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("thumb_path") val thumbPath: String,
    val width: Int,
    val height: Int,
    val caption: String? = null,
    @SerialName("attribution_text") val attributionText: String,
    @SerialName("license_code") val licenseCode: String,
    @SerialName("license_url") val licenseUrl: String,
    @SerialName("source_page_url") val sourcePageUrl: String,
)

/** Remote read of `place_photos`. RLS returns published rows only; no write path exists in the app. */
interface PlacePhotoRemote {
    suspend fun fetch(placeId: String): List<PlacePhotoDto>
}

@Singleton
class SupabasePlacePhotoRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : PlacePhotoRemote {
    override suspend fun fetch(placeId: String): List<PlacePhotoDto> {
        val client = provider.client ?: throw NotConfiguredException()
        return client.from("place_photos")
            .select(
                Columns.raw(
                    "id,place_id,storage_path,thumb_path,width,height,caption,attribution_text,license_code,license_url,source_page_url",
                ),
            ) {
                filter {
                    eq("place_id", placeId)
                    eq("is_published", true)
                }
                order("sort_order", Order.ASCENDING)
                order("created_at", Order.ASCENDING)
            }
            .decodeList<PlacePhotoDto>()
    }
}

private const val BUCKET_PATH = "/storage/v1/object/public/place-photos/"

/** Public URL of an object in the `place-photos` bucket. */
internal fun photoPublicUrl(supabaseUrl: String, path: String): String =
    normalizeSupabaseUrl(supabaseUrl) + BUCKET_PATH + path.trimStart('/')

/** Drops rows that must not be shown: no attribution, or a link that is not https. */
internal fun PlacePhotoDto.toModelOrNull(supabaseUrl: String): PlacePhoto? {
    if (attributionText.isBlank() || supabaseUrl.isBlank()) return null
    if (!sourcePageUrl.startsWith("https://") || !licenseUrl.startsWith("https://")) return null
    if (storagePath.isBlank() || thumbPath.isBlank()) return null
    val full = photoPublicUrl(supabaseUrl, storagePath)
    val thumb = photoPublicUrl(supabaseUrl, thumbPath)
    if (!full.startsWith("https://") || !thumb.startsWith("https://")) return null
    return PlacePhoto(
        id = id,
        placeId = placeId,
        url = full,
        thumbUrl = thumb,
        width = width,
        height = height,
        caption = caption?.takeIf { it.isNotBlank() },
        attributionText = attributionText.trim(),
        licenseCode = licenseCode,
        licenseUrl = licenseUrl,
        sourcePageUrl = sourcePageUrl,
    )
}

/**
 * Cache-then-network over the Room photo cache: emits the cached list for the place first (if any), then the fresh
 * one after a refresh that replaces the place's rows in one transaction. A failed refresh never touches the cache:
 * with cached rows the collector just keeps them; with none the exception reaches the collector so the gallery
 * shows an error state. Image bytes are cached by the app-wide Coil loader.
 */
@Singleton
class SupabasePlacePhotoRepository internal constructor(
    private val remote: PlacePhotoRemote,
    private val dao: PlacePhotoDao,
    private val supabaseUrl: String,
    private val minIntervalMs: Long,
) : PlacePhotoRepository {
    @Inject
    constructor(remote: PlacePhotoRemote, dao: PlacePhotoDao) :
        this(remote, dao, BuildConfig.SUPABASE_URL, RefreshGate.DEFAULT_MIN_INTERVAL_MS)

    private val gates = ConcurrentHashMap<String, RefreshGate>()

    override fun photos(placeId: String): Flow<List<PlacePhoto>> = flow {
        val gate = gates.getOrPut(placeId) { RefreshGate(minIntervalMs) }
        val cached = dao.get(placeId)
        if (cached.isNotEmpty()) emit(cached.map { it.toModel() })
        var ran = false
        var failure: Exception? = null
        gate.run {
            ran = true
            try {
                // A blank base URL means "not configured": every row would be dropped, so keep the cache as is.
                if (supabaseUrl.isNotBlank()) {
                    val rows = remote.fetch(placeId).mapNotNull { it.toModelOrNull(supabaseUrl) }
                    dao.replaceForPlace(placeId, rows.mapIndexed { i, p -> p.toEntity(i) })
                }
            } catch (e: Exception) {
                failure = e
                throw e
            }
        }
        val error = failure
        if (error != null) {
            if (cached.isEmpty()) throw error
            return@flow
        }
        // Skipped by the gate with a cache already shown: nothing new to say.
        if (!ran && cached.isNotEmpty()) return@flow
        emit(dao.get(placeId).map { it.toModel() })
    }
}

private fun PlacePhoto.toEntity(position: Int) = PlacePhotoEntity(
    id = id, placeId = placeId, position = position, url = url, thumbUrl = thumbUrl, width = width, height = height,
    caption = caption, attributionText = attributionText, licenseCode = licenseCode, licenseUrl = licenseUrl,
    sourcePageUrl = sourcePageUrl,
)

private fun PlacePhotoEntity.toModel() = PlacePhoto(
    id = id, placeId = placeId, url = url, thumbUrl = thumbUrl, width = width, height = height, caption = caption,
    attributionText = attributionText, licenseCode = licenseCode, licenseUrl = licenseUrl, sourcePageUrl = sourcePageUrl,
)

/** In-memory stand-in for previews and tests; not bound by Hilt. Sample photos use no network by default. */
class FakePlacePhotoRepository(initial: List<PlacePhoto> = samplePhotos("p1")) : PlacePhotoRepository {
    private val all = MutableStateFlow(initial)
    override fun photos(placeId: String): Flow<List<PlacePhoto>> = all.map { list -> list.filter { it.placeId == placeId } }

    companion object {
        fun samplePhotos(placeId: String): List<PlacePhoto> = (1..4).map { i ->
            PlacePhoto(
                id = "$placeId-$i",
                placeId = placeId,
                url = "https://example.invalid/photos/$placeId/$i.jpg",
                thumbUrl = "https://example.invalid/photos/$placeId/${i}_t.jpg",
                width = 1600,
                height = 1067,
                caption = if (i == 1) "Tampak depan" else null,
                attributionText = "Foto: Contoh $i · CC BY-SA 4.0 · Wikimedia Commons",
                licenseCode = "CC-BY-SA-4.0",
                licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
                sourcePageUrl = "https://commons.wikimedia.org/wiki/File:Contoh$i.jpg",
            )
        }
    }
}

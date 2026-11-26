package id.kelana.core.data.route

import id.kelana.core.model.RouteLeg
import id.kelana.core.model.RoutePoint
import id.kelana.core.model.RouteRepository
import id.kelana.core.model.TravelMode
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/** Server profile for a travel mode. Buses follow roads like cars; ride-hailing is a motorcycle. */
internal fun profileOf(mode: TravelMode): String = when (mode) {
    TravelMode.Car, TravelMode.Transit -> "car"
    TravelMode.RideHailing -> "motorcycle"
    TravelMode.Walk -> "walk"
}

/** Server limit per request. */
internal const val MAX_POINTS_PER_REQUEST = 10

/**
 * Splits [size] points into inclusive index ranges of at most [maxPoints] points where each chunk starts on the last
 * point of the previous one, so the legs of all chunks together cover every consecutive pair exactly once.
 */
internal fun chunkRanges(size: Int, maxPoints: Int = MAX_POINTS_PER_REQUEST): List<IntRange> {
    if (size < 2) return emptyList()
    val out = ArrayList<IntRange>()
    var start = 0
    while (start < size - 1) {
        val end = minOf(start + maxPoints - 1, size - 1)
        out += start..end
        start = end
    }
    return out
}

/**
 * Road routing on top of the `route` Edge Function, with an in-memory LRU cache keyed by (profile, rounded
 * coordinates) so reopening the screen does not refetch. Any failure returns null.
 */
@Singleton
class SupabaseRouteRepository @Inject constructor(
    private val remote: RouteRemote,
) : RouteRepository {

    private val cache = object : LinkedHashMap<String, List<RouteLeg>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<RouteLeg>>?) = size > CACHE_SIZE
    }

    override suspend fun route(mode: TravelMode, points: List<RoutePoint>): List<RouteLeg>? {
        if (points.size < 2) return null
        val profile = profileOf(mode)
        val legs = ArrayList<RouteLeg>(points.size - 1)
        for (range in chunkRanges(points.size)) {
            val chunk = points.subList(range.first, range.last + 1)
            legs += fetchChunk(profile, chunk) ?: return null
        }
        return legs
    }

    private suspend fun fetchChunk(profile: String, chunk: List<RoutePoint>): List<RouteLeg>? {
        val key = cacheKey(profile, chunk)
        synchronized(cache) { cache[key] }?.let { return it }
        val response = try {
            remote.fetch(profile, chunk)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
        if (response.legs.size != chunk.size - 1) return null
        val legs = response.legs.map { RouteLeg(it.seconds, it.meters, decodePolyline(it.polyline)) }
        if (legs.any { it.path.size < 2 }) return null
        synchronized(cache) { cache[key] = legs }
        return legs
    }

    private fun cacheKey(profile: String, points: List<RoutePoint>): String =
        profile + points.joinToString("|") { "${(it.lat * 1e5).roundToLong()},${(it.lng * 1e5).roundToLong()}" }

    private companion object {
        const val CACHE_SIZE = 32
    }
}

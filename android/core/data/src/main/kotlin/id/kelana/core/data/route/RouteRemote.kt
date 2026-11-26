package id.kelana.core.data.route

import id.kelana.core.model.RoutePoint
import kotlinx.serialization.Serializable

/** Server side of routing (Edge Function `route`). Real implementation: [SupabaseRouteRemote]. Tests use a fake. */
interface RouteRemote {
    /** The decoded response, or null on any failure (not configured, offline, 400, 429, 502, unreadable body). */
    suspend fun fetch(profile: String, points: List<RoutePoint>): RouteResponseDto?
}

@Serializable
data class RoutePointDto(val lat: Double, val lng: Double)

@Serializable
data class RouteRequestDto(val profile: String, val points: List<RoutePointDto>)

@Serializable
data class RouteLegDto(val seconds: Int, val meters: Int, val polyline: String)

@Serializable
data class RouteResponseDto(val legs: List<RouteLegDto>, val source: String? = null)

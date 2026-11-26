package id.kelana.core.model

/** A public place coordinate on a route. Never the user's own location. */
data class RoutePoint(val lat: Double, val lng: Double)

/** One stretch between two consecutive points, following roads: [seconds], [meters] and the drawn [path]. */
data class RouteLeg(val seconds: Int, val meters: Int, val path: List<RoutePoint>)

/** Road routing for Peta rute. */
interface RouteRepository {
    /**
     * One leg per consecutive pair of [points], or null on any failure (offline, throttled, not configured).
     * Never throws except for cancellation. Callers keep a straight line when this returns null.
     */
    suspend fun route(mode: TravelMode, points: List<RoutePoint>): List<RouteLeg>?
}

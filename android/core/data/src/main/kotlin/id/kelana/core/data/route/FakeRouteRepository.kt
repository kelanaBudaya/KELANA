package id.kelana.core.data.route

import id.kelana.core.model.RouteLeg
import id.kelana.core.model.RoutePoint
import id.kelana.core.model.RouteRepository
import id.kelana.core.model.TravelMode

/** For previews and offline builds: no road data, so the caller keeps its straight line. */
class FakeRouteRepository : RouteRepository {
    override suspend fun route(mode: TravelMode, points: List<RoutePoint>): List<RouteLeg>? = null
}

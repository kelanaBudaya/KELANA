package id.kelana.core.ui.map

/*
 * Map tile configuration. Everything that names a tile provider lives here, so the provider can be swapped in one
 * place. POLICY WARNING: tile.openstreetmap.org is a volunteer-run server; its usage policy forbids heavy or
 * commercial app traffic and requires an identifying User-Agent. Before public launch, switch [TILE_URL_TEMPLATE] to a
 * commercial tile provider or a self-hosted tile server.
 */

/** Raster tile URL template ({z}/{x}/{y}). The single place to change the provider. */
internal const val TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

internal const val TILE_SIZE_PX = 256
internal const val TILE_MAX_ZOOM = 19

/** Attribution required by the OpenStreetMap licence; shown with every map (see [MapAttribution]). */
internal const val ATTRIBUTION_URL = "https://www.openstreetmap.org/copyright"

/** Identifies the app to the tile server, as the OSM tile usage policy requires. */
internal const val TILE_USER_AGENT = "KelanaApp/1.0 (id.kelana.app; Android)"

/** MapLibre style JSON with one raster layer over [TILE_URL_TEMPLATE]. No glyphs or sprites are needed. */
internal fun osmStyleJson(): String =
    """{"version":8,"sources":{"osm":{"type":"raster","tiles":["$TILE_URL_TEMPLATE"],"tileSize":$TILE_SIZE_PX,"maxzoom":$TILE_MAX_ZOOM}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""

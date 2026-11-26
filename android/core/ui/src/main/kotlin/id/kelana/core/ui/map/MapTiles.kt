package id.kelana.core.ui.map

import android.content.Context
import id.kelana.core.ui.R

/*
 * Basemap configuration. The basemap is the OpenFreeMap "positron" vector style, bundled as res/raw/map_style_muted.json
 * (sprite, glyphs and the vector source stay absolute https://tiles.openfreemap.org URLs). Only paint/visibility were edited.
 *
 * POLICY: OpenFreeMap is free, keyless and best-effort, with NO SLA. It asks clients to identify themselves
 * ([TILE_USER_AGENT]) and to show attribution (see MapAttribution). Fallback plan if it becomes unreliable or the
 * traffic grows: serve a self-hosted Protomaps PMTiles extract of Java and point the style's "openmaptiles" source at it.
 *
 * Muting decisions (so our pins and the route line stand out; brand blue #1F3F8F, accent orange, cream #FFFBF3):
 *  - background #F6F1E7 warm very pale land (a touch darker than the app cream so cards still lift off it);
 *    residential/aeroway areas #F2EDE2.
 *  - water #D2DBE2 pale desaturated blue-grey (waterways the same); water names #7F90A5.
 *  - parks #E4E8DB and woods #E0E5D8: low-contrast pale green-grey.
 *  - roads: casings #E4DECF, fills #FBF8F1, minor roads #EAE4D6 thinner and only from zoom 14 (was 8, width reduced).
 *  - buildings #EEE8DB at 0.6 opacity with a same-colour outline: barely visible.
 *  - hidden: footpaths, all railways, road shields, minor/path road names, airport label, "other" and village labels,
 *    ice-cap landcover (irrelevant at our zooms). Kept: city/town/state/country names (#6A645A, cream halo),
 *    major road names (#8C867A) and major water names.
 *  - boundaries lightened to #CFC8B8.
 */

/** The OpenStreetMap copyright page, opened by the attribution (ODbL credit). OpenFreeMap is credited in the text. */
internal const val ATTRIBUTION_URL = "https://www.openstreetmap.org/copyright"

/** Identifies the app to the tile server (OpenFreeMap asks for an identifying User-Agent). */
internal const val TILE_USER_AGENT = "KelanaApp/1.0 (id.kelana.app; Android)"

/** The bundled muted MapLibre style as a JSON string, for `Style.Builder().fromJson(...)`. */
internal fun mutedStyleJson(context: Context): String =
    context.resources.openRawResource(R.raw.map_style_muted).bufferedReader().use { it.readText() }

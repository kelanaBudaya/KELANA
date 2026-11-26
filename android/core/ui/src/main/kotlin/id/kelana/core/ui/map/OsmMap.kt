package id.kelana.core.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil

/** Distance from the top of the pin column to the drop's tip (rotated square: centre + half diagonal). */
private val TipOffset = PinTopInset + DropSize / 2 + DropSize * 0.7071f

private const val MAP_PADDING_TOP_PX = 220
private const val MAP_PADDING_SIDE_PX = 100
private const val MAP_PADDING_BOTTOM_PX = 560

/**
 * The OpenStreetMap view (MapLibre, bundled muted OpenFreeMap style, see [mutedStyleJson]) with one pin per marker, shared by every
 * screen that shows a map. Pins are Compose nodes laid over the map and repositioned from the map projection whenever
 * the camera moves, so they keep the design's marker look and accessibility semantics. The camera frames
 * [framedPoints] (lat, lng pairs) once. Markers are tappable only when [onMarkerTap] is given. When [routePath] (lat, lng
 * points, any number) has two or more points, a [routeColor] line follows it, under the pins. No location component is used:
 * the map shows places only (location is read only from "Ambil cap"). Show a [MapAttribution] next to it.
 */
@Composable
fun OsmMap(
    markers: List<MapMarker>,
    framedPoints: List<Pair<Double, Double>>,
    selectedKey: String?,
    modifier: Modifier = Modifier,
    onMarkerTap: ((String) -> Unit)? = null,
    routePath: List<Pair<Double, Double>> = emptyList(),
    routeColor: Color = Color.Unspecified,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        installUserAgent()
        MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var screenPoints by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var fitted by remember { mutableStateOf(false) }
    var routeScreenPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val currentMarkers by rememberUpdatedState(markers)
    val currentRoute by rememberUpdatedState(thinPath(routePath))

    fun refreshPoints() {
        val m = map ?: return
        screenPoints = currentMarkers.associate {
            val pt = m.projection.toScreenLocation(LatLng(it.lat, it.lng))
            it.key to Offset(pt.x, pt.y)
        }
        routeScreenPoints = currentRoute.map {
            val pt = m.projection.toScreenLocation(LatLng(it.first, it.second))
            Offset(pt.x, pt.y)
        }
    }

    // The MapView follows the host lifecycle and is torn down when this composable leaves the composition.
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            val state = lifecycle.currentState
            if (state != Lifecycle.State.DESTROYED) {
                if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isAttributionEnabled = false // replaced by our own visible text, see MapAttribution
                isLogoEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            m.setMinZoomPreference(8.0)
            m.setMaxZoomPreference(18.0)
            m.setStyle(Style.Builder().fromJson(mutedStyleJson(context)))
            m.addOnCameraMoveListener { refreshPoints() }
            m.addOnCameraIdleListener { refreshPoints() }
            map = m
        }
    }

    // Frame the points once, when the map and the points are both ready.
    LaunchedEffect(map, framedPoints.isNotEmpty()) {
        val m = map ?: return@LaunchedEffect
        if (fitted || framedPoints.isEmpty()) return@LaunchedEffect
        val bounds = boundsOf(framedPoints) ?: return@LaunchedEffect
        val update = if (bounds.isPoint()) {
            CameraUpdateFactory.newLatLngZoom(LatLng(bounds.centerLat, bounds.centerLng), SINGLE_PLACE_ZOOM)
        } else {
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder()
                    .include(LatLng(bounds.north, bounds.east))
                    .include(LatLng(bounds.south, bounds.west))
                    .build(),
                MAP_PADDING_SIDE_PX, MAP_PADDING_TOP_PX, MAP_PADDING_SIDE_PX, MAP_PADDING_BOTTOM_PX,
            )
        }
        m.moveCamera(update)
        fitted = true
        refreshPoints()
    }
    LaunchedEffect(map, markers, routePath) { refreshPoints() }

    Box(modifier.fillMaxSize().clipToBounds()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (routeScreenPoints.size >= 2) {
            Canvas(Modifier.fillMaxSize()) {
                val path = Path()
                routeScreenPoints.forEachIndexed { i, pt ->
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, routeColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        // The selected pin is drawn last so it stays on top of its neighbours.
        for (marker in markers.sortedBy { it.key == selectedKey }) {
            val pt = screenPoints[marker.key] ?: continue
            with(density) {
                MapPin(
                    marker = marker,
                    selected = marker.key == selectedKey,
                    onClick = onMarkerTap?.let { tap -> { tap(marker.key) } },
                    modifier = Modifier.offset(x = pt.x.toDp() - PinWidth / 2, y = pt.y.toDp() - TipOffset),
                )
            }
        }
    }
}

/** One OkHttp client for tile requests, carrying the identifying User-Agent OpenFreeMap asks for. */
private fun installUserAgent() {
    HttpRequestUtil.setOkHttpClient(
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", TILE_USER_AGENT).build())
            }
            .build(),
    )
}

private const val MAX_ROUTE_POINTS = 2000

/** Keeps every n-th point (and always the last) when a path is very long, so projecting it stays cheap. */
internal fun thinPath(path: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
    if (path.size <= MAX_ROUTE_POINTS) return path
    val step = (path.size + MAX_ROUTE_POINTS - 1) / MAX_ROUTE_POINTS
    val out = path.filterIndexed { i, _ -> i % step == 0 }.toMutableList()
    if (out.last() != path.last()) out += path.last()
    return out
}

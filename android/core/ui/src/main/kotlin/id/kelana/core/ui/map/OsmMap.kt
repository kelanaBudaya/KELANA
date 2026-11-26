package id.kelana.core.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
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
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil

/** Distance from the top of the pin column to the drop's tip (rotated square: centre + half diagonal). */
private val TipOffset = PinTopInset + DropSize / 2 + DropSize * 0.7071f

/** Side margin kept free of pins when framing. */
private val FRAME_SIDE_MARGIN = 24.dp

/** Default overlay room for screens that give no [contentPadding] (top bar, bottom card). */
private val DefaultContentPadding = PaddingValues(top = 80.dp, bottom = 200.dp)

/** Room a pin takes around its tip: the label hangs below it, the selected pin is scaled 1.12. */
private val PinBelowTip = 24.dp

/**
 * Lowest zoom. Must be low enough to fit the widest region on a phone: Jawa Timur needs about 6.7 and Jawa Tengah 6.4, so
 * a minimum of 8 (the old value) clamped the camera too close and pushed the outer pins off screen.
 */
private const val MIN_ZOOM = 5.0
internal const val MAX_ZOOM = 18.0
private const val CLUSTER_ANIM_MS = 350

/** Height of the box a pin occupies for overlap tests (the pin's visible height, label included). */
internal val PinCollisionHeight = 72.dp

/**
 * The OpenStreetMap view (MapLibre, bundled muted OpenFreeMap style, see [mutedStyleJson]) with one pin per marker, shared by every
 * screen that shows a map. Pins are Compose nodes laid over the map and repositioned from the map projection whenever
 * the camera moves, so they keep the design's marker look and accessibility semantics. The camera frames
 * [framedPoints] (lat, lng pairs) once, so that every pin (label included) is inside the view and clear of [contentPadding]
 * (the room taken by overlays such as the top chips and the bottom card), measured from the map's own size. Markers are tappable only when [onMarkerTap] is given. When [routePath] (lat, lng
 * points, any number) has two or more points, a [routeColor] line follows it, under the pins. Pins whose boxes overlap are fanned out with leader lines ([MarkerOverlap.Spread], the default) or merged into tappable count bubbles ([MarkerOverlap.Cluster]). No location component is used:
 * the map shows places only (location is read only from "Ambil cap"). The credit is shown by [MapCreditDialog], reached from Profil.
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
    overlap: MarkerOverlap = MarkerOverlap.Spread,
    contentPadding: PaddingValues = DefaultContentPadding,
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
    var zoom by remember { mutableStateOf(0.0) }
    var sheetMembers by remember { mutableStateOf<List<MapMarker>?>(null) }
    var fitted by remember { mutableStateOf(false) }
    var routeScreenPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val currentPadding by rememberUpdatedState(contentPadding)
    val currentMarkers by rememberUpdatedState(markers)

    /** Camera that fits [points] in the measured map view, or null while it has no size yet. */
    fun fitFor(points: List<Pair<Double, Double>>): CameraFit? {
        if (viewSize.width <= 0 || viewSize.height <= 0) return null
        val pad = currentPadding
        val viewport = with(density) {
            FramingViewport(
                widthDp = viewSize.width.toDp().value.toDouble(),
                heightDp = viewSize.height.toDp().value.toDouble(),
                padLeftDp = (pad.calculateStartPadding(LayoutDirection.Ltr).coerceAtLeast(0.dp) + FRAME_SIDE_MARGIN).value.toDouble(),
                padTopDp = pad.calculateTopPadding().value.toDouble(),
                padRightDp = (pad.calculateEndPadding(LayoutDirection.Ltr).coerceAtLeast(0.dp) + FRAME_SIDE_MARGIN).value.toDouble(),
                padBottomDp = pad.calculateBottomPadding().value.toDouble(),
                pinHalfWidthDp = (PinWidth / 2).value.toDouble(),
                pinAboveDp = (TipOffset + 4.dp).value.toDouble(),
                pinBelowDp = PinBelowTip.value.toDouble(),
            )
        }
        return fitCamera(points, viewport, MIN_ZOOM, MAX_ZOOM)
    }
    val currentRoute by rememberUpdatedState(thinPath(routePath))

    fun refreshPoints() {
        val m = map ?: return
        zoom = m.cameraPosition.zoom
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
                isAttributionEnabled = false // the credit is in MapCreditDialog
                isLogoEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            m.setMinZoomPreference(MIN_ZOOM)
            m.setMaxZoomPreference(MAX_ZOOM)
            m.setStyle(Style.Builder().fromJson(mutedStyleJson(context)))
            m.addOnCameraMoveListener { refreshPoints() }
            m.addOnCameraIdleListener { refreshPoints() }
            map = m
        }
    }

    // Frame the points once, when the map, the points and the measured size of the view are all ready.
    val hasSize = viewSize.width > 0 && viewSize.height > 0
    LaunchedEffect(map, framedPoints.isNotEmpty(), hasSize) {
        val m = map ?: return@LaunchedEffect
        if (fitted || framedPoints.isEmpty()) return@LaunchedEffect
        val fit = fitFor(framedPoints) ?: return@LaunchedEffect
        m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fit.lat, fit.lng), fit.zoom))
        fitted = true
        refreshPoints()
    }
    LaunchedEffect(map, markers, routePath) { refreshPoints() }

    val items = remember(screenPoints, markers, selectedKey, overlap, density) {
        val pts = markers.mapNotNull { mk -> screenPoints[mk.key]?.let { ScreenPoint(mk.key, it.x, it.y) } }
        with(density) {
            layoutMarkers(pts, selectedKey, overlap, PinWidth.toPx(), PinCollisionHeight.toPx(), PinWidth.toPx())
        }
    }
    val byKey = remember(markers) { markers.associateBy { it.key } }

    fun onClusterTap(members: List<MapMarker>, opensList: Boolean) {
        if (opensList) {
            sheetMembers = members
            return
        }
        val m = map ?: return
        val fit = fitFor(members.map { it.lat to it.lng }) ?: return
        val zoom = if (boundsOf(members.map { it.lat to it.lng })?.isPoint() == true) MAX_ZOOM else fit.zoom
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(fit.lat, fit.lng), zoom)
        m.animateCamera(update, CLUSTER_ANIM_MS)
    }

    Box(modifier.fillMaxSize().clipToBounds().onSizeChanged { viewSize = it }) {
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
        if (items.any { it is PlacedItem.Single && it.displaced }) {
            Canvas(Modifier.fillMaxSize()) {
                val lineWidth = 2.dp.toPx()
                val dotRadius = 4.dp.toPx()
                for (item in items) {
                    if (item !is PlacedItem.Single || !item.displaced) continue
                    val accent = byKey[item.key]?.accent ?: continue
                    val anchor = Offset(item.anchorX, item.anchorY)
                    drawLine(accent, anchor, Offset(item.x, item.y), strokeWidth = lineWidth, cap = StrokeCap.Round)
                    drawCircle(accent, dotRadius, anchor)
                }
            }
        }
        // The selected pin is drawn last so it stays on top of its neighbours.
        for (item in items.sortedBy { it is PlacedItem.Single && it.key == selectedKey }) {
            when (item) {
                is PlacedItem.Single -> key(item.key) {
                    val marker = byKey[item.key]
                    if (marker != null) with(density) {
                        MapPin(
                            marker = marker,
                            selected = marker.key == selectedKey,
                            onClick = onMarkerTap?.let { tap -> { tap(marker.key) } },
                            modifier = Modifier.offset(x = item.x.toDp() - PinWidth / 2, y = item.y.toDp() - TipOffset),
                        )
                    }
                }
                is PlacedItem.Group -> key("cluster:" + item.keys.first()) {
                    val members = item.keys.mapNotNull { k -> byKey[k] }
                    val opensList = onMarkerTap != null && shouldListCluster(members.map { it.lat to it.lng }, zoom)
                    if (members.size >= 2) with(density) {
                        ClusterBubble(
                            members = members,
                            opensList = opensList,
                            onClick = { onClusterTap(members, opensList) },
                            modifier = Modifier.offset(
                                x = item.x.toDp() - ClusterTouchSize / 2,
                                y = item.y.toDp() - ClusterTouchSize / 2,
                            ),
                        )
                    }
                }
            }
        }
    }
    sheetMembers?.let { members ->
        ClusterListSheet(
            members = members,
            onPick = { k ->
                sheetMembers = null
                onMarkerTap?.invoke(k)
            },
            onDismiss = { sheetMembers = null },
        )
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

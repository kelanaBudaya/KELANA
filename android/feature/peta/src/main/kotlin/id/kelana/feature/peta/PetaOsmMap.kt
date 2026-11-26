package id.kelana.feature.peta

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

/** Width of the pin box (drop plus label). */
internal val PinWidth = 76.dp
internal val DropSize = 44.dp

/** Distance from the top of the pin column to the drop's tip (rotated square: centre + half diagonal). */
private val TipOffset = DropSize / 2 + DropSize * 0.7071f

private const val MAP_PADDING_TOP_PX = 220
private const val MAP_PADDING_SIDE_PX = 100
private const val MAP_PADDING_BOTTOM_PX = 560

/**
 * The OpenStreetMap view (MapLibre, raster tiles from [TILE_URL_TEMPLATE]) with one tappable pin per place. Pins are
 * Compose nodes laid over the map and repositioned from the map projection whenever the camera moves, so they keep the
 * design's marker look and accessibility semantics. The camera frames [allPlaces] once. No location component is used:
 * the map shows places only (location is read only from "Ambil cap").
 */
@Composable
internal fun PetaMap(
    allPlaces: List<PetaPlace>,
    pins: List<PetaPlace>,
    selectedId: String?,
    onPinTap: (String) -> Unit,
    modifier: Modifier = Modifier,
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
    val currentPins by rememberUpdatedState(pins)

    fun refreshPoints() {
        val m = map ?: return
        screenPoints = currentPins.associate {
            val pt = m.projection.toScreenLocation(LatLng(it.place.lat, it.place.lng))
            it.place.id to Offset(pt.x, pt.y)
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
                isAttributionEnabled = false // replaced by our own visible text, see PetaScreen
                isLogoEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            m.setMinZoomPreference(8.0)
            m.setMaxZoomPreference(18.0)
            m.setStyle(Style.Builder().fromJson(osmStyleJson()))
            m.addOnCameraMoveListener { refreshPoints() }
            m.addOnCameraIdleListener { refreshPoints() }
            map = m
        }
    }

    // Frame the region's places once, when the map and the places are both ready.
    LaunchedEffect(map, allPlaces.isNotEmpty()) {
        val m = map ?: return@LaunchedEffect
        if (fitted || allPlaces.isEmpty()) return@LaunchedEffect
        val bounds = boundsOf(allPlaces.map { it.place.lat to it.place.lng }) ?: return@LaunchedEffect
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
    LaunchedEffect(map, pins) { refreshPoints() }

    Box(modifier.fillMaxSize().clipToBounds()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        // The selected pin is drawn last so it stays on top of its neighbours.
        for (pin in pins.sortedBy { it.place.id == selectedId }) {
            val pt = screenPoints[pin.place.id] ?: continue
            with(density) {
                MapPin(
                    place = pin,
                    selected = pin.place.id == selectedId,
                    onClick = { onPinTap(pin.place.id) },
                    modifier = Modifier.offset(x = pt.x.toDp() - PinWidth / 2, y = pt.y.toDp() - TipOffset),
                )
            }
        }
    }
}

/** One OkHttp client for tile requests, carrying the identifying User-Agent OSM requires. */
private fun installUserAgent() {
    HttpRequestUtil.setOkHttpClient(
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", TILE_USER_AGENT).build())
            }
            .build(),
    )
}

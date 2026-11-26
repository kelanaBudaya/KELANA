package id.kelana.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URLEncoder
import java.util.Locale

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

private fun coord(value: Double): String = String.format(Locale.US, "%.6f", value)

private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

/** `geo:` URI that drops a labelled pin at the place's coordinates (opens any maps app; Google Maps is preferred). */
fun googleMapsGeoUri(lat: Double, lng: Double, name: String): String =
    "geo:${coord(lat)},${coord(lng)}?q=${coord(lat)},${coord(lng)}(${encode(name)})"

/** Google's documented Maps URL, used when no maps app is installed. No API key needed. */
fun googleMapsWebUri(lat: Double, lng: Double): String =
    "https://www.google.com/maps/search/?api=1&query=${coord(lat)},${coord(lng)}"

/**
 * Opens the place's coordinates in Google Maps, falling back to the web URL in a Custom Tab. This is a plain link
 * out: no maps SDK and no device location (the coordinates sent are the place's, not the user's).
 */
fun openInGoogleMaps(context: Context, lat: Double, lng: Double, name: String) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsGeoUri(lat, lng, name))).setPackage(GOOGLE_MAPS_PACKAGE)
    try {
        context.startActivity(geo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(googleMapsWebUri(lat, lng)))
        } catch (_: ActivityNotFoundException) {
            // No browser either: nothing sensible to do.
        }
    }
}

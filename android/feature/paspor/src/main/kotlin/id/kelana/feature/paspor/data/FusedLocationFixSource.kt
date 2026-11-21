package id.kelana.feature.paspor.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One call to `FusedLocationProviderClient.getCurrentLocation()`. No `requestLocationUpdates`, no
 * `lastLocation`, nothing cached, nothing logged.
 */
@Singleton
class FusedLocationFixSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationFixSource {

    @SuppressLint("MissingPermission") // checked right below
    override suspend fun currentFix(): LocationFix {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return LocationFix.NoPermission

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellation = CancellationTokenSource()
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cancellation.cancel() }
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location ->
                        val fix = if (location == null || !location.hasAccuracy()) {
                            LocationFix.NoFix
                        } else {
                            LocationFix.Fix(
                                lat = location.latitude,
                                lng = location.longitude,
                                accuracyM = location.accuracy.toDouble(),
                                isMock = location.isMockLocation(),
                            )
                        }
                        if (cont.isActive) cont.resume(fix)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(LocationFix.NoFix) }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(LocationFix.NoPermission)
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun android.location.Location.isMockLocation(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider

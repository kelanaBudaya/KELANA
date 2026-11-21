package id.kelana.feature.paspor.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import id.kelana.core.navigation.CapBerhasilRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PetaRoute

private fun hasPreciseLocation(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Ambil cap for one place (Figma 6.1 to 6.5). Asks for location permission after explaining why, and reads the
 * location only when the user taps "Cap sekarang". [onReplace] swaps this screen for the next one (Cap berhasil).
 */
@Composable
internal fun AmbilCapScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AmbilCapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    // Re-check on every resume: the user may have changed the permission in the app settings.
    val owner = activity as? LifecycleOwner
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionChecked(hasPreciseLocation(context))
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { viewModel.onPermissionChecked(hasPreciseLocation(context)) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AmbilCapEvent.Stamped -> onReplace(CapBerhasilRoute(event.placeId))
            }
        }
    }

    val actions = remember(viewModel, launcher, context) {
        AmbilCapActions(
            onBack = onBack,
            onAllowLocation = {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onCapNow = viewModel::onCapNow,
            onRetry = viewModel::onRetry,
            onOpenSettings = { openAppSettings(context) },
            onCancelPending = viewModel::onCancelPending,
            onPendingExpired = viewModel::onPendingExpired,
            onDirections = { navigate(PetaRoute) },
            onLogin = { navigate(MasukRoute) },
        )
    }
    AmbilCapContent(state, actions, modifier)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No settings screen to open: the steps on screen still tell the user what to do.
    }
}

package id.kelana.feature.postcard.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.navigation.AmbilCapRoute
import id.kelana.core.navigation.BagikanPostcardRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.feature.postcard.data.PostcardFiles

/**
 * Tulis postcard (7.1). [navigate] pushes a route; [onReplace] swaps this screen for the next one so back from
 * Bagikan does not return to a filled form that could be published twice.
 */
@Composable
internal fun TulisScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
    viewModel: TulisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Photo Picker: images only, no permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoChosen(uri.toString())
    }
    // System camera app writes into our FileProvider cache file. No CAMERA permission is declared.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) viewModel.onPhotoChosen(PostcardFiles.cameraUri(context).toString())
    }

    // A guest is sent to Masuk once; coming back without signing in shows the "Masuk" prompt instead.
    var promptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.mode) {
        if (state.mode == TulisMode.Guest && !promptedLogin) {
            promptedLogin = true
            navigate(MasukRoute)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TulisEvent.OpenBagikan -> onReplace(BagikanPostcardRoute)
                TulisEvent.OpenLogin -> navigate(MasukRoute)
            }
        }
    }

    TulisContent(
        state = state,
        actions = TulisActions(
            onBack = onBack,
            onPickGallery = {
                try {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onPhotoUnavailable()
                }
            },
            onPickCamera = {
                try {
                    camera.launch(PostcardFiles.newCameraUri(context))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onPhotoUnavailable()
                }
            },
            onRemovePhoto = viewModel::onPhotoRemoved,
            onRating = viewModel::onRating,
            onMessage = viewModel::onMessageChanged,
            onStyle = viewModel::onStyle,
            onPublic = viewModel::onPublicChanged,
            onPublish = viewModel::onPublish,
            onPlace = viewModel::onPlacePicked,
            onLogin = { navigate(MasukRoute) },
            onOpenPaspor = { navigate(PasporRoute) },
            onTakeStamp = { navigate(AmbilCapRoute(it)) },
        ),
    )
}

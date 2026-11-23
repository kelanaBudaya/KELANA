package id.kelana.feature.postcard.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.navigation.AmbilCapRoute
import id.kelana.core.navigation.CeritaRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PasporRoute

/**
 * Unggah video (7.3). [navigate] pushes a route; [onReplace] swaps this screen for the next one so back from Cerita
 * does not return to a filled form that could be uploaded twice.
 */
@Composable
internal fun UnggahVideoScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
    viewModel: UnggahVideoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Photo Picker: videos only, no permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onVideoPicked(uri.toString())
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UnggahEvent.Uploaded -> onReplace(CeritaRoute)
                UnggahEvent.OpenLogin -> navigate(MasukRoute)
            }
        }
    }

    UnggahVideoContent(
        state = state,
        actions = UnggahVideoActions(
            onBack = onBack,
            onPick = {
                try {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onPickerUnavailable()
                }
            },
            onRemove = viewModel::onVideoRemoved,
            onCaption = viewModel::onCaption,
            onConsentArea = viewModel::onConsentArea,
            onConsentPeople = viewModel::onConsentPeople,
            onConsentCeremony = viewModel::onConsentCeremony,
            onUpload = viewModel::onUpload,
            onLogin = { navigate(MasukRoute) },
            onOpenPaspor = { navigate(PasporRoute) },
            onTakeStamp = { navigate(AmbilCapRoute(it)) },
        ),
    )
}

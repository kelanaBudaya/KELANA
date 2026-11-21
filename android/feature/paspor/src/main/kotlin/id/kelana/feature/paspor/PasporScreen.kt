package id.kelana.feature.paspor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.navigation.AmbilCapRoute
import id.kelana.core.navigation.CapBerhasilRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.feature.paspor.ui.AmbilCapScreen
import id.kelana.feature.paspor.ui.CapBerhasilScreen
import id.kelana.feature.paspor.ui.PasporActions
import id.kelana.feature.paspor.ui.PasporContent
import id.kelana.feature.paspor.ui.PasporViewModel

/**
 * Registers this feature's destinations: Paspor (7.4), Ambil cap (6.1 to 6.5) and Cap berhasil (6.3).
 * Called by :app only. [navigate] takes a route object; [onBack] pops; [onReplace] navigates and removes the
 * current Ambil cap screen from the back stack.
 */
fun NavGraphBuilder.pasporScreens(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
) {
    composable<PasporRoute> { PasporScreen(navigate = navigate, onBack = onBack) }
    composable<AmbilCapRoute> { AmbilCapScreen(navigate = navigate, onBack = onBack, onReplace = onReplace) }
    composable<CapBerhasilRoute> { CapBerhasilScreen(navigate = navigate, onReplace = onReplace) }
}

@Composable
internal fun PasporScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: PasporViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // A guest is sent to Masuk once; coming back without signing in shows the "Masuk" prompt instead.
    var promptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isGuest) {
        if (state.isGuest && !promptedLogin) {
            promptedLogin = true
            navigate(MasukRoute)
        }
    }

    PasporContent(
        state = state,
        actions = PasporActions(
            onBack = onBack,
            onProfile = { navigate(AkunRoute) },
            onPlace = { navigate(DetailTempatRoute(it.id)) },
            onPostcard = { navigate(PostcardRoute) },
            onLogin = { navigate(MasukRoute) },
        ),
    )
}

package id.kelana.feature.cerita

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.CeritaPostcardRoute
import id.kelana.core.navigation.CeritaRoute
import id.kelana.core.navigation.CeritaVideoRoute
import id.kelana.feature.cerita.ui.CeritaFeedScreen
import id.kelana.feature.cerita.ui.PostcardDetailScreen
import id.kelana.feature.cerita.ui.VideoDetailScreen

/**
 * Registers this feature's destinations: the Cerita tab feed, postcard detail and video detail. Called by :app only.
 * [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.ceritaScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<CeritaRoute> { CeritaFeedScreen(navigate = navigate) }
    composable<CeritaPostcardRoute> { PostcardDetailScreen(navigate = navigate, onBack = onBack) }
    composable<CeritaVideoRoute> { VideoDetailScreen(navigate = navigate, onBack = onBack) }
}

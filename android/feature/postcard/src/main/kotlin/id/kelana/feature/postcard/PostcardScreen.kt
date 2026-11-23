package id.kelana.feature.postcard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.BagikanPostcardRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.feature.postcard.ui.BagikanScreen
import id.kelana.feature.postcard.ui.TulisScreen

/**
 * Registers this feature's destinations: Tulis postcard (7.1, [PostcardRoute]) and Bagikan postcard (7.2,
 * [BagikanPostcardRoute]). Called by :app only. [navigate] takes a route object; [onBack] pops; [onReplace]
 * navigates and removes the current screen from the back stack.
 */
fun NavGraphBuilder.postcardScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
) {
    composable<PostcardRoute> { TulisScreen(navigate = navigate, onBack = onBack, onReplace = onReplace) }
    composable<BagikanPostcardRoute> { BagikanScreen(onBack = onBack, onReplace = onReplace) }
}

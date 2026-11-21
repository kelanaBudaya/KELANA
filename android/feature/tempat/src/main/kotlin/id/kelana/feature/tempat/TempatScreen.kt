package id.kelana.feature.tempat

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.feature.tempat.ui.DetailTempatScreen
import id.kelana.feature.tempat.ui.FotoResmiScreen
import id.kelana.feature.tempat.ui.LaporkanScreen

/**
 * Registers this feature's destinations: Detail tempat, Foto resmi, Laporkan. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.tempatScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<DetailTempatRoute> { entry ->
        DetailTempatScreen(route = entry.toRoute(), navigate = navigate, onBack = onBack)
    }
    composable<FotoResmiRoute> { entry -> FotoResmiScreen(route = entry.toRoute(), onBack = onBack) }
    composable<LaporkanRoute> { entry ->
        LaporkanScreen(route = entry.toRoute(), navigate = navigate, onBack = onBack)
    }
}

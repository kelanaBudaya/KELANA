package id.kelana.feature.tempat

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.ui.PlaceholderScreen

/**
 * Registers this feature's destinations: Detail tempat, Foto resmi, Laporkan. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.tempatScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<DetailTempatRoute> { entry ->
        DetailTempatScreen(route = entry.toRoute(), navigate = navigate, onBack = onBack)
    }
    composable<FotoResmiRoute> { entry -> FotoResmiScreen(route = entry.toRoute(), onBack = onBack) }
    composable<LaporkanRoute> { entry -> LaporkanScreen(route = entry.toRoute(), onBack = onBack) }
}

// Stub screens until the real ones are built from their Figma frames (feature agent replaces these).
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DetailTempatScreen(route: DetailTempatRoute, navigate: (Any) -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.tempat_placeholder, route.placeId), onBack = onBack)
}

@Composable
internal fun FotoResmiScreen(route: FotoResmiRoute, onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.foto_resmi_placeholder, route.placeId), onBack = onBack)
}

@Composable
internal fun LaporkanScreen(route: LaporkanRoute, onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.laporkan_placeholder, route.placeId), onBack = onBack)
}

package id.kelana.feature.jelajah

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.navigation.CariRoute
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.navigation.KategoriRoute
import id.kelana.core.navigation.SemuaTempatRoute
import id.kelana.core.ui.PlaceholderScreen
import id.kelana.core.ui.label

/**
 * Registers this feature's destinations: Jelajah (tab home), Cari, Kategori, Semua tempat. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.jelajahScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<JelajahRoute> { JelajahScreen(navigate = navigate) }
    composable<CariRoute> { CariScreen(onBack = onBack) }
    composable<KategoriRoute> { entry ->
        KategoriScreen(route = entry.toRoute(), onBack = onBack)
    }
    composable<SemuaTempatRoute> { SemuaTempatScreen(onBack = onBack) }
}

// Stub screens until the real ones are built from their Figma frames (feature agent replaces these).
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun JelajahScreen(navigate: (Any) -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.jelajah_placeholder))
}

@Composable
internal fun CariScreen(onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.cari_placeholder), onBack = onBack)
}

@Composable
internal fun KategoriScreen(route: KategoriRoute, onBack: () -> Unit) {
    val name = route.placeCategory?.label() ?: route.category
    PlaceholderScreen(text = stringResource(R.string.kategori_placeholder, name), onBack = onBack)
}

@Composable
internal fun SemuaTempatScreen(onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.semua_tempat_placeholder), onBack = onBack)
}

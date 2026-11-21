package id.kelana.feature.cerita

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.CeritaRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.ceritaScreen() {
    composable<CeritaRoute> { CeritaScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun CeritaScreen() {
    PlaceholderScreen(text = stringResource(R.string.cerita_placeholder))
}

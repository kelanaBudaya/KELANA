package id.kelana.feature.akun

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.akunScreen() {
    composable<AkunRoute> { AkunScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun AkunScreen() {
    PlaceholderScreen(text = stringResource(R.string.akun_placeholder))
}

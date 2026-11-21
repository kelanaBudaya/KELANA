package id.kelana.feature.akun

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.feature.akun.ui.MasukScreen
import id.kelana.core.ui.PlaceholderScreen

/**
 * Registers this feature's destinations. Called by :app only.
 * [onSignedIn] and [onGuest] tell the app where to go after Masuk finishes.
 */
fun NavGraphBuilder.akunScreen(onSignedIn: () -> Unit, onGuest: () -> Unit) {
    composable<AkunRoute> { AkunScreen() }
    composable<MasukRoute> { MasukScreen(onSignedIn = onSignedIn, onGuest = onGuest) }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun AkunScreen() {
    PlaceholderScreen(text = stringResource(R.string.akun_placeholder))
}

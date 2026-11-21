package id.kelana.feature.tiket

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.TiketRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.tiketScreen() {
    composable<TiketRoute> { TiketScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun TiketScreen() {
    PlaceholderScreen(text = stringResource(R.string.tiket_placeholder))
}

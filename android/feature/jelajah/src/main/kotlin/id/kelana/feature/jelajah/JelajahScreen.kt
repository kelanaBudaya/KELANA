package id.kelana.feature.jelajah

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.jelajahScreen() {
    composable<JelajahRoute> { JelajahScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun JelajahScreen() {
    PlaceholderScreen(text = stringResource(R.string.jelajah_placeholder))
}

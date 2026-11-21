package id.kelana.feature.rencana

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.RencanaRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.rencanaScreen() {
    composable<RencanaRoute> { RencanaScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun RencanaScreen() {
    PlaceholderScreen(text = stringResource(R.string.rencana_placeholder))
}

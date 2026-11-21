package id.kelana.feature.paspor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.pasporScreen() {
    composable<PasporRoute> { PasporScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun PasporScreen() {
    PlaceholderScreen(text = stringResource(R.string.paspor_placeholder))
}

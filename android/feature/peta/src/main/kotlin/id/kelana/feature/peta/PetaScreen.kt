package id.kelana.feature.peta

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.PetaRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.petaScreen() {
    composable<PetaRoute> { PetaScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun PetaScreen() {
    PlaceholderScreen(text = stringResource(R.string.peta_placeholder))
}

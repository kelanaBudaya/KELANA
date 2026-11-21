package id.kelana.feature.notifikasi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.NotifikasiRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.notifikasiScreen() {
    composable<NotifikasiRoute> { NotifikasiScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun NotifikasiScreen() {
    PlaceholderScreen(text = stringResource(R.string.notifikasi_placeholder))
}

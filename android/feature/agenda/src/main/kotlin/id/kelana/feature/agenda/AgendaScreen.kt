package id.kelana.feature.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.agendaScreen() {
    composable<AgendaRoute> { AgendaScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun AgendaScreen() {
    PlaceholderScreen(text = stringResource(R.string.agenda_placeholder))
}

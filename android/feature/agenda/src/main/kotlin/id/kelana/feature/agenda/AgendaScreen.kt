package id.kelana.feature.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.ui.PlaceholderScreen

/**
 * Registers this feature's destinations: Agenda (tab home) and Detail pentas. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation (for example
 * `OfficialSiteRoute` for the ticket site); [onBack] pops the back stack.
 */
fun NavGraphBuilder.agendaScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<AgendaRoute> { AgendaScreen(navigate = navigate) }
    composable<DetailPentasRoute> { entry ->
        DetailPentasScreen(route = entry.toRoute(), navigate = navigate, onBack = onBack)
    }
}

// Stub screens until the real ones are built from their Figma frames (feature agent replaces these).
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun AgendaScreen(navigate: (Any) -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.agenda_placeholder))
}

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DetailPentasScreen(route: DetailPentasRoute, navigate: (Any) -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(text = stringResource(R.string.detail_pentas_placeholder, route.eventId), onBack = onBack)
}

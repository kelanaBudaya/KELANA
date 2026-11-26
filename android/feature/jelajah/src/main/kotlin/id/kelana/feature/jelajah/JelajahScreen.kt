package id.kelana.feature.jelajah

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.SessionState
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.navigation.CariRoute
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.navigation.KategoriRoute
import id.kelana.core.navigation.NotifikasiRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PetaRoute
import id.kelana.core.navigation.SemuaTempatRoute
import id.kelana.core.navigation.TiketRoute
import id.kelana.core.ui.session.SessionViewModel
import id.kelana.core.ui.session.rememberRequireLogin
import kotlinx.coroutines.launch

/**
 * Registers this feature's destinations: Jelajah (tab home), Cari, Kategori, Semua tempat. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.jelajahScreens(navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<JelajahRoute> { JelajahScreen(navigate = navigate) }
    composable<CariRoute> { CariScreen(navigate = navigate, onBack = onBack) }
    composable<KategoriRoute> { entry ->
        KategoriScreen(route = entry.toRoute(), navigate = navigate, onBack = onBack)
    }
    composable<SemuaTempatRoute> { SemuaTempatScreen(navigate = navigate, onBack = onBack) }
}

/** Destination of the Paspor card on the home screen. */
internal val passportRoute: Any = PasporRoute

/** Where a home tile goes. Every tile has a destination registered in :app now, so this never returns null. */
internal fun tileRoute(tile: HomeTile): Any = when (tile) {
    HomeTile.Museum -> KategoriRoute.of(PlaceCategory.Museum)
    HomeTile.Candi -> KategoriRoute.of(PlaceCategory.Candi)
    HomeTile.Tari -> KategoriRoute.of(PlaceCategory.Tari)
    HomeTile.Pentas -> KategoriRoute.of(PlaceCategory.Pentas)
    HomeTile.SeniMusik -> KategoriRoute.of(PlaceCategory.SeniMusik)
    HomeTile.Semua -> SemuaTempatRoute
    HomeTile.Tiket -> TiketRoute
    HomeTile.Peta -> PetaRoute
}

private fun initialsOf(state: SessionState): String = when (state) {
    is SessionState.SignedIn -> (state.user.displayName ?: state.user.email).orEmpty()
        .split(' ', '@').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    else -> ""
}

@Composable
internal fun JelajahScreen(navigate: (Any) -> Unit) {
    val viewModel: JelajahViewModel = hiltViewModel()
    val session: SessionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val sessionState by session.sessionState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate, session)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbar.showSnackbar(comingSoon) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        HomeContent(
            state = state,
            avatarInitials = initialsOf(sessionState),
            modifier = Modifier.padding(padding),
            callbacks = HomeCallbacks(
                onSearch = { navigate(CariRoute) },
                onBell = { navigate(NotifikasiRoute) },
                // Artifact: the avatar opens Profil (AkunRoute). A guest goes to Masuk.
                onAvatar = { requireLogin { navigate(AkunRoute) } },
                // The passport needs an account: a guest goes to Masuk first.
                onPassport = { requireLogin { navigate(passportRoute) } },
                onQuickAction = { action ->
                    when (action) {
                        QuickAction.Agenda -> navigate(AgendaRoute)
                        // "Ambil cap" starts from a place; the passport lists the places (Detail tempat has the button).
                        QuickAction.AmbilCap -> requireLogin { navigate(passportRoute) }
                        // Phase 3 (PROVISIONAL): guests are routed to login first, then "segera hadir".
                        else -> if (action.needsAccount) requireLogin(showComingSoon) else showComingSoon()
                    }
                },
                onTile = { tile -> navigate(tileRoute(tile)) },
                onEvent = { navigate(DetailPentasRoute(it.event.id)) },
                onPlace = { navigate(DetailTempatRoute(it.id)) },
                onSeeAll = { navigate(SemuaTempatRoute) },
            ),
        )
    }
}

@Composable
internal fun CariScreen(navigate: (Any) -> Unit, onBack: () -> Unit) {
    val viewModel: CariViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    CariContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onPlace = { navigate(DetailTempatRoute(it.id)) },
        onEvent = { navigate(DetailPentasRoute(it.event.id)) },
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun KategoriScreen(route: KategoriRoute, navigate: (Any) -> Unit, onBack: () -> Unit) {
    // The ViewModel reads the category from its SavedStateHandle; [route] documents the argument.
    val viewModel: KategoriViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    KategoriContent(
        state = state,
        onBack = onBack,
        onOpenTodayChange = viewModel::onOpenTodayChange,
        onPlace = { navigate(DetailTempatRoute(it.id)) },
        // "Kabari aku" needs an account; the subscription itself is not built yet (does nothing after login).
        onNotifyMe = { requireLogin {} },
    )
}

@Composable
internal fun SemuaTempatScreen(navigate: (Any) -> Unit, onBack: () -> Unit) {
    val viewModel: SemuaTempatViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    SemuaTempatContent(
        state = state,
        onBack = onBack,
        onCategoryChange = viewModel::onCategoryChange,
        onOpenTodayChange = viewModel::onOpenTodayChange,
        onPlace = { navigate(DetailTempatRoute(it.id)) },
    )
}

package id.kelana.feature.jelajah

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import id.kelana.core.navigation.PostcardRoute
import id.kelana.core.navigation.RencanaRoute
import id.kelana.core.navigation.SemuaTempatRoute
import id.kelana.core.navigation.TiketRoute
import id.kelana.core.ui.session.SessionViewModel
import id.kelana.core.ui.session.rememberRequireLogin

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
    HomeTile.Topeng -> KategoriRoute.of(PlaceCategory.Topeng)
    HomeTile.Gamelan -> KategoriRoute.of(PlaceCategory.Gamelan)
    HomeTile.Semua -> SemuaTempatRoute
    HomeTile.Tiket -> TiketRoute
    HomeTile.Peta -> PetaRoute
}

/** Where a quick action of the passport card goes. Ambil cap starts from the passport; Rencana is a tab destination. */
internal fun quickActionRoute(action: QuickAction): Any = when (action) {
    QuickAction.Agenda -> AgendaRoute
    QuickAction.AmbilCap -> passportRoute
    QuickAction.Rencana -> RencanaRoute
    QuickAction.Postcard -> PostcardRoute()
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        HomeContent(
            state = state,
            avatarInitials = initialsOf(sessionState),
            greeting = greetingNameOf(sessionState),
            modifier = Modifier.padding(padding),
            callbacks = HomeCallbacks(
                onSearch = { navigate(CariRoute) },
                onBell = { navigate(NotifikasiRoute) },
                // Artifact: the avatar opens Profil (AkunRoute). A guest goes to Masuk.
                onAvatar = { requireLogin { navigate(AkunRoute) } },
                // The passport needs an account: a guest goes to Masuk first.
                onPassport = { requireLogin { navigate(passportRoute) } },
                onQuickAction = { action ->
                    // "Ambil cap" starts from the passport (Detail tempat has the button). Rencana is a tab: a plain
                    // navigate puts it on top of Jelajah and the bottom bar selects it; back returns to Jelajah.
                    val route = quickActionRoute(action)
                    if (action.needsAccount) requireLogin { navigate(route) } else navigate(route)
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

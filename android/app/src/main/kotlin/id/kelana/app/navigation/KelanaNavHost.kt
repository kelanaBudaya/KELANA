package id.kelana.app.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.app.AppViewModel
import id.kelana.app.StartDestination
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.navigation.DaftarRoute
import id.kelana.core.navigation.MasukEmailRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.IntroRoute
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.navigation.PilihDaerahRoute
import id.kelana.core.ui.OfficialSiteSheet
import id.kelana.core.ui.openCustomTab
import id.kelana.feature.agenda.agendaScreens
import id.kelana.feature.akun.akunScreen
import id.kelana.feature.cerita.ceritaScreens
import id.kelana.feature.jelajah.jelajahScreens
import id.kelana.feature.notifikasi.notifikasiScreen
import id.kelana.feature.onboarding.onboardingScreens
import id.kelana.feature.paspor.pasporScreens
import id.kelana.feature.peta.petaScreen
import id.kelana.feature.postcard.postcardScreen
import id.kelana.feature.rencana.rencanaScreens
import id.kelana.feature.tempat.tempatScreens
import id.kelana.feature.tiket.tiketScreen

/** Owns the NavHost and registers every destination. Bottom bar shows on the four tab destinations only. */
@Composable
fun KelanaAppContent(appViewModel: AppViewModel = hiltViewModel()) {
    val start by appViewModel.startDestination.collectAsState()
    // Wait for the stored first-launch flag so the NavHost starts at the right destination.
    val startDestination = start ?: return
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        destination?.hasRoute(tab.routeClass) == true
    }

    // One way into a tab, for the bottom bar and for in-screen shortcuts (e.g. the Agenda button on the passport card).
    // A plain navigate() to a tab leaves a back stack the bottom bar cannot leave (Explore did nothing after Agenda).
    val navigateToTab: (TopLevelTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedTab != null) {
                KelanaBottomBar(
                    selected = selectedTab,
                    onTabClick = navigateToTab,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when (startDestination) {
                StartDestination.Onboarding -> IntroRoute(page = 0)
                StartDestination.Jelajah -> JelajahRoute
            },
            // The Scaffold padding already holds the status bar, navigation bar and bottom bar insets. Mark them consumed,
            // otherwise every screen that calls statusBarsPadding() / navigationBarsPadding() adds them a second time.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            val navigate: (Any) -> Unit = { route ->
                val tab = TopLevelTab.entries.firstOrNull { it.route == route }
                if (tab != null) navigateToTab(tab) else navController.navigate(route)
            }
            val onBack: () -> Unit = { navController.popBackStack() }

            jelajahScreens(navigate = navigate, onBack = onBack)
            agendaScreens(navigate = navigate, onBack = onBack)
            rencanaScreens(navController = navController, navigate = navigate, onBack = onBack)
            ceritaScreens(navigate = navigate, onBack = onBack)

            // First launch: Intro(0) Mulai -> Intro(1..3) Kenalan -> PilihDaerah -> Jelajah. Whichever way the flow ends
            // (Pilih daerah, or Masuk reached from it), mark onboarding done and drop the whole flow from the back stack.
            val finishOnboarding: () -> Unit = {
                appViewModel.completeOnboarding()
                navController.navigate(JelajahRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
            onboardingScreens(navigate = navigate, onFinished = finishOnboarding)

            // Masuk finishes the same way whether the user signed in or chose to browse as a guest:
            // from onboarding it continues to Jelajah, from a login prompt it returns to where the user was.
            val finishMasuk: () -> Unit = {
                // Masuk, Masuk with email and Daftar all sit above the screen the user came from: pop back through Masuk.
                navController.popBackStack<MasukRoute>(inclusive = true)
                val current = navController.currentDestination
                val cameFromOnboarding = current?.hasRoute(IntroRoute::class) == true ||
                    current?.hasRoute(PilihDaerahRoute::class) == true
                if (cameFromOnboarding) finishOnboarding()
            }
            // Masuk with email and Daftar replace each other, so back from either returns to Masuk.
            val navigateAkun: (Any) -> Unit = { route ->
                val current = navController.currentDestination
                val swap = (route is DaftarRoute && current?.hasRoute(MasukEmailRoute::class) == true) ||
                    (route is MasukEmailRoute && current?.hasRoute(DaftarRoute::class) == true)
                if (swap && current != null) {
                    navController.navigate(route) { popUpTo(current.id) { inclusive = true } }
                } else {
                    navController.navigate(route)
                }
            }
            // Keluar on Profil: the user is a guest now, so restart at Jelajah with a clean back stack.
            val finishSignOut: () -> Unit = {
                navController.navigate(JelajahRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
            akunScreen(
                onSignedIn = finishMasuk,
                onGuest = finishMasuk,
                navigate = navigateAkun,
                onBack = onBack,
                onSignedOut = finishSignOut,
            )

            // "Ke situs tiket resmi" confirm sheet (Figma 03.04); "Buka situs" opens a Custom Tab.
            dialog<OfficialSiteRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
                val route = entry.toRoute<OfficialSiteRoute>()
                val context = LocalContext.current
                OfficialSiteSheet(
                    label = route.label,
                    onOpen = {
                        openCustomTab(context, route.url)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() },
                )
            }

            tempatScreens(navigate = navigate, onBack = onBack)
            petaScreen(navigate = navigate, onBack = onBack)
            // Ambil cap -> Cap berhasil -> Paspor: each step replaces the finished one so back does not return to it.
            val onReplace: (Any) -> Unit = { route ->
                val current = navController.currentDestination
                navController.navigate(route) { if (current != null) popUpTo(current.id) { inclusive = true } }
            }
            pasporScreens(navigate = navigate, onBack = onBack, onReplace = onReplace)
            postcardScreen(navigate = navigate, onBack = onBack, onReplace = onReplace)
            notifikasiScreen(navController = navController, navigate = navigate, onBack = onBack)
            tiketScreen(navigate = navigate, onBack = onBack)
        }
    }
}

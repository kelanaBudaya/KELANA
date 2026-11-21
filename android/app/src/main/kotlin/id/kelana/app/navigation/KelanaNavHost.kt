package id.kelana.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.app.AppViewModel
import id.kelana.app.StartDestination
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.OnboardingRoute
import id.kelana.feature.agenda.agendaScreen
import id.kelana.feature.akun.akunScreen
import id.kelana.feature.cerita.ceritaScreen
import id.kelana.feature.jelajah.jelajahScreen
import id.kelana.feature.notifikasi.notifikasiScreen
import id.kelana.feature.onboarding.onboardingScreen
import id.kelana.feature.paspor.pasporScreen
import id.kelana.feature.peta.petaScreen
import id.kelana.feature.postcard.postcardScreen
import id.kelana.feature.rencana.rencanaScreen
import id.kelana.feature.tempat.tempatScreen
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedTab != null) {
                KelanaBottomBar(
                    selected = selectedTab,
                    onTabClick = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when (startDestination) {
                StartDestination.Onboarding -> OnboardingRoute
                StartDestination.Jelajah -> JelajahRoute
            },
            modifier = Modifier.padding(padding),
        ) {
            jelajahScreen()
            agendaScreen()
            rencanaScreen()
            ceritaScreen()

            onboardingScreen(
                onContinue = {
                    appViewModel.completeOnboarding()
                    navController.navigate(JelajahRoute) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                },
                onLogin = { navController.navigate(MasukRoute) },
            )
            // Masuk finishes the same way whether the user signed in or chose to browse as a guest:
            // from onboarding it continues to Jelajah, from a login prompt it returns to where the user was.
            val finishMasuk: () -> Unit = {
                val cameFromOnboarding =
                    navController.previousBackStackEntry?.destination?.hasRoute(OnboardingRoute::class) == true
                if (cameFromOnboarding) {
                    appViewModel.completeOnboarding()
                    navController.navigate(JelajahRoute) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            }
            akunScreen(onSignedIn = finishMasuk, onGuest = finishMasuk)

            tempatScreen()
            petaScreen()
            pasporScreen()
            postcardScreen()
            notifikasiScreen()
            tiketScreen()
        }
    }
}

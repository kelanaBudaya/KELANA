package id.kelana.feature.rencana

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import id.kelana.core.navigation.MenyusunRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PengingatRoute
import id.kelana.core.navigation.PetaRuteRoute
import id.kelana.core.navigation.RencanaOfflineRoute
import id.kelana.core.navigation.RencanaRoute
import id.kelana.core.navigation.SusunGraphRoute
import id.kelana.core.navigation.SusunKebutuhanRoute
import id.kelana.core.navigation.SusunKendaraanRoute
import id.kelana.core.navigation.SusunMinatRoute
import id.kelana.core.navigation.SusunTanggalRoute
import id.kelana.core.navigation.SusunTemanRoute
import id.kelana.core.navigation.SusunWaktuRoute
import id.kelana.core.navigation.TempatTutupRoute
import id.kelana.feature.rencana.ui.BuildOutcome
import id.kelana.feature.rencana.ui.MenyusunContent
import id.kelana.feature.rencana.ui.PengingatScreen
import id.kelana.feature.rencana.ui.PetaRuteScreen
import id.kelana.feature.rencana.ui.RencanaOfflineScreen
import id.kelana.feature.rencana.ui.RencanaScreen
import id.kelana.feature.rencana.ui.SusunKebutuhanContent
import id.kelana.feature.rencana.ui.SusunKendaraanContent
import id.kelana.feature.rencana.ui.SusunMinatContent
import id.kelana.feature.rencana.ui.SusunTanggalContent
import id.kelana.feature.rencana.ui.SusunTemanContent
import id.kelana.feature.rencana.ui.SusunViewModel
import id.kelana.feature.rencana.ui.SusunWaktuContent
import id.kelana.feature.rencana.ui.TempatTutupScreen

/**
 * Registers every destination of the Rencana tab: the tab itself, the six planner questions (one nested graph so they
 * share a [SusunViewModel]), Menyusun, Pengingat, TempatTutup, the offline state and the route view. Called by :app only.
 *
 * [navController] is needed to scope the wizard ViewModel to its graph and to pop back to the tab when a plan is built.
 * [navigate] takes a route object from :core:navigation; [onBack] pops the back stack.
 */
fun NavGraphBuilder.rencanaScreens(navController: NavController, navigate: (Any) -> Unit, onBack: () -> Unit) {
    composable<RencanaRoute> { RencanaScreen(navigate = navigate) }

    navigation<SusunGraphRoute>(startDestination = SusunTanggalRoute) {
        composable<SusunTanggalRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunTanggalContent(
                state = state,
                onPick = vm::pickDate,
                onPreviousMonth = vm::showPreviousMonth,
                onNextMonth = vm::showNextMonth,
                onBack = onBack,
                onContinue = { navigate(SusunTemanRoute) },
            )
        }
        composable<SusunTemanRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunTemanContent(state, vm::selectCompanion, onBack) { navigate(SusunMinatRoute) }
        }
        composable<SusunMinatRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunMinatContent(state, vm::toggleInterest, onBack) { navigate(SusunWaktuRoute) }
        }
        composable<SusunWaktuRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunWaktuContent(state, vm::shiftStart, vm::shiftEnd, vm::selectPace, onBack) { navigate(SusunKendaraanRoute) }
        }
        composable<SusunKendaraanRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunKendaraanContent(state, vm::selectVehicle, onBack) { navigate(SusunKebutuhanRoute) }
        }
        composable<SusunKebutuhanRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            SusunKebutuhanContent(
                state = state,
                onToggle = vm::toggleNeed,
                onNone = vm::clearNeeds,
                onBack = onBack,
                // Building needs the server: without signal show the offline state instead of the loading screen.
                onSubmit = { navigate(if (vm.canBuildNow()) MenyusunRoute else RencanaOfflineRoute) },
            )
        }
        composable<MenyusunRoute> { entry ->
            val vm = wizardViewModel(navController, entry)
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) { vm.startBuild() }
            BackHandler(enabled = state.isBuilding) { /* the plan is being built; wait */ }
            LaunchedEffect(state.outcome) {
                when (state.outcome) {
                    BuildOutcome.Built -> {
                        vm.consumeOutcome()
                        navController.popBackStack<RencanaRoute>(inclusive = false)
                    }
                    BuildOutcome.Offline -> {
                        vm.consumeOutcome()
                        navController.navigate(RencanaOfflineRoute) { popUpTo<MenyusunRoute> { inclusive = true } }
                    }
                    BuildOutcome.NothingFits, null -> Unit
                }
            }
            MenyusunContent(
                state = state,
                onRetryBack = {
                    vm.consumeOutcome()
                    onBack()
                },
            )
        }
    }

    dialog<PengingatRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        PengingatScreen(onClose = onBack)
    }
    composable<TempatTutupRoute> {
        TempatTutupScreen(
            onBack = onBack,
            onDone = { navController.popBackStack<RencanaRoute>(inclusive = false) },
            onChooseOtherDay = { navigate(SusunGraphRoute) },
            onOpenPaspor = { navigate(PasporRoute) },
        )
    }
    composable<RencanaOfflineRoute> {
        RencanaOfflineScreen(
            onOpenPlan = { navController.popBackStack<RencanaRoute>(inclusive = false) },
            onOpenPaspor = { navigate(PasporRoute) },
        )
    }
    composable<PetaRuteRoute> { PetaRuteScreen(onBack = onBack) }
}

/** The ViewModel shared by every wizard screen, scoped to the nested graph's back stack entry. */
@Composable
private fun wizardViewModel(navController: NavController, entry: NavBackStackEntry): SusunViewModel {
    val parent = remember(entry) { navController.getBackStackEntry<SusunGraphRoute>() }
    return hiltViewModel(parent)
}

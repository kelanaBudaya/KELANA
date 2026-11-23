package id.kelana.feature.notifikasi

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import id.kelana.core.navigation.IZIN_NOTIF_RESULT_KEY
import id.kelana.core.navigation.IzinNotifRoute
import id.kelana.core.navigation.NotifikasiRoute
import id.kelana.core.navigation.PengaturanNotifRoute
import id.kelana.feature.notifikasi.ui.IzinNotifScreen
import id.kelana.feature.notifikasi.ui.NotifikasiScreen
import id.kelana.feature.notifikasi.ui.PengaturanNotifScreen

/**
 * Registers this feature's destinations: the notification center, Pengaturan notifikasi and the Izin notifikasi
 * sheet. Called by :app only. When the Izin sheet closes it sets [IZIN_NOTIF_RESULT_KEY] on the entry below it so
 * the caller (the Pengingat sheet) can carry on saving.
 */
fun NavGraphBuilder.notifikasiScreen(
    navController: NavController,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
) {
    composable<NotifikasiRoute> {
        NotifikasiScreen(onBack = onBack, onSettings = { navigate(PengaturanNotifRoute) }, navigate = navigate)
    }
    composable<PengaturanNotifRoute> { PengaturanNotifScreen(onBack = onBack) }
    dialog<IzinNotifRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        IzinNotifScreen(
            onDone = {
                navController.previousBackStackEntry?.savedStateHandle?.set(IZIN_NOTIF_RESULT_KEY, true)
                navController.popBackStack()
            },
        )
    }
}

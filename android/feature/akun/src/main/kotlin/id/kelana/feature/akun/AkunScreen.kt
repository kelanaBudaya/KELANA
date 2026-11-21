package id.kelana.feature.akun

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.navigation.DaftarRoute
import id.kelana.core.navigation.MasukEmailRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PilihDaerahRoute
import id.kelana.feature.akun.ui.DaftarScreen
import id.kelana.feature.akun.ui.MasukEmailScreen
import id.kelana.feature.akun.ui.MasukScreen
import id.kelana.feature.akun.ui.ProfilScreen

/**
 * Registers this feature's destinations. Called by :app only.
 * [onSignedIn] and [onGuest] tell the app where to go after Masuk finishes; [onSignedOut] after Keluar on Profil.
 * [AkunRoute] is the Profil screen (artifact 8.2).
 */
fun NavGraphBuilder.akunScreen(
    onSignedIn: () -> Unit,
    onGuest: () -> Unit,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    composable<AkunRoute> {
        ProfilScreen(
            onBack = onBack,
            onPaspor = { navigate(PasporRoute) },
            onDaerah = { navigate(PilihDaerahRoute) },
            onMasuk = { navigate(MasukRoute) },
            onSignedOut = onSignedOut,
        )
    }
    composable<MasukRoute> {
        MasukScreen(
            onSignedIn = onSignedIn,
            onGuest = onGuest,
            onEmail = { navigate(MasukEmailRoute) },
            onDaftar = { navigate(DaftarRoute) },
        )
    }
    // Masuk with email and Daftar swap places instead of stacking, so back never loops between them.
    composable<MasukEmailRoute> {
        MasukEmailScreen(onSignedIn = onSignedIn, onBack = onBack, onDaftar = { navigate(DaftarRoute) })
    }
    composable<DaftarRoute> {
        DaftarScreen(onSignedIn = onSignedIn, onBack = onBack, onMasuk = { navigate(MasukEmailRoute) })
    }
}

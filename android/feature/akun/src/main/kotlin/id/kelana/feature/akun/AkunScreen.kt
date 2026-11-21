package id.kelana.feature.akun

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.AkunRoute
import id.kelana.core.navigation.DaftarRoute
import id.kelana.core.navigation.MasukEmailRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.feature.akun.ui.DaftarScreen
import id.kelana.feature.akun.ui.MasukEmailScreen
import id.kelana.feature.akun.ui.MasukScreen
import id.kelana.core.ui.PlaceholderScreen

/**
 * Registers this feature's destinations. Called by :app only.
 * [onSignedIn] and [onGuest] tell the app where to go after Masuk finishes.
 */
fun NavGraphBuilder.akunScreen(
    onSignedIn: () -> Unit,
    onGuest: () -> Unit,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
) {
    composable<AkunRoute> { AkunScreen() }
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

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun AkunScreen() {
    PlaceholderScreen(text = stringResource(R.string.akun_placeholder))
}

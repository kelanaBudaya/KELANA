package id.kelana.feature.tiket

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.TiketRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. [onBack] pops the back stack. */
fun NavGraphBuilder.tiketScreen(onBack: () -> Unit) {
    composable<TiketRoute> { TiketScreen(onBack = onBack) }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun TiketScreen(onBack: () -> Unit) {
    // PROVISIONAL stub: titled placeholder with a back button until the real screen is built from its Figma frame.
    PlaceholderScreen(
        text = stringResource(R.string.tiket_placeholder),
        modifier = Modifier.statusBarsPadding(),
        onBack = onBack,
    )
}

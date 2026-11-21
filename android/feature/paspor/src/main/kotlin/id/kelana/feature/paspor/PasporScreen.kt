package id.kelana.feature.paspor

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. [onBack] pops the back stack. */
fun NavGraphBuilder.pasporScreen(onBack: () -> Unit) {
    composable<PasporRoute> { PasporScreen(onBack = onBack) }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun PasporScreen(onBack: () -> Unit) {
    // PROVISIONAL stub: titled placeholder with a back button until the real screen is built from its Figma frame.
    PlaceholderScreen(
        text = stringResource(R.string.paspor_placeholder),
        modifier = Modifier.statusBarsPadding(),
        onBack = onBack,
    )
}

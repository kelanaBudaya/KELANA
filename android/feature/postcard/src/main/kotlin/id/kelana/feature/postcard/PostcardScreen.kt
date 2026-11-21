package id.kelana.feature.postcard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.PostcardRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.postcardScreen() {
    composable<PostcardRoute> { PostcardScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun PostcardScreen() {
    PlaceholderScreen(text = stringResource(R.string.postcard_placeholder))
}

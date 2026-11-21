package id.kelana.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.kelana.core.navigation.OnboardingRoute
import id.kelana.core.ui.PlaceholderScreen

/** Registers this feature's destination. Called by :app only. */
fun NavGraphBuilder.onboardingScreen() {
    composable<OnboardingRoute> { OnboardingScreen() }
}

// Placeholder until the real screen is built from its Figma frame.
@Composable
internal fun OnboardingScreen() {
    PlaceholderScreen(text = stringResource(R.string.onboarding_placeholder))
}

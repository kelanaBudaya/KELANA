package id.kelana.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.navigation.IntroRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PilihDaerahRoute

/** Number of intro pages (frames 01.02 to 01.04 follow the Mulai page: the flow is Intro 0, 1, 2). */
internal const val IntroPageCount = 3

/**
 * Registers this feature's destinations: Intro (3 pages) and Pilih daerah. Called by :app only.
 * This signature is stable. [navigate] takes a route object from :core:navigation; [onFinished] ends onboarding
 * (the app stores the flag and opens Jelajah).
 */
fun NavGraphBuilder.onboardingScreens(navigate: (Any) -> Unit, onFinished: () -> Unit) {
    composable<IntroRoute> { entry -> IntroScreen(route = entry.toRoute(), navigate = navigate) }
    composable<PilihDaerahRoute> { PilihDaerahScreen(navigate = navigate, onFinished = onFinished) }
}

// Stub screens until the real ones are built from frames 01.01 to 01.05 (feature agent replaces these).
@Composable
internal fun IntroScreen(route: IntroRoute, navigate: (Any) -> Unit) {
    val isLast = route.page >= IntroPageCount - 1
    StubColumn(text = stringResource(R.string.intro_placeholder, route.page + 1, IntroPageCount)) {
        KelanaButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = { navigate(if (isLast) PilihDaerahRoute else IntroRoute(route.page + 1)) },
            modifier = Modifier.padding(top = KelanaSpacing.lg),
        )
        HaveAccountButton(navigate)
    }
}

@Composable
internal fun PilihDaerahScreen(navigate: (Any) -> Unit, onFinished: () -> Unit) {
    StubColumn(text = stringResource(R.string.pilih_daerah_placeholder)) {
        KelanaButton(
            text = stringResource(R.string.onboarding_start),
            onClick = onFinished,
            modifier = Modifier.padding(top = KelanaSpacing.lg),
        )
        HaveAccountButton(navigate)
    }
}

@Composable
private fun HaveAccountButton(navigate: (Any) -> Unit) {
    KelanaButton(
        text = stringResource(R.string.onboarding_have_account),
        onClick = { navigate(MasukRoute) },
        variant = KelanaButtonVariant.Text,
    )
}

@Composable
private fun StubColumn(text: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = KelanaSpacing.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = KelanaTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        content()
    }
}

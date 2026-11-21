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
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.navigation.OnboardingRoute

/**
 * Registers this feature's destination. Called by :app only.
 * [onContinue] ends the intro and goes on to Jelajah; [onLogin] opens Masuk.
 */
fun NavGraphBuilder.onboardingScreen(onContinue: () -> Unit, onLogin: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onContinue = onContinue, onLogin = onLogin) }
}

// Placeholder for frames 01.01 Mulai to 01.05 Pilih daerah. Real screens come later; do not build them here.
@Composable
internal fun OnboardingScreen(onContinue: () -> Unit, onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = KelanaSpacing.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_placeholder),
            style = MaterialTheme.typography.titleMedium,
            color = KelanaTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        KelanaButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier.padding(top = KelanaLg),
        )
        KelanaButton(
            text = stringResource(R.string.onboarding_have_account),
            onClick = onLogin,
            variant = KelanaButtonVariant.Text,
        )
    }
}

private val KelanaLg = KelanaSpacing.lg

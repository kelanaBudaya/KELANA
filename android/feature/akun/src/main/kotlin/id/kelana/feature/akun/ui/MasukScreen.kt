package id.kelana.feature.akun.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.akun.R
import id.kelana.feature.akun.data.GoogleCredentialHelper

private val ButtonHeight = 52.dp // PROVISIONAL: measured by eye from the 08.01 reference image

@Composable
internal fun MasukScreen(
    onSignedIn: () -> Unit,
    onGuest: () -> Unit,
    viewModel: MasukViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val helper = remember { GoogleCredentialHelper() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MasukEvent.RequestGoogleToken -> {
                    val result = helper.getIdToken(context, event.webClientId, event.hashedNonce)
                    viewModel.onGoogleResult(result)
                }
                MasukEvent.SignedIn -> onSignedIn()
                MasukEvent.ContinueAsGuest -> onGuest()
            }
        }
    }

    MasukContent(
        state = state,
        onGoogleClick = viewModel::onGoogleClick,
        onGuestClick = viewModel::onGuestClick,
    )
}

/** Figma frame 08.01 Masuk. The email button is intentionally not rendered yet (see TODO). */
@Composable
internal fun MasukContent(
    state: MasukUiState,
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = KelanaSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        // PROVISIONAL placeholder illustration; replace with the Figma export.
        Image(
            painter = painterResource(R.drawable.ill_masuk_placeholder),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.masuk_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.masuk_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.danger,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        GoogleButton(isLoading = state.isLoading, onClick = onGoogleClick)
        // TODO: "Lanjut dengan email" (primary blue pill, Figma 08.01) is hidden until email login is built.
        // Only Google sign-in exists, and a dead or greyed-out primary button would mislead.
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.masuk_guest),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            ),
            color = colors.primary,
            modifier = Modifier
                .clickable(onClick = onGuestClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GoogleButton(isLoading: Boolean, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        shape = RoundedCornerShape(50),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(colors.skyText, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.masuk_google_mark),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = colors.navy,
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.masuk_google),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = colors.ink,
                )
            }
        }
    }
}

@StringRes
private fun MasukError.messageRes(): Int = when (this) {
    MasukError.NotConfigured -> R.string.masuk_error_not_configured
    MasukError.Cancelled -> R.string.masuk_error_cancelled
    MasukError.NoGoogleAccount -> R.string.masuk_error_no_account
    MasukError.Network -> R.string.masuk_error_network
    MasukError.Generic -> R.string.masuk_error_generic
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MasukPreview() {
    KelanaTheme {
        MasukContent(state = MasukUiState(), onGoogleClick = {}, onGuestClick = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MasukErrorPreview() {
    KelanaTheme {
        MasukContent(state = MasukUiState(error = MasukError.NotConfigured), onGoogleClick = {}, onGuestClick = {})
    }
}

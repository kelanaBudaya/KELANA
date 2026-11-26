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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import id.kelana.core.model.AppLanguage
import id.kelana.core.ui.LanguageFlag
import id.kelana.core.ui.LanguageSheet
import id.kelana.core.ui.labelRes
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
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.akun.R
import id.kelana.feature.akun.data.GoogleCredentialHelper

private val ButtonHeight = 52.dp // PROVISIONAL: measured by eye from the 08.01 reference image

@Composable
internal fun MasukScreen(
    onSignedIn: () -> Unit,
    onGuest: () -> Unit,
    onEmail: () -> Unit,
    onDaftar: () -> Unit,
    viewModel: MasukViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val language by viewModel.language.collectAsState()
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
        onEmailClick = onEmail,
        onDaftarClick = onDaftar,
        language = language,
        onLanguage = viewModel::onLanguageSelected,
    )
}

/** Figma frame 08.01 Masuk. The "atau" divider, email option and Daftar link are PROVISIONAL (no artifact yet). */
@Composable
internal fun MasukContent(
    state: MasukUiState,
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onEmailClick: () -> Unit = {},
    onDaftarClick: () -> Unit = {},
    language: AppLanguage = AppLanguage.ID,
    onLanguage: (AppLanguage) -> Unit = {},
) {
    val colors = KelanaTheme.colors
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    if (showLanguageSheet) {
        LanguageSheet(
            selected = language,
            onSelect = {
                onLanguage(it)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
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
        Spacer(Modifier.height(KelanaSpacing.md))
        OrDivider()
        Spacer(Modifier.height(KelanaSpacing.md))
        KelanaButton(
            text = stringResource(R.string.masuk_email),
            onClick = onEmailClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        )
        Spacer(Modifier.height(KelanaSpacing.sm))
        SwitchLinkRow(
            prompt = stringResource(R.string.masuk_daftar_prompt),
            link = stringResource(R.string.masuk_daftar_link),
            onClick = onDaftarClick,
        )
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
        LanguageLink(language = language, onClick = { showLanguageSheet = true })
        Spacer(Modifier.height(8.dp))
    }
}

/** Quiet tertiary row: current flag, language name and a chevron. Opens the shared language sheet. */
@Composable
private fun LanguageLink(language: AppLanguage, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.masuk_language_description)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        LanguageFlag(language)
        Text(
            text = stringResource(language.labelRes()),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = colors.muted,
        )
        Image(painter = painterResource(R.drawable.akun_ic_chevron), contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun OrDivider() {
    val colors = KelanaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
        Text(
            text = stringResource(R.string.masuk_or),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = KelanaSpacing.md),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
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

package id.kelana.feature.akun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.akun.R

@Composable
internal fun MasukEmailScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    onDaftar: () -> Unit,
    viewModel: MasukEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MasukEmailEvent.SignedIn -> onSignedIn()
            }
        }
    }
    MasukEmailContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        onDaftar = onDaftar,
    )
}

/** PROVISIONAL design: built from existing tokens, no artifact screen yet. */
@Composable
internal fun MasukEmailContent(
    state: MasukEmailUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onDaftar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelanaTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        KelanaBackTopBar(title = stringResource(R.string.email_masuk_topbar), onBack = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KelanaSpacing.gutterWide, vertical = KelanaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            AuthHeader(
                title = stringResource(R.string.email_masuk_title),
                subtitle = stringResource(R.string.email_masuk_subtitle),
            )
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.email_field_email),
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading,
            )
            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.email_field_password),
                visible = state.passwordVisible,
                onToggleVisible = onTogglePasswordVisible,
                error = state.passwordError,
                imeAction = ImeAction.Done,
                enabled = !state.isLoading,
            )
            state.formError?.let { FormErrorText(it) }
            SubmitButton(
                text = stringResource(R.string.email_masuk_button),
                isLoading = state.isLoading,
                enabled = state.canSubmit,
                onClick = onSubmit,
            )
            SwitchLinkRow(
                prompt = stringResource(R.string.email_masuk_to_daftar_prompt),
                link = stringResource(R.string.email_masuk_to_daftar_link),
                onClick = onDaftar,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MasukEmailPreview() {
    KelanaTheme {
        MasukEmailContent(MasukEmailUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MasukEmailErrorPreview() {
    KelanaTheme {
        MasukEmailContent(
            MasukEmailUiState(
                email = "nama@contoh.id",
                password = "rahasia123",
                emailError = EmailAuthError.InvalidEmail,
                formError = EmailAuthError.WrongCredentials,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MasukEmailLoadingPreview() {
    KelanaTheme {
        MasukEmailContent(
            MasukEmailUiState(email = "nama@contoh.id", password = "rahasia123", passwordVisible = true, isLoading = true),
            {}, {}, {}, {}, {}, {},
        )
    }
}

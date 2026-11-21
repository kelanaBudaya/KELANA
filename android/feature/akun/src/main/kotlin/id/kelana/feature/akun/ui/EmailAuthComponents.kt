package id.kelana.feature.akun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.akun.R

/** Minimum touch target (accessibility). */
private val MinTarget = 48.dp

@Composable
private fun fieldColors() = KelanaTheme.colors.let { colors ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        errorBorderColor = colors.danger,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        errorContainerColor = colors.surface,
        focusedTextColor = colors.ink,
        unfocusedTextColor = colors.ink,
        errorTextColor = colors.ink,
        cursorColor = colors.primary,
        errorCursorColor = colors.danger,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.muted,
        errorLabelColor = colors.danger,
        errorSupportingTextColor = colors.danger,
    )
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: EmailAuthError?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    supportingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = KelanaTheme.colors
    val message = error?.let { stringResource(it.messageRes()) } ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KelanaSizes.buttonHeight),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        shape = KelanaShapes.input,
        colors = fieldColors(),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        // Only composed when there is something to say, so no empty line is reserved under the field.
        supportingText = message?.let { text ->
            { Text(text, color = if (error != null) colors.danger else colors.muted) }
        },
    )
}

/** Password field with a show/hide text toggle (text, not an icon, so it reads clearly and stays 48 dp). */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    error: EmailAuthError?,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val toggleLabel = stringResource(if (visible) R.string.email_password_hide else R.string.email_password_show)
    val toggleDescription = stringResource(
        if (visible) R.string.email_password_hide_description else R.string.email_password_show_description,
    )
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        modifier = modifier,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        enabled = enabled,
        supportingText = supportingText,
        trailing = {
            Box(
                modifier = Modifier
                    .heightIn(min = MinTarget)
                    .clickable(role = Role.Button, onClick = onToggleVisible)
                    .semantics { contentDescription = toggleDescription }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = toggleLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = KelanaTheme.colors.primary,
                )
            }
        },
    )
}

/** Primary pill button with a loading spinner; disabled while loading or when the form is incomplete. */
@Composable
internal fun SubmitButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KelanaSizes.buttonHeight),
        shape = KelanaShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.white,
            disabledContainerColor = if (isLoading) colors.primary else colors.outline,
            disabledContentColor = if (isLoading) colors.white else colors.muted,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.white,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Message about the whole form (network, server, rate limit, misconfiguration). */
@Composable
internal fun FormErrorText(error: EmailAuthError, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodySmall,
        color = KelanaTheme.colors.danger,
        modifier = modifier.fillMaxWidth(),
    )
}

/** "Belum punya akun? Daftar": a muted prompt and an underlined link, 48 dp tall. */
@Composable
internal fun SwitchLinkRow(
    prompt: String,
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prompt, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        Box(
            modifier = Modifier
                .heightIn(min = MinTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = KelanaSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
                color = colors.primary,
            )
        }
    }
}

@Composable
internal fun AuthHeader(title: String, subtitle: String) {
    val colors = KelanaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

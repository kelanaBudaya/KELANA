package id.kelana.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.AppLanguage
import id.kelana.core.ui.labelRes

/**
 * "Pilih bahasa" step between Mulai and Kenalan. Not in the artifact (no language screen there): built from the
 * Kenalan pieces (tinted panel, blob, primary "Lanjut" button) with option cards in the Profil row style.
 * Selecting applies at once: the platform recreates the Activity in the chosen language.
 */
@Composable
internal fun LanguageStepContent(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onNext: () -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KelanaSpacing.gutterWide),
        ) {
            Spacer(Modifier.height(KelanaSpacing.lg))
            val description = stringResource(R.string.bahasa_illustration)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(KelanaShapes.imagePanel)
                    .background(colors.skyText)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.onboarding_kenalan1_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Image(painterResource(R.drawable.onboarding_bahasa_bubbles), null, Modifier.size(width = 200.dp, height = 140.dp))
            }
            Spacer(Modifier.height(KelanaSpacing.lg))
            Text(stringResource(R.string.bahasa_title), color = colors.ink, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.bahasa_body), color = colors.muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(KelanaSpacing.lg))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppLanguage.entries.forEach { option ->
                    LanguageOptionCard(option, option == selected) { onSelect(option) }
                }
            }
            Spacer(Modifier.height(KelanaSpacing.md))
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KelanaSpacing.gutterWide)
                .padding(bottom = 28.dp)
                .heightIn(min = 52.dp),
            shape = KelanaShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.white),
        ) { Text(stringResource(R.string.intro_next), style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun LanguageOptionCard(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(KelanaShapes.card)
            .background(if (selected) colors.skyText else colors.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline, KelanaShapes.card)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = KelanaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.muted),
        )
        Text(stringResource(language.labelRes()), style = MaterialTheme.typography.titleSmall, color = colors.ink)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LanguageStepPreview() {
    KelanaTheme { LanguageStepContent(AppLanguage.SYSTEM, {}, {}) }
}

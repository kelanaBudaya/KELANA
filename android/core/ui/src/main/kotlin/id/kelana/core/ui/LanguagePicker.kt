package id.kelana.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.AppLanguage

/**
 * Label of a language choice. "Bahasa Indonesia" and "English" are written in their own language in every locale
 * (same text in both string files); only "Ikuti sistem" is translated.
 */
@StringRes
fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.ID -> R.string.language_name_id
    AppLanguage.EN -> R.string.language_name_en
}

/**
 * Bottom sheet with the three language options as radio rows (Profil > Bahasa). Picking an option applies
 * it at once, so the sheet closes right after [onSelect].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KelanaTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = KelanaSpacing.gutter).padding(bottom = KelanaSpacing.md),
        ) {
            Text(
                text = stringResource(R.string.language_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
                modifier = Modifier.padding(bottom = KelanaSpacing.sm),
            )
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.muted),
                        )
                        Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    }
                }
            }
        }
    }
}

package id.kelana.feature.tempat.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.BannerTone
import id.kelana.core.designsystem.component.InfoBanner
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.ReportReason
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.feature.tempat.R

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun LaporkanScreen(
    route: LaporkanRoute,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: LaporkanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val thanks = stringResource(R.string.laporkan_success)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LaporkanEvent.Sent -> {
                    Toast.makeText(context, thanks, Toast.LENGTH_SHORT).show()
                    onBack()
                }
                // A guest is sent to Masuk by route object (the login hook's behaviour).
                LaporkanEvent.NeedsLogin -> navigate(MasukRoute)
            }
        }
    }
    LaporkanContent(
        state = state,
        onBack = onBack,
        onReason = viewModel::selectReason,
        onDetails = viewModel::setDetails,
        onSubmit = viewModel::submit,
    )
}

/** Report form. PROVISIONAL layout (no frame PNG; overview slide conv 3, right phone). */
@Composable
internal fun LaporkanContent(
    state: LaporkanUiState,
    onBack: () -> Unit,
    onReason: (ReportReason) -> Unit,
    onDetails: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().imePadding()) {
        KelanaBackTopBar(title = stringResource(R.string.laporkan_title), onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KelanaSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            Text(
                text = stringResource(R.string.laporkan_question),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Column(Modifier.selectableGroup()) {
                LaporkanReasons.forEach { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = state.reason == reason,
                                onClick = { onReason(reason) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row is the touch target; the button itself carries no click.
                        RadioButton(
                            selected = state.reason == reason,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.muted,
                            ),
                        )
                        Text(
                            text = stringResource(reason.labelRes()),
                            modifier = Modifier.padding(start = KelanaSpacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.details,
                onValueChange = onDetails,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                label = { Text(stringResource(R.string.laporkan_details_label)) },
                supportingText = { Text(stringResource(R.string.laporkan_details_optional)) },
                shape = KelanaShapes.button,
            )
            // PROVISIONAL: visual-only, no photo picker is wired.
            KelanaButton(
                text = stringResource(R.string.laporkan_add_photo),
                onClick = {},
                variant = KelanaButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.failed) {
                InfoBanner(text = stringResource(R.string.laporkan_error), tone = BannerTone.Warning)
            }
        }
        KelanaButton(
            text = stringResource(R.string.laporkan_submit),
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().padding(KelanaSpacing.gutter),
        )
    }
}

private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.HoursOrPriceChanged -> R.string.laporkan_reason_hours_or_price
    ReportReason.PlaceClosed -> R.string.laporkan_reason_closed
    ReportReason.CeremonyShownWithoutPermission -> R.string.laporkan_reason_ceremony
    ReportReason.InappropriateMedia -> R.string.laporkan_reason_inappropriate
    ReportReason.Other -> R.string.laporkan_reason_inappropriate
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun LaporkanEmptyPreview() {
    KelanaTheme { LaporkanContent(LaporkanUiState(), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun LaporkanFilledPreview() {
    KelanaTheme {
        LaporkanContent(
            LaporkanUiState(reason = ReportReason.PlaceClosed, details = "Gerbang terkunci."),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun LaporkanErrorPreview() {
    KelanaTheme {
        LaporkanContent(LaporkanUiState(reason = ReportReason.PlaceClosed, failed = true), {}, {}, {}, {})
    }
}

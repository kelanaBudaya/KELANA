package id.kelana.feature.rencana.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.rencana.R

@Composable
internal fun PengingatScreen(onClose: () -> Unit, viewModel: PengingatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onClose() }
    PengingatContent(state, viewModel::selectTiming, viewModel::setAddToCalendar, onCancel = onClose, onSave = viewModel::save)
}

// Layout from Pengingat.dc.html: bottom sheet over a 55% ink scrim. Compared as code, not as a render.
@Composable
internal fun PengingatContent(
    state: PengingatUiState,
    onTiming: (ReminderTiming) -> Unit,
    onCalendar: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.inkDeep.copy(alpha = 0.55f))) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.background),
        ) {
            // PROVISIONAL art: the artifact draws two marks on a 170 dp cream panel.
            Box(Modifier.fillMaxWidth().height(170.dp).background(colors.creamYellow)) {
                Image(painterResource(R.drawable.rencana_cat_pentas), contentDescription = null, modifier = Modifier.align(Alignment.Center).size(88.dp).rotate(-8f))
            }
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp)) {
                Text(
                    stringResource(R.string.rencana_reminder_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                Text(
                    stringResource(R.string.rencana_reminder_body),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderTiming.entries.forEach { timing ->
                        val selected = state.timing == timing
                        val shape = RoundedCornerShape(14.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(if (selected) colors.skyText else colors.surface)
                                .border(if (selected) 2.dp else 1.5.dp, if (selected) colors.primary else colors.outline, shape)
                                .singleChoice(selected) { onTiming(timing) }
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colors.primary),
                            )
                            Text(
                                stringResource(reminderLabelRes(timing)),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.ink,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().multiChoice(state.addToCalendar, onCalendar),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.addToCalendar,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = colors.white),
                        )
                        Text(
                            stringResource(R.string.rencana_reminder_calendar),
                            modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.ink,
                        )
                    }
                }
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KelanaButton(
                        text = stringResource(R.string.rencana_cancel),
                        onClick = onCancel,
                        variant = KelanaButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                    KelanaButton(
                        text = stringResource(R.string.rencana_save),
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PengingatPreview() = KelanaTheme { PengingatContent(PengingatUiState(), {}, {}, {}, {}) }

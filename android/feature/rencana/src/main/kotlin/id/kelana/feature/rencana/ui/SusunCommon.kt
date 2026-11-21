package id.kelana.feature.rencana.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.rencana.R

/** Total number of wizard questions ("1/6" .. "6/6"). */
internal const val SusunSteps = 6

/**
 * Frame shared by the six questions (SusunTanggal .. SusunKebutuhan): back arrow, 8 dp progress bar, "n/6", title,
 * hint, content, and the 52 dp pill button pinned to the bottom. Compared as code with the artifact files, not a render.
 */
@Composable
internal fun SusunScaffold(
    step: Int,
    title: String,
    onBack: () -> Unit,
    buttonText: String,
    canContinue: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    topContent: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(KelanaSizes.backButton)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.rencana_ic_back),
                    contentDescription = stringResource(R.string.rencana_back),
                    modifier = Modifier.size(24.dp),
                )
            }
            val progressLabel = stringResource(R.string.rencana_susun_progress_label, step, SusunSteps)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.backgroundWarm)
                    .semantics {
                        contentDescription = progressLabel
                        progressBarRangeInfo = ProgressBarRangeInfo(step.toFloat(), 0f..SusunSteps.toFloat(), SusunSteps - 1)
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(step / SusunSteps.toFloat())
                        .height(8.dp)
                        .clip(KelanaShapes.chip)
                        .background(colors.accentOrange),
                )
            }
            Text(
                text = stringResource(R.string.rencana_susun_progress, step, SusunSteps),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = colors.muted,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            topContent()
            Text(
                text = title,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            content()
        }
        KelanaButton(
            text = buttonText,
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
                .navigationBarsPadding(),
        )
    }
}

/** Border 1.5 dp outline, or 2.5 dp orange on peach when [selected] (every Susun card in the artifact). */
@Composable
internal fun Modifier.susunCard(selected: Boolean, shape: Shape): Modifier {
    val colors = KelanaTheme.colors
    return this
        .clip(shape)
        .background(if (selected) colors.peach else colors.surface)
        .border(
            BorderStroke(if (selected) 2.5.dp else 1.5.dp, if (selected) colors.accentOrange else colors.outline),
            shape,
        )
}

/** A single-choice card (radio semantics). */
internal fun Modifier.singleChoice(selected: Boolean, onClick: () -> Unit): Modifier =
    this.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)

/** A multi-choice card (checkbox semantics). */
internal fun Modifier.multiChoice(checked: Boolean, onChange: (Boolean) -> Unit): Modifier =
    this.toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)

internal val ScreenGutter = KelanaSpacing.gutterWide

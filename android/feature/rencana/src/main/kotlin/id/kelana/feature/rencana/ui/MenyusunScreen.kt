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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Interest
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.TravelGroup
import id.kelana.feature.rencana.R
import java.time.LocalDate
import java.time.YearMonth

/** One line of the checklist. */
private enum class StageState { Done, Running, Pending }

/** Checklist labels for the answers, in order. Opening hours and travel time are always there. */
private fun stageLabels(state: SusunUiState): List<Int> = buildList {
    add(if (Interest.Pentas in state.interests) R.string.rencana_stage_shows else R.string.rencana_stage_hours)
    if (state.showsRestStage) add(if (state.companion == TravelGroup.Family) R.string.rencana_stage_rest_family else R.string.rencana_stage_rest)
    add(R.string.rencana_stage_travel)
    if (state.showsAccessStage) add(R.string.rencana_stage_access)
}

// Layout from Menyusun.dc.html. Compared as code, not as a render.
@Composable
internal fun MenyusunContent(
    state: SusunUiState,
    onRetryBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val labels = stageLabels(state)
    Column(modifier = modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        // PROVISIONAL art: the artifact draws a blob with three large marks.
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 80.dp)
                .fillMaxWidth()
                .height(300.dp)
                .clip(KelanaShapes.imagePanel)
                .background(colors.skyText),
        ) {
            Image(painterResource(R.drawable.rencana_cat_candi), contentDescription = null, modifier = Modifier.align(Alignment.Center).size(80.dp))
            Image(painterResource(R.drawable.rencana_cat_museum), contentDescription = null, modifier = Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 40.dp).size(80.dp).rotate(-8f))
            Image(painterResource(R.drawable.rencana_veh_walk), contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).size(36.dp).rotate(190f))
        }
        if (state.outcome == BuildOutcome.NothingFits) {
            Text(
                stringResource(R.string.rencana_nothing_title),
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.rencana_nothing_body),
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            KelanaButton(
                text = stringResource(R.string.rencana_nothing_action),
                onClick = onRetryBack,
                variant = KelanaButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
            )
        } else {
            Text(
                stringResource(R.string.rencana_building_title),
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            Column(
                modifier = Modifier.padding(start = 40.dp, end = 40.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    val stage = when {
                        index < state.stage -> StageState.Done
                        index == state.stage && state.isBuilding -> StageState.Running
                        else -> StageState.Pending
                    }
                    StageRow(stringResource(label), stage)
                }
            }
        }
    }
}

@Composable
private fun StageRow(label: String, stage: StageState) {
    val colors = KelanaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (stage) {
            StageState.Done -> Image(painterResource(R.drawable.rencana_check), contentDescription = null, modifier = Modifier.size(24.dp))
            StageState.Running -> {
                val running = stringResource(R.string.rencana_stage_running)
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).semantics { contentDescription = running },
                    color = colors.accentOrange,
                    strokeWidth = 3.dp,
                )
            }
            StageState.Pending -> Box(Modifier.size(24.dp).clip(CircleShape).border(2.dp, colors.divider, CircleShape))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = if (stage == StageState.Running) FontWeight.ExtraBold else FontWeight.SemiBold,
            ),
            color = if (stage == StageState.Pending) colors.iconMuted else colors.ink,
        )
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 11, 20)

private fun previewBuilding() = SusunUiState(
    today = PreviewToday,
    visibleMonth = YearMonth.of(2026, 11),
    startDate = PreviewToday,
    companion = TravelGroup.Family,
    interests = setOf(Interest.Pentas, Interest.Museum),
    needs = setOf(SpecialNeed.Wheelchair),
    isBuilding = true,
    stage = 2,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BuildingPreview() = KelanaTheme { MenyusunContent(previewBuilding(), {}) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NothingFitsPreview() = KelanaTheme { MenyusunContent(previewBuilding().copy(isBuilding = false, outcome = BuildOutcome.NothingFits), {}) }

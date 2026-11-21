package id.kelana.feature.rencana.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlanStop
import id.kelana.core.ui.formatClock
import id.kelana.feature.rencana.R

@Composable
internal fun PetaRuteScreen(onBack: () -> Unit, viewModel: PetaRuteViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    PetaRuteContent(state, onBack, onOpenMaps = { openInMapsApp(context, it) })
}

/** Hands the stop to the phone's map app. Does not read the user's location; the map app decides what to do. */
private fun openInMapsApp(context: Context, stop: PlanStop) {
    val label = Uri.encode(stop.placeName)
    val uri = Uri.parse("geo:${stop.lat},${stop.lng}?q=${stop.lat},${stop.lng}($label)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No map app installed: nothing to open.
    }
}

/**
 * Route view (PetaRuteIlustratif.dc.html). PROVISIONAL: the map SDK (MapLibre) is owned by :feature:peta and may not be
 * imported here, so the map area is a flat placeholder with the numbered markers placed from the stops' coordinates
 * (schematic, not a real map). Replace the placeholder box with the shared map component when it exists.
 */
@Composable
internal fun PetaRuteContent(
    state: PetaRuteUiState,
    onBack: () -> Unit,
    onOpenMaps: (PlanStop) -> Unit,
) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.skyText)) {
        SchematicMap(state, Modifier.fillMaxSize())
        Text(
            stringResource(R.string.rencana_map_placeholder),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp)
                .clip(KelanaShapes.chip)
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
        )
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp)
                .size(KelanaSizes.backButton)
                .clip(CircleShape)
                .background(colors.surface)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.rencana_ic_back), contentDescription = stringResource(R.string.rencana_back), modifier = Modifier.size(24.dp))
        }
        val next = state.next
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.background)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)
                .navigationBarsPadding(),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.divider),
            )
            if (next == null) {
                Text(
                    stringResource(R.string.rencana_map_empty),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            } else {
                Text(
                    stringResource(R.string.rencana_map_next),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                    color = colors.accentOrange,
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(56.dp).clip(KelanaShapes.card).background(colors.skyText), contentAlignment = Alignment.Center) {
                        Image(painterResource(categoryArt(next.category, next.isEvent)), contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(next.eventTitle ?: next.placeName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = colors.ink)
                        val travel = next.travel
                        val lines = listOfNotNull(
                            travel?.let { travelText(it.mode, it.minutes) },
                            arrivalLine(next),
                        )
                        if (lines.isNotEmpty()) {
                            Text(lines.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                    }
                }
                KelanaButton(
                    text = stringResource(R.string.rencana_map_open_app),
                    onClick = { onOpenMaps(next) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
        }
    }
}

/** "Tiba sebelum 18.45" for a show with an arrival advice and a checked schedule. */
@Composable
private fun arrivalLine(stop: PlanStop): String? {
    val schedule = stop.schedule
    val advice = stop.arrivalAdviceMax
    if (schedule == null || schedule.checkedAt == null || advice == null) return null
    return stringResource(R.string.rencana_map_arrive_before, formatClock(schedule.value.start.minusMinutes(advice.toLong())))
}

/** Numbered markers placed by relative position of the stops' coordinates. */
@Composable
private fun SchematicMap(state: PetaRuteUiState, modifier: Modifier) {
    val colors = KelanaTheme.colors
    val stops = state.stops
    if (stops.isEmpty()) return
    BoxWithConstraints(modifier.padding(horizontal = 48.dp, vertical = 120.dp)) {
        val minLat = stops.minOf { it.lat }
        val maxLat = stops.maxOf { it.lat }
        val minLng = stops.minOf { it.lng }
        val maxLng = stops.maxOf { it.lng }
        val nextIndex = state.next?.let { n -> stops.indexOfFirst { it.key == n.key } } ?: -1
        stops.forEachIndexed { index, stop ->
            val fx = if (maxLng > minLng) ((stop.lng - minLng) / (maxLng - minLng)).toFloat() else 0.5f
            val fy = if (maxLat > minLat) ((maxLat - stop.lat) / (maxLat - minLat)).toFloat() else 0.5f
            val color = when {
                nextIndex >= 0 && index < nextIndex -> colors.successText
                index == nextIndex -> colors.accentOrange
                else -> colors.primary
            }
            Marker(
                number = index + 1,
                label = stop.placeName,
                color = color,
                modifier = Modifier.offset(x = (maxWidth - MarkerWidth) * fx, y = (maxHeight - MarkerHeight) * fy),
            )
        }
    }
}

private val MarkerWidth: Dp = 96.dp
private val MarkerHeight: Dp = 68.dp

@Composable
private fun Marker(number: Int, label: String, color: Color, modifier: Modifier) {
    val colors = KelanaTheme.colors
    Column(modifier.size(width = MarkerWidth, height = MarkerHeight), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surface).border(3.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp).clip(KelanaShapes.chip).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaRutePreview() = KelanaTheme {
    val stops = PreviewRencana.stops()
    PetaRuteContent(PetaRuteUiState(isLoading = false, stops = stops, next = stops[1]), {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PetaRuteEmptyPreview() = KelanaTheme { PetaRuteContent(PetaRuteUiState(isLoading = false), {}, {}) }

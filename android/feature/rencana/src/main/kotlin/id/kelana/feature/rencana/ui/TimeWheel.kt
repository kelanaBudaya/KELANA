package id.kelana.feature.rencana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.rencana.R
import kotlinx.coroutines.flow.filter
import java.time.LocalTime

// Layout dimensions of this component (the design system has no wheel picker).
private val WheelRowHeight = 36.dp
private const val VisibleRows = 3
private val WheelHeight = WheelRowHeight * VisibleRows
private val HighlightShape = RoundedCornerShape(10.dp)

/**
 * An alarm-style scrolling wheel: [VisibleRows] rows, the centred row is the selection and sits on a warm highlight,
 * and a fling snaps to a row. No wrap-around. Scrolling settles into [onSelected]; a change of [selected] from outside
 * (or from the accessibility actions) scrolls the wheel, but never while the user is scrolling it.
 *
 * Accessibility: the wheel is one element with [contentDescription], the current value as state, and two custom
 * actions that move the selection one step earlier or later, so TalkBack users need not scroll.
 */
@Composable
internal fun WheelPicker(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    label: (Int) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val rowPx = with(LocalDensity.current) { WheelRowHeight.roundToPx() }
    // With one blank row of padding above and below, the item at the scroll origin is the centred one.
    val centeredIndex by remember(values) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset * 2 >= rowPx) 1 else 0
            index.coerceIn(0, values.lastIndex)
        }
    }
    val currentValues by rememberUpdatedState(values)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelected by rememberUpdatedState(onSelected)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                val value = currentValues[centeredIndex]
                if (value != currentSelected) currentOnSelected(value)
            }
    }
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && centeredIndex != selectedIndex) listState.animateScrollToItem(selectedIndex)
    }

    val earlierText = stringResource(R.string.rencana_waktu_earlier, contentDescription)
    val laterText = stringResource(R.string.rencana_waktu_later, contentDescription)
    Box(
        modifier = modifier
            .height(WheelHeight)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                stateDescription = label(selected)
                customActions = listOf(
                    CustomAccessibilityAction(earlierText) {
                        values.getOrNull(selectedIndex - 1)?.let(onSelected)
                        true
                    },
                    CustomAccessibilityAction(laterText) {
                        values.getOrNull(selectedIndex + 1)?.let(onSelected)
                        true
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(WheelRowHeight)
                .background(colors.backgroundWarm, HighlightShape),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = WheelRowHeight),
            flingBehavior = rememberSnapFlingBehavior(listState),
        ) {
            itemsIndexed(values) { index, value ->
                val centered = index == centeredIndex
                Box(Modifier.fillMaxWidth().height(WheelRowHeight), contentAlignment = Alignment.Center) {
                    Text(
                        text = label(value),
                        textAlign = TextAlign.Center,
                        style = if (centered) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        color = if (centered) colors.ink else colors.muted,
                        modifier = if (centered) Modifier else Modifier.alpha(0.6f),
                    )
                }
            }
        }
    }
}

private val Hours = (5..23).toList()
private val Minutes = (0..55 step 5).toList()

/** Hour wheel (05..23), a colon and minute wheel (00..55 in steps of 5). [label] names the field, e.g. "Mulai". */
@Composable
internal fun TimeWheels(time: LocalTime, onChange: (LocalTime) -> Unit, label: String, modifier: Modifier = Modifier) {
    val hourName = stringResource(R.string.rencana_waktu_hour)
    val minuteName = stringResource(R.string.rencana_waktu_minute)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        WheelPicker(
            values = Hours,
            selected = time.hour,
            onSelected = { onChange(LocalTime.of(it, time.minute)) },
            label = { "%02d".format(it) },
            contentDescription = "$label $hourName",
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineSmall,
            color = KelanaTheme.colors.ink,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        WheelPicker(
            values = Minutes,
            selected = time.minute,
            onSelected = { onChange(LocalTime.of(time.hour, it)) },
            label = { "%02d".format(it) },
            contentDescription = "$label $minuteName",
            modifier = Modifier.weight(1f),
        )
    }
}

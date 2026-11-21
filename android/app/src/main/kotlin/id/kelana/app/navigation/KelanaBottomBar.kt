package id.kelana.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.theme.TabLabelActive
import id.kelana.core.designsystem.theme.TabLabelInactive

private val PillWidth = 64.dp
private val PillHeight = 32.dp
private val IconSize = 24.dp
private val TopPadding = 10.dp
private val PillToLabel = 8.dp

/**
 * Bottom bar, 84 dp tall. Active tab: colour icon on a light-blue pill (about 64 x 32 dp) with a Bold navy label.
 * Inactive tab: grey icon and Regular muted label (docs/design/FIGMA-FRAMES.md).
 * The vertical offsets (10 dp above the pill, 8 dp pill to label) are PROVISIONAL, measured by eye from the reference image.
 */
@Composable
fun KelanaBottomBar(
    selected: TopLevelTab?,
    onTabClick: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Surface(modifier = modifier, color = colors.surface) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = colors.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KelanaSizes.bottomBarHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TopLevelTab.entries.forEach { tab ->
                    TabItem(tab = tab, isSelected = tab == selected, onClick = { onTabClick(tab) })
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: TopLevelTab, isSelected: Boolean, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier
            .weight(1f)
            .height(KelanaSizes.bottomBarHeight)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(top = TopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(PillWidth, PillHeight)
                .clip(RoundedCornerShape(50))
                .then(if (isSelected) Modifier.background(colors.tabActivePill) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isSelected) tab.icon.active else tab.icon.inactive),
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(PillToLabel))
        Text(
            text = stringResource(tab.label),
            style = if (isSelected) TabLabelActive else TabLabelInactive,
            color = if (isSelected) colors.navy else colors.muted,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun KelanaBottomBarPreview() {
    KelanaTheme {
        KelanaBottomBar(selected = TopLevelTab.Jelajah, onTabClick = {})
    }
}

package id.kelana.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.theme.TabLabelActive
import id.kelana.core.designsystem.theme.TabLabelInactive

/**
 * Bottom bar: 84 dp tall, active label Bold 12 navy, inactive Regular 12 muted
 * (docs/design/FIGMA-FRAMES.md). No icons yet: the icon set is not in the design notes.
 */
@Composable
fun KelanaBottomBar(
    selected: TopLevelTab?,
    onTabClick: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Surface(modifier = modifier, color = colors.white) {
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
    Box(
        modifier = Modifier
            .weight(1f)
            .height(KelanaSizes.bottomBarHeight)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(tab.label),
            style = if (isSelected) TabLabelActive else TabLabelInactive,
            color = if (isSelected) colors.navy else colors.muted,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KelanaBottomBarPreview() {
    KelanaTheme {
        KelanaBottomBar(selected = TopLevelTab.Agenda, onTabClick = {})
    }
}

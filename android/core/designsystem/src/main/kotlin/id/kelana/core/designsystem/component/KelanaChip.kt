package id.kelana.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.ChipLabel
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme

@Composable
fun KelanaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = ChipLabel) },
        modifier = modifier.height(KelanaSizes.chipHeight),
        shape = KelanaShapes.chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.white,
            labelColor = colors.ink,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.white,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outline,
            selectedBorderColor = colors.primary,
            borderWidth = 1.5.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun KelanaChipPreview() {
    KelanaTheme {
        Row(Modifier.padding(16.dp)) {
            KelanaChip(stringResource(R.string.ds_preview_chip_selected), selected = true, onClick = {})
            KelanaChip(
                stringResource(R.string.ds_preview_chip_unselected),
                selected = false,
                onClick = {},
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

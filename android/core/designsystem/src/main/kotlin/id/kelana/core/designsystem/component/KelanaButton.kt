package id.kelana.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme

enum class KelanaButtonVariant { Primary, Secondary, Outline, Text }

@Composable
fun KelanaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KelanaButtonVariant = KelanaButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = KelanaTheme.colors
    val sizing = modifier.heightIn(min = KelanaSizes.buttonHeight)
    when (variant) {
        KelanaButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = sizing,
            enabled = enabled,
            shape = KelanaShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = colors.navy, contentColor = colors.white),
        ) { Text(text) }

        KelanaButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = sizing,
            enabled = enabled,
            shape = KelanaShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = colors.orange, contentColor = colors.white),
        ) { Text(text) }

        KelanaButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = sizing,
            enabled = enabled,
            shape = KelanaShapes.button,
            border = BorderStroke(1.dp, if (enabled) colors.navy else colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.navy),
        ) { Text(text) }

        KelanaButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = sizing,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.navy),
        ) { Text(text) }
    }
}

@Preview(showBackground = true)
@Composable
private fun KelanaButtonPreview() {
    KelanaTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = stringResource(R.string.ds_preview_button)
            KelanaButtonVariant.entries.forEach {
                KelanaButton(label, onClick = {}, variant = it, modifier = Modifier.fillMaxWidth())
            }
            KelanaButton(label, onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}

package id.kelana.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.kelana.core.designsystem.component.OfficialFactRow as DesignOfficialFactRow
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.OfficialFact

/**
 * Renders an [OfficialFact]. The value is formatted only when the fact has a check date; otherwise the row shows
 * "Sedang dicek" and [format] is never called. This is the way a feature shows hours, prices and show times.
 */
@Composable
fun <T> OfficialFactRow(
    label: String,
    fact: OfficialFact<T>,
    modifier: Modifier = Modifier,
    format: @Composable (T) -> String,
) {
    when (val display = fact.display()) {
        is FactDisplay.Checked -> DesignOfficialFactRow(
            label = label,
            value = format(display.value),
            checkedAt = display.checkedAt,
            modifier = modifier,
        )
        FactDisplay.Checking -> DesignOfficialFactRow(label = label, value = null, checkedAt = null, modifier = modifier)
    }
}

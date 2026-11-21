package id.kelana.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import java.time.LocalDate

/**
 * The value that may be shown for an official fact: null when there is no check date (the product rule in
 * AGENTS.md: a fact without `checkedAt` must not be rendered), or when there is no value at all.
 */
internal fun visibleFactValue(value: String?, checkedAt: LocalDate?): String? =
    if (checkedAt == null) null else value

/**
 * One official fact: [label], then the value with "Diperiksa <tanggal>" ([VerifiedBadge]).
 * Without [checkedAt] the value is hidden and "Sedang dicek" is shown instead.
 * Prefer the `OfficialFactRow(label, fact, format)` overload in :core:ui, which takes an `OfficialFact`.
 * Layout: label 13/18 muted (bodySmall), value 16/800 ink (titleMedium, KatMuseum row title), badge below.
 */
@Composable
fun OfficialFactRow(
    label: String,
    value: String?,
    checkedAt: LocalDate?,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val shown = visibleFactValue(value, checkedAt)
    Column(modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        if (shown != null && checkedAt != null) {
            Text(
                text = shown,
                modifier = Modifier.padding(top = KelanaSpacing.xs),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            VerifiedBadge(checkedAt = checkedAt, modifier = Modifier.padding(top = KelanaSpacing.xs))
        } else {
            Text(
                text = stringResource(R.string.ds_fact_pending),
                modifier = Modifier.padding(top = KelanaSpacing.xs),
                style = MaterialTheme.typography.titleMedium,
                color = colors.warnText,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun OfficialFactRowPreview() {
    KelanaTheme {
        Column(Modifier.padding(KelanaSpacing.gutter)) {
            OfficialFactRow(
                label = stringResource(R.string.ds_preview_fact_label),
                value = stringResource(R.string.ds_preview_fact_value),
                checkedAt = LocalDate.of(2026, 11, 20),
            )
            OfficialFactRow(
                label = stringResource(R.string.ds_preview_fact_label),
                value = null,
                checkedAt = null,
                modifier = Modifier.padding(top = KelanaSpacing.md),
            )
        }
    }
}

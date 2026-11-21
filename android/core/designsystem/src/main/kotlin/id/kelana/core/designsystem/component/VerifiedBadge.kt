package id.kelana.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

/** Formats a check date for id-ID, for example `20 Nov 2026`. */
fun formatCheckedDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale).format(date)

/** "Diperiksa 20 Nov 2026". Official facts must always show their check date. */
@Composable
fun VerifiedBadge(checkedAt: LocalDate, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val text = stringResource(R.string.ds_verified_checked, remember(checkedAt) { formatCheckedDate(checkedAt) })
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(colors.success, CircleShape))
        Text(
            text = text,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifiedBadgePreview() {
    KelanaTheme {
        VerifiedBadge(checkedAt = LocalDate.of(2026, 11, 20), modifier = Modifier.padding(16.dp))
    }
}

package id.kelana.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.kelana.core.model.PlaceCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "Rp150.000" (id-ID grouping, no space). */
fun formatRupiah(amount: Long): String {
    val symbols = DecimalFormatSymbols(IdLocale)
    return "Rp" + DecimalFormat("#,##0", symbols).format(amount)
}

/** "19.30" (id-ID uses a dot between hour and minute). */
fun formatClock(time: LocalTime): String = DateTimeFormatter.ofPattern("HH.mm", IdLocale).format(time)

/** "19.30–20.30", or just "19.30" when there is no end time. */
fun formatTimeRange(start: LocalTime, end: LocalTime?): String =
    if (end == null) formatClock(start) else "${formatClock(start)}–${formatClock(end)}"

@StringRes
fun PlaceCategory.labelRes(): Int = when (this) {
    PlaceCategory.Museum -> R.string.category_museum
    PlaceCategory.Candi -> R.string.category_candi
    PlaceCategory.Tari -> R.string.category_tari
    PlaceCategory.Pentas -> R.string.category_pentas
    PlaceCategory.SeniMusik -> R.string.category_seni_musik
    PlaceCategory.Kerajinan -> R.string.category_kerajinan
    PlaceCategory.Lainnya -> R.string.category_lainnya
}

/** Localised name of a category. */
@Composable
fun PlaceCategory.label(): String = stringResource(labelRes())

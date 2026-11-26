package id.kelana.core.designsystem.component

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

/** Formats a date for id-ID, for example `20 Nov 2026`. Not used to render a fact's check date (product rule). */
fun formatCheckedDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale).format(date)

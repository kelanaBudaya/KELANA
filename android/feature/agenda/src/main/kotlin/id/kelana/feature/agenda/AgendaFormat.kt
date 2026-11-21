package id.kelana.feature.agenda

import java.net.URI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Dates are always formatted for id-ID (AGENTS.md conventions). */
internal val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** Evening starts at 17.00: the "Malam ini" card shows the first event starting at or after this. */
internal val EveningStart: LocalTime = LocalTime.of(17, 0)

/** "Sabtu, 21 November". */
internal fun formatDayLong(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", IdLocale).format(date)

/** "Sab". */
internal fun formatDayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, IdLocale)

/** "November 2026". */
internal fun formatMonthYear(date: LocalDate): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", IdLocale).format(date)

/** "Jumat". */
internal fun weekdayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, IdLocale)

/** The host of an `https` URL, or null for anything else (only https links are used, AGENTS.md). */
internal fun httpsHost(url: String?): String? {
    if (url == null) return null
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!"https".equals(uri.scheme, ignoreCase = true)) return null
    return uri.host?.takeIf { it.isNotBlank() }
}

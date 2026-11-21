package id.kelana.feature.rencana.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Dates are always formatted for id-ID (AGENTS.md conventions). */
internal val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "Jumat, 20 November". */
internal fun formatDayLong(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", IdLocale).format(date)

/** "Jumat 20". */
internal fun formatDayAndNumber(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE d", IdLocale).format(date)

/** "Jumat 20 November". */
internal fun formatDayNumberMonth(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE d MMMM", IdLocale).format(date)

/** "November 2026". */
internal fun formatMonthYear(date: LocalDate): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", IdLocale).format(date)

/** "Min", "Sen", ... */
internal fun weekdayShort(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, IdLocale)

/** "Senin". */
internal fun weekdayFull(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, IdLocale)

/** Range label without the day count: "Jumat 20 – Minggu 22 November" or "Jumat 20 November". */
internal fun formatRange(start: LocalDate, end: LocalDate?): String = when {
    end == null || end == start -> formatDayNumberMonth(start)
    start.month == end.month && start.year == end.year -> "${formatDayAndNumber(start)} – ${formatDayNumberMonth(end)}"
    else -> "${formatDayNumberMonth(start)} – ${formatDayNumberMonth(end)}"
}

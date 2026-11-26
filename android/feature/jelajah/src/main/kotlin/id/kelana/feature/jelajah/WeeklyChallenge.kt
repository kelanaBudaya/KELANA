package id.kelana.feature.jelajah

import id.kelana.core.model.Stamp
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Stamps needed to finish the weekly challenge. The one place this number lives. */
const val WEEKLY_STAMP_TARGET = 3

/** The challenge week runs Monday 00:00 to Sunday 23:59 in this zone. */
val ChallengeZone: ZoneId = ZoneId.of("Asia/Jakarta")

/** Stamps confirmed by the server this week against [target]. [done] may exceed [target]; the bar clamps. */
data class WeeklyProgress(val done: Int = 0, val target: Int = WEEKLY_STAMP_TARGET) {
    val fraction: Float get() = if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f)
    val isComplete: Boolean get() = target in 1..done
}

/** Counts the stamps whose time falls in the current Jakarta week (Monday 00:00 inclusive to next Monday exclusive). */
internal fun weeklyProgress(
    stamps: List<Stamp>,
    clock: Clock,
    target: Int = WEEKLY_STAMP_TARGET,
): WeeklyProgress {
    val today = clock.instant().atZone(ChallengeZone).toLocalDate()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = monday.atStartOfDay(ChallengeZone).toInstant()
    val end = monday.plusDays(7).atStartOfDay(ChallengeZone).toInstant()
    return WeeklyProgress(stamps.count { !it.stampedAt.isBefore(start) && it.stampedAt.isBefore(end) }, target)
}

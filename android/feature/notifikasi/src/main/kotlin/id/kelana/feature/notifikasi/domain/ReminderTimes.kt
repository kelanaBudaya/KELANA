package id.kelana.feature.notifikasi.domain

import id.kelana.core.model.EventSchedule
import id.kelana.core.model.ReminderTiming
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * PROVISIONAL: lead time of "Waktunya berangkat", in minutes before the show starts. The design says it is
 * calculated from the show time and travel time, but this build never reads location or live travel time,
 * so one fixed lead time stands in for it. Change it here only.
 */
const val WHEN_TO_LEAVE_LEAD_MINUTES: Long = 90

/** PROVISIONAL: shows are held in DI Yogyakarta, the only region, so their clock times are WIB. */
val SHOW_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")

/** Minutes before the show start at which the reminder of this timing fires. */
fun ReminderTiming.leadMinutes(): Long = when (this) {
    ReminderTiming.WhenToLeave -> WHEN_TO_LEAVE_LEAD_MINUTES
    ReminderTiming.OneHourBefore -> 60
    ReminderTiming.ThirtyMinutesBefore -> 30
}

object ReminderTimes {
    private const val LOOKAHEAD_DAYS = 366

    /**
     * The first moment after [now] at which a reminder of [timing] is due for [schedule]: the start of the next
     * occurrence minus the lead time. Null when there is none (a `Once` show in the past, or one whose
     * reminder moment has already passed). Occurrences are looked up one year ahead at most.
     */
    fun nextTrigger(schedule: EventSchedule, timing: ReminderTiming, now: Instant, zone: ZoneId = SHOW_ZONE): Instant? {
        val today: LocalDate = now.atZone(zone).toLocalDate()
        val lead = Duration.ofMinutes(timing.leadMinutes())
        for (offset in 0 until LOOKAHEAD_DAYS) {
            val date = today.plusDays(offset.toLong())
            if (!schedule.recurrence.occursOn(date)) continue
            val start = LocalDateTime.of(date, schedule.start).atZone(zone).toInstant()
            val trigger = start.minus(lead)
            if (trigger.isAfter(now)) return trigger
        }
        return null
    }

    /** Delay from [now] until [nextTrigger], or null when no reminder should be scheduled. */
    fun delayUntilReminder(
        schedule: EventSchedule,
        timing: ReminderTiming,
        now: Instant,
        zone: ZoneId = SHOW_ZONE,
    ): Duration? = nextTrigger(schedule, timing, now, zone)?.let { Duration.between(now, it) }
}

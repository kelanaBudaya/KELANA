package id.kelana.feature.notifikasi.domain

import id.kelana.core.model.EventSchedule
import id.kelana.core.model.Recurrence
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.notifikasi.FakeEvents
import id.kelana.feature.notifikasi.Now
import id.kelana.feature.notifikasi.data.LocalReminderScheduler
import id.kelana.feature.notifikasi.data.ReminderWork
import id.kelana.feature.notifikasi.show
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** "Now" is Friday 20 November 2026, 10.40 WIB. A show at 19.30 WIB that day starts in 8 h 50 min. */
class ReminderTimesTest {
    private val tonight = EventSchedule(Recurrence.Once(LocalDate.of(2026, 11, 20)), LocalTime.of(19, 30), null)

    private fun delay(schedule: EventSchedule, timing: ReminderTiming) =
        ReminderTimes.delayUntilReminder(schedule, timing, Now)

    @Test
    fun oneHourBeforeFiresSixtyMinutesBeforeStart() {
        assertEquals(Duration.ofHours(7).plusMinutes(50), delay(tonight, ReminderTiming.OneHourBefore))
    }

    @Test
    fun thirtyMinutesBeforeFiresThirtyMinutesBeforeStart() {
        assertEquals(Duration.ofHours(8).plusMinutes(20), delay(tonight, ReminderTiming.ThirtyMinutesBefore))
    }

    @Test
    fun whenToLeaveUsesTheFixedLeadTimeAndNothingElse() {
        assertEquals(90, WHEN_TO_LEAVE_LEAD_MINUTES)
        assertEquals(Duration.ofHours(7).plusMinutes(20), delay(tonight, ReminderTiming.WhenToLeave))
        assertEquals(
            Instant.parse("2026-11-20T11:00:00Z"), // 18.00 WIB = 19.30 minus 90 minutes
            ReminderTimes.nextTrigger(tonight, ReminderTiming.WhenToLeave, Now),
        )
    }

    @Test
    fun aShowThatAlreadyHappenedIsNotScheduled() {
        val yesterday = EventSchedule(Recurrence.Once(LocalDate.of(2026, 11, 19)), LocalTime.of(19, 30), null)
        ReminderTiming.entries.forEach { assertNull(delay(yesterday, it)) }
    }

    @Test
    fun aReminderMomentThatHasAlreadyPassedIsNotScheduled() {
        // The show is at 11.00: one hour before was 10.00, already past; 30 minutes before is 10.30, also past.
        val soon = EventSchedule(Recurrence.Once(LocalDate.of(2026, 11, 20)), LocalTime.of(11, 0), null)
        assertNull(delay(soon, ReminderTiming.OneHourBefore))
        assertNull(delay(soon, ReminderTiming.ThirtyMinutesBefore))
        assertNull(delay(soon, ReminderTiming.WhenToLeave))
    }

    @Test
    fun aReminderMomentExactlyNowIsNotScheduled() {
        val atMinus30 = EventSchedule(Recurrence.Once(LocalDate.of(2026, 11, 20)), LocalTime.of(11, 10), null)
        assertNull(delay(atMinus30, ReminderTiming.ThirtyMinutesBefore)) // trigger == now
        assertEquals(Duration.ofMinutes(1), ReminderTimes.delayUntilReminder(atMinus30, ReminderTiming.ThirtyMinutesBefore, Now.minusSeconds(60)))
    }

    @Test
    fun aRecurringShowWhoseTodayReminderPassedUsesTheNextOccurrence() {
        val daily = EventSchedule(Recurrence.Daily, LocalTime.of(11, 0), null)
        // Tomorrow 10.00 WIB is 23 h 20 min after 10.40 WIB today.
        assertEquals(Duration.ofHours(23).plusMinutes(20), delay(daily, ReminderTiming.OneHourBefore))
        val weekly = EventSchedule(Recurrence.Weekly(DayOfWeek.SATURDAY), LocalTime.of(19, 30), null)
        assertEquals(Instant.parse("2026-11-21T12:30:00Z").minus(Duration.ofHours(1)), ReminderTimes.nextTrigger(weekly, ReminderTiming.OneHourBefore, Now))
    }

    @Test
    fun aReminderCanFallOnTheDayBeforeTheShow() {
        val earlyMorning = EventSchedule(Recurrence.Once(LocalDate.of(2026, 11, 21)), LocalTime.of(0, 30), null)
        assertEquals(Instant.parse("2026-11-20T16:30:00Z"), ReminderTimes.nextTrigger(earlyMorning, ReminderTiming.OneHourBefore, Now))
    }
}

private class RecordingWork : ReminderWork {
    /** Mirrors WorkManager's unique work with REPLACE: one entry per event id. */
    val pending = linkedMapOf<String, Pair<ReminderTiming, Duration>>()
    var enqueueCalls = 0

    override fun enqueue(eventId: String, timing: ReminderTiming, delay: Duration) {
        enqueueCalls++
        pending[eventId] = timing to delay
    }

    override fun cancel(eventId: String) {
        pending.remove(eventId)
    }
}

class LocalReminderSchedulerTest {
    private val clock = Clock.fixed(Now, ZoneOffset.UTC)
    private val work = RecordingWork()

    private fun scheduler(vararg events: id.kelana.core.model.Event) = LocalReminderScheduler(FakeEvents(events.toList()), work, clock)

    @Test
    fun schedulesOneWorkWithTheDelayOfTheChosenTiming() = runTest {
        scheduler(show()).scheduleShowReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        assertEquals(mapOf("roro" to (ReminderTiming.OneHourBefore to Duration.ofHours(7).plusMinutes(50))), work.pending)
    }

    @Test
    fun choosingAgainReplacesTheEarlierReminderOfTheSameEvent() = runTest {
        val s = scheduler(show())
        s.scheduleShowReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        s.scheduleShowReminder("roro", ReminderChoice(ReminderTiming.ThirtyMinutesBefore, true))
        assertEquals(2, work.enqueueCalls)
        assertEquals(1, work.pending.size)
        assertEquals(ReminderTiming.ThirtyMinutesBefore, work.pending.getValue("roro").first)
    }

    @Test
    fun remindersOfDifferentEventsDoNotReplaceEachOther() = runTest {
        val s = scheduler(show(id = "a"), show(id = "b"))
        s.scheduleShowReminder("a", ReminderChoice(ReminderTiming.OneHourBefore, false))
        s.scheduleShowReminder("b", ReminderChoice(ReminderTiming.OneHourBefore, false))
        assertEquals(setOf("a", "b"), work.pending.keys)
    }

    @Test
    fun aPastShowIsNotScheduledAndClearsTheEarlierReminder() = runTest {
        val s = scheduler(show(), show(id = "old", day = LocalDate.of(2026, 11, 19)))
        s.scheduleShowReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        assertTrue(work.pending.containsKey("roro"))
        s.scheduleShowReminder("old", ReminderChoice(ReminderTiming.OneHourBefore, false))
        assertTrue(!work.pending.containsKey("old"))
        assertEquals(1, work.enqueueCalls)
    }

    @Test
    fun aShowTimeWithoutACheckDateIsNotScheduled() = runTest {
        scheduler(show(checkedAt = null)).scheduleShowReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        assertTrue(work.pending.isEmpty())
    }

    @Test
    fun anUnknownEventIsNotScheduled() = runTest {
        scheduler().scheduleShowReminder("nope", ReminderChoice(ReminderTiming.WhenToLeave, false))
        assertTrue(work.pending.isEmpty())
    }

    @Test
    fun cancelRemovesTheReminder() = runTest {
        val s = scheduler(show())
        s.scheduleShowReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        s.cancelShowReminder("roro")
        assertTrue(work.pending.isEmpty())
    }
}

package id.kelana.feature.rencana.data

import id.kelana.core.model.PlanChoices
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TripPlan
import id.kelana.feature.rencana.ui.FakeCache
import id.kelana.feature.rencana.ui.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RencanaUserDataCleanerTest {

    private val day = LocalDate.of(2026, 11, 20)

    private fun trip() = TripPlan(PlanChoices(day, day, TravelGroup.entries.first(), emptySet()), emptyList(), day)

    @Test
    fun cancelsEveryChosenReminder_andClearsThePlan() = runTest {
        val cache = FakeCache()
        cache.saveReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        cache.saveReminder("wayang", ReminderChoice(ReminderTiming.WhenToLeave, true))
        cache.tripState.value = trip()
        val scheduler = FakeReminderScheduler()

        RencanaUserDataCleaner(cache, scheduler).clear()

        assertEquals(setOf("roro", "wayang"), scheduler.cancelCalls.toSet())
        assertNull(cache.trip.first())
    }

    @Test
    fun aFailingCancel_stillClearsThePlan_andIsReported() = runTest {
        val cache = FakeCache()
        cache.saveReminder("roro", ReminderChoice(ReminderTiming.OneHourBefore, false))
        cache.tripState.value = trip()
        val scheduler = object : id.kelana.core.model.ReminderScheduler {
            override suspend fun scheduleShowReminder(eventId: String, choice: ReminderChoice) = Unit
            override suspend fun cancelShowReminder(eventId: String) = throw IllegalStateException("work")
        }

        val thrown = runCatching { RencanaUserDataCleaner(cache, scheduler).clear() }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertNull(cache.trip.first())
    }
}

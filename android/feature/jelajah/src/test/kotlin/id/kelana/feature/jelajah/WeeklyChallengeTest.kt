package id.kelana.feature.jelajah

import id.kelana.core.model.Stamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** 2026-11-23 is a Monday; the week is 23 to 29 Nov in Asia/Jakarta (UTC+7). */
class WeeklyChallengeTest {
    private fun clockAt(utc: String) = Clock.fixed(Instant.parse(utc), ZoneOffset.UTC)
    private fun stamp(utc: String) = Stamp("p", Instant.parse(utc))
    private val wed = clockAt("2026-11-25T05:00:00Z")

    @Test fun empty() {
        val p = weeklyProgress(emptyList(), wed)
        assertEquals(0, p.done)
        assertEquals(WEEKLY_STAMP_TARGET, p.target)
        assertEquals(0f, p.fraction, 0f)
    }

    @Test fun countsThisWeekAndExcludesLastWeek() {
        val p = weeklyProgress(
            listOf(stamp("2026-11-24T03:00:00Z"), stamp("2026-11-25T01:00:00Z"), stamp("2026-11-18T03:00:00Z")),
            wed,
        )
        assertEquals(2, p.done)
    }

    @Test fun mondayMidnightJakartaIsInclusive() {
        // Monday 00:00 Jakarta = Sunday 17:00 UTC.
        val p = weeklyProgress(listOf(stamp("2026-11-22T17:00:00Z"), stamp("2026-11-22T16:59:59Z")), wed)
        assertEquals(1, p.done)
    }

    @Test fun sundayEndOfWeekInclusiveNextMondayExcluded() {
        // Sunday 23:59 Jakarta = 16:59 UTC; next Monday 00:00 Jakarta = Sunday 17:00 UTC.
        val p = weeklyProgress(listOf(stamp("2026-11-29T16:59:00Z"), stamp("2026-11-29T17:00:00Z")), wed)
        assertEquals(1, p.done)
    }

    @Test fun sundayEveningUtcIsAlreadyMondayInJakarta() {
        // Sunday 29 Nov 20:00 UTC is Monday 30 Nov 03:00 in Jakarta: a new week.
        val now = clockAt("2026-11-29T20:00:00Z")
        val p = weeklyProgress(listOf(stamp("2026-11-29T20:00:00Z"), stamp("2026-11-25T01:00:00Z")), now)
        assertEquals(1, p.done)
    }

    @Test fun moreThanTargetClampsBar() {
        val p = weeklyProgress(List(5) { stamp("2026-11-24T03:00:00Z") }, wed)
        assertEquals(5, p.done)
        assertEquals(1f, p.fraction, 0f)
        assertTrue(p.isComplete)
    }

    @Test fun belowTargetIsNotComplete() {
        assertFalse(weeklyProgress(listOf(stamp("2026-11-24T03:00:00Z")), wed).isComplete)
    }
}

package id.kelana.core.designsystem.component

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VerifiedBadgeTest {
    @Test
    fun formatsCheckDateWithIndonesianMonth() {
        assertEquals("20 Nov 2026", formatCheckedDate(LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun doesNotPadSingleDigitDay() {
        assertEquals("5 Nov 2026", formatCheckedDate(LocalDate.of(2026, 11, 5)))
    }

    @Test
    fun usesIndonesianNameForMay() {
        // Indonesian "Mei" is spelled the same in short and long form.
        assertEquals("1 Mei 2026", formatCheckedDate(LocalDate.of(2026, 5, 1)))
    }
}

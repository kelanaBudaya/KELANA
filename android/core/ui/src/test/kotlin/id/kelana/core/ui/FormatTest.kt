package id.kelana.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FormatTest {
    @Test
    fun rupiahUsesDotGrouping() {
        assertEquals("Rp150.000", formatRupiah(150_000))
        assertEquals("Rp5.000", formatRupiah(5_000))
        assertEquals("Rp0", formatRupiah(0))
    }

    @Test
    fun timeRangeUsesDotAndEnDash() {
        assertEquals("19.30–20.30", formatTimeRange(LocalTime.of(19, 30), LocalTime.of(20, 30)))
        assertEquals("09.00", formatTimeRange(LocalTime.of(9, 0), null))
    }
}

package id.kelana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class StampTest {
    @Test
    fun rejectReasons_haveTheExactServerWireValues() {
        assertEquals(
            listOf(
                "too_far", "expired", "future_timestamp", "low_accuracy", "mock_location",
                "integrity_failed", "place_unknown", "radius_not_set", "bad_request",
            ),
            StampRejectReason.entries.map { it.wire },
        )
    }

    @Test
    fun fromWire_roundTripsAndRejectsUnknown() {
        StampRejectReason.entries.forEach { assertEquals(it, StampRejectReason.fromWire(it.wire)) }
        assertNull(StampRejectReason.fromWire("nope"))
        assertNull(StampRejectReason.fromWire(null))
    }

    @Test
    fun queueExpiry_isThirtyMinutes() {
        assertEquals(30L, STAMP_QUEUE_EXPIRY_MINUTES)
    }

    @Test
    fun stampRequest_toStringHidesTheFix() {
        val text = StampRequest("p", -7.805, 110.3642, 20.0, false, Instant.EPOCH).toString()
        assertFalse(text.contains("7.805"))
        assertFalse(text.contains("110.36"))
    }
}

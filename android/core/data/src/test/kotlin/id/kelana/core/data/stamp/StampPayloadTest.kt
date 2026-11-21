package id.kelana.core.data.stamp

import id.kelana.core.model.StampRequest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

/**
 * The expected nonces below were produced by running the server's own `computeNonce` (verify.ts) with Deno
 * on the same inputs; see supabase/functions/verify-stamp/verify.ts.
 */
class StampPayloadTest {
    private val placeId = "123e4567-e89b-42d3-a456-426614174000"
    private val savedLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(savedLocale)

    @Test
    fun nonce_matchesServerVector_negativeLatitude() {
        assertEquals(
            "123e4567-e89b-42d3-a456-426614174000|-7.805000|110.364200|20.0|2026-11-21T10:15:30.123Z",
            stampCanonical(placeId, -7.805, 110.3642, 20.0, "2026-11-21T10:15:30.123Z"),
        )
        assertEquals(
            "IIz-pBCv3Z8ZCw4D5vuvKKlrJP2P4YRha_xYNuTjKQg",
            stampNonce(placeId, -7.805, 110.3642, 20.0, "2026-11-21T10:15:30.123Z"),
        )
    }

    @Test
    fun nonce_negativeZeroPrintsUnsigned() {
        assertEquals(
            "T_hR0ws3IzyDdjgtth4IIw3elYP94R-ZA6dfgERJa1Y",
            stampNonce(placeId, -0.0, 0.0, 0.0, "2026-11-21T10:15:30.000Z"),
        )
        // A value that rounds to zero from below must also print without a sign (server normalises "-0.000000").
        assertEquals(
            "H2A76dh_H-_6gGhfWj3I1z2XrSOl7BFQfZ5QuMgVztE",
            stampNonce(placeId, -1e-7, 110.491399, 8.5, "2026-11-21T00:00:00.007Z"),
        )
        assertEquals("0.000000", fixedDecimal(-0.0, 6))
        assertEquals("0.000000", fixedDecimal(-1e-7, 6))
    }

    @Test
    fun nonce_boundaryValues() {
        assertEquals(
            "9T8MBnguXIa-TnvdPZB6VWVBBEjp1Ilj_B57uhO4xAA",
            stampNonce(
                "00000000-0000-4000-8000-000000000001", 89.999999, -179.999999, 100.0, "2026-12-31T23:59:59.999Z",
            ),
        )
    }

    @Test
    fun nonce_isUnpaddedBase64Url_43Chars() {
        val n = stampNonce(placeId, -7.805, 110.3642, 20.0, "2026-11-21T10:15:30.123Z")
        assertEquals(43, n.length)
        assertTrue(n.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun formatting_usesDotUnderIndonesianDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        assertEquals("-7.805000", fixedDecimal(-7.805, 6))
        assertEquals("20.0", fixedDecimal(20.0, 1))
        assertEquals(
            "IIz-pBCv3Z8ZCw4D5vuvKKlrJP2P4YRha_xYNuTjKQg",
            stampNonce(placeId, -7.805, 110.3642, 20.0, "2026-11-21T10:15:30.123Z"),
        )
        val payload = payload(Instant.parse("2026-11-21T10:15:30.123Z"))!!
        assertEquals("2026-11-21T10:15:30.123Z", payload.capturedAt)
    }

    @Test
    fun capturedAt_isUtcWithExactlyThreeMillisecondDigits() {
        assertEquals("2026-11-21T10:15:30.000Z", formatCapturedAt(Instant.parse("2026-11-21T10:15:30Z")))
        assertEquals("2026-11-21T10:15:30.120Z", formatCapturedAt(Instant.parse("2026-11-21T10:15:30.12Z")))
        // Sub-millisecond precision is cut, never rounded up into the next millisecond.
        assertEquals("2026-11-21T10:15:30.123Z", formatCapturedAt(Instant.parse("2026-11-21T10:15:30.123999Z")))
    }

    @Test
    fun payload_roundsBeforeHashingAndSending() {
        val p = StampPayload.from(
            placeId = placeId.uppercase(),
            lat = -7.80500049,
            lng = 110.36420051,
            accuracyM = 19.96,
            isMock = false,
            capturedAt = Instant.parse("2026-11-21T10:15:30.123Z"),
        )!!
        assertEquals(placeId, p.placeId)
        assertEquals(-7.805, p.lat, 0.0)
        assertEquals(110.364201, p.lng, 0.0)
        assertEquals(20.0, p.accuracyM, 0.0)
        // The nonce is computed from the same rounded numbers that are sent.
        assertEquals(stampNonce(placeId, p.lat, p.lng, p.accuracyM, p.capturedAt), p.nonce())
    }

    @Test
    fun payload_negativeZeroLatitudeIsSentAsPositiveZero() {
        val p = StampPayload.from(placeId, -1e-9, 0.0, 5.0, false, Instant.parse("2026-11-21T10:15:30.123Z"))!!
        assertEquals(0.0, p.lat, 0.0)
        assertFalse(1.0 / p.lat < 0) // not -0.0
    }

    @Test
    fun body_serializesTheContractFieldNames() {
        val body = payload(Instant.parse("2026-11-21T10:15:30.123Z"))!!.toBody("tok")
        val json = Json.encodeToString(VerifyStampBody.serializer(), body)
        assertEquals(
            """{"placeId":"$placeId","lat":-7.805,"lng":110.3642,"accuracyM":20.0,""" +
                """"capturedAt":"2026-11-21T10:15:30.123Z","isMock":false,"integrityToken":"tok"}""",
            json,
        )
    }

    @Test
    fun invalidFixesAreRefused() {
        val t = Instant.parse("2026-11-21T10:15:30.123Z")
        assertNull(StampPayload.from(placeId, Double.NaN, 0.0, 5.0, false, t))
        assertNull(StampPayload.from(placeId, 0.0, Double.POSITIVE_INFINITY, 5.0, false, t))
        assertNull(StampPayload.from(placeId, 91.0, 0.0, 5.0, false, t))
        assertNull(StampPayload.from(placeId, 0.0, 181.0, 5.0, false, t))
        assertNull(StampPayload.from(placeId, 0.0, 0.0, -1.0, false, t))
        assertNotNull(StampPayload.from(placeId, 90.0, -180.0, 0.0, false, t))
    }

    @Test
    fun toStrings_doNotLeakLocationOrToken() {
        val p = payload(Instant.parse("2026-11-21T10:15:30.123Z"))!!
        val text = p.toString() + p.toBody("secret-token") + IntegrityTokenResult.Token("secret-token")
        assertFalse(text.contains("7.805"))
        assertFalse(text.contains("secret-token"))
        val req = StampRequest(placeId, -7.805, 110.3642, 20.0, false, Instant.EPOCH)
        assertFalse(req.toString().contains("7.805"))
    }

    private fun payload(at: Instant) = StampPayload.from(placeId, -7.805, 110.3642, 20.0, false, at)
}

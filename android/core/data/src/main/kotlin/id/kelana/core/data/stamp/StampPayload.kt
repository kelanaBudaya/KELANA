package id.kelana.core.data.stamp

import id.kelana.core.model.StampRequest
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/**
 * Body of `POST /functions/v1/verify-stamp` (see supabase/contract/kelana.openapi.yaml). Never log it:
 * it holds the location fix.
 */
@Serializable
data class VerifyStampBody(
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
    val capturedAt: String,
    val isMock: Boolean,
    val integrityToken: String,
) {
    override fun toString(): String = "VerifyStampBody(redacted)"
}

/**
 * A stamp request in the exact form the server hashes: lat/lng rounded to 6 decimals, accuracy to 1 decimal
 * and capturedAt as `yyyy-MM-ddTHH:mm:ss.SSSZ` (UTC). Rounding happens here, before both the nonce and the
 * request body, so client and server format the same numbers. All number text goes through BigDecimal, so a
 * default id-ID locale cannot produce a comma.
 */
class StampPayload private constructor(
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
    val capturedAt: String,
    val isMock: Boolean,
) {
    /** Nonce for Play Integrity `setNonce`, identical to the server's `computeNonce`. */
    fun nonce(): String = stampNonce(placeId, lat, lng, accuracyM, capturedAt)

    fun toBody(integrityToken: String): VerifyStampBody =
        VerifyStampBody(placeId, lat, lng, accuracyM, capturedAt, isMock, integrityToken)

    override fun toString(): String = "StampPayload(redacted)"

    companion object {
        /** Null when the fix cannot be a valid request (non-finite, negative accuracy, out of range). */
        fun from(
            placeId: String,
            lat: Double,
            lng: Double,
            accuracyM: Double,
            isMock: Boolean,
            capturedAt: Instant,
        ): StampPayload? {
            if (!lat.isFinite() || !lng.isFinite() || !accuracyM.isFinite()) return null
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180 || accuracyM < 0) return null
            return StampPayload(
                placeId = placeId.lowercase(Locale.ROOT),
                lat = round(lat, 6),
                lng = round(lng, 6),
                accuracyM = round(accuracyM, 1),
                capturedAt = formatCapturedAt(capturedAt),
                isMock = isMock,
            )
        }

        fun from(request: StampRequest): StampPayload? = from(
            request.placeId, request.lat, request.lng, request.accuracyM, request.isMock, request.capturedAt,
        )
    }
}

private val CAPTURED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)

/** UTC with exactly 3 millisecond digits, e.g. `2026-11-21T10:15:30.123Z`. */
fun formatCapturedAt(instant: Instant): String = CAPTURED_AT_FORMAT.format(instant)

private fun round(value: Double, digits: Int): Double = decimal(value, digits).toDouble()

/** Half-up rounding on the shortest decimal text of the double (BigDecimal has no negative zero). */
private fun decimal(value: Double, digits: Int): BigDecimal =
    BigDecimal(value.toString()).setScale(digits, RoundingMode.HALF_UP)

/** Fixed-point text with `.` as the separator; a negative zero prints as `0.000000`. */
fun fixedDecimal(value: Double, digits: Int): String = decimal(value, digits).toPlainString()

/** `placeId|lat(6dp)|lng(6dp)|accuracyM(1dp)|capturedAt`, same as `canonicalString` in verify.ts. */
fun stampCanonical(placeId: String, lat: Double, lng: Double, accuracyM: Double, capturedAt: String): String =
    listOf(placeId, fixedDecimal(lat, 6), fixedDecimal(lng, 6), fixedDecimal(accuracyM, 1), capturedAt)
        .joinToString("|")

/** `base64url_no_padding(sha256_utf8(canonical))`, same as `computeNonce` in verify.ts. */
fun stampNonce(placeId: String, lat: Double, lng: Double, accuracyM: Double, capturedAt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(stampCanonical(placeId, lat, lng, accuracyM, capturedAt).toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}

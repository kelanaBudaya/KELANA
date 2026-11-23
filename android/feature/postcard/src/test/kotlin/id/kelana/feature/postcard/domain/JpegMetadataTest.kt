package id.kelana.feature.postcard.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * The EXIF-strip guarantee. The photo is re-encoded by `Bitmap.compress` (writes no EXIF); [JpegMetadata.strip]
 * is the second line of defence and is what these JVM tests can exercise: a JPEG that carries an EXIF block with a
 * GPS IFD, XMP and a comment must come out without any of them.
 */
class JpegMetadataTest {
    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0xFF)
        out.write(marker)
        val length = payload.size + 2
        out.write(length shr 8)
        out.write(length and 0xFF)
        out.write(payload)
        return out.toByteArray()
    }

    /** A tiny TIFF/EXIF payload with a GPS IFD pointer, latitude and longitude rationals (values are fake). */
    private fun exifWithGps(): ByteArray {
        val header = "Exif".toByteArray(Charsets.US_ASCII) + byteArrayOf(0, 0)
        val tiff = "MM".toByteArray() + byteArrayOf(0, 0x2A, 0, 0, 0, 8) +
            "GPSLatitude=7.8053S;GPSLongitude=110.3644E;GPSVersionID".toByteArray(Charsets.US_ASCII)
        return header + tiff
    }

    private val jfif = segment(0xE0, "JFIF".toByteArray() + byteArrayOf(0, 1, 1, 0, 0, 1, 0, 1, 0, 0))
    private val exif = segment(0xE1, exifWithGps())
    private val xmp = segment(0xE1, "http://ns.adobe.com/xap/1.0/\u0000<x:xmpmeta GPSLatitude/>".toByteArray())
    private val icc = segment(0xE2, "ICC_PROFILE".toByteArray() + ByteArray(20))
    private val comment = segment(0xFE, "taken at home, secret".toByteArray())
    private val dqt = segment(0xDB, ByteArray(65) { 8 })
    private val sof = segment(0xC0, byteArrayOf(8, 0, 1, 0, 1, 1, 1, 0x11, 0))
    private val sos = segment(0xDA, byteArrayOf(1, 1, 0, 0, 0x3F, 0))
    private val scan = byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0x00, 0x56) // entropy data with a stuffed 0xFF
    private val soi = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val eoi = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    private fun jpeg(vararg parts: ByteArray): ByteArray = parts.fold(ByteArray(0)) { acc, p -> acc + p }

    private fun contains(haystack: ByteArray, needle: String): Boolean =
        String(haystack, Charsets.ISO_8859_1).contains(needle)

    @Test fun exif_gps_xmp_and_comment_are_removed() {
        val input = jpeg(soi, jfif, exif, xmp, icc, comment, dqt, sof, sos, scan, eoi)
        assertTrue(contains(input, "GPSLatitude")) // the fixture really carries the location

        val out = JpegMetadata.strip(input)
        assertNotNull(out)
        assertFalse(contains(out!!, "Exif"))
        assertFalse(contains(out, "GPS"))
        assertFalse(contains(out, "xmpmeta"))
        assertFalse(contains(out, "secret"))
        assertFalse(contains(out, "ICC_PROFILE"))
        assertEquals(listOf<Int>(), appMarkersAfterJfif(out))
    }

    @Test fun image_data_and_the_jfif_header_are_kept_byte_for_byte() {
        val input = jpeg(soi, jfif, exif, dqt, sof, sos, scan, eoi)
        val expected = jpeg(soi, jfif, dqt, sof, sos, scan, eoi)
        assertArrayEquals(expected, JpegMetadata.strip(input))
    }

    @Test fun a_jpeg_without_metadata_comes_out_unchanged() {
        val clean = jpeg(soi, jfif, dqt, sof, sos, scan, eoi)
        assertArrayEquals(clean, JpegMetadata.strip(clean))
    }

    @Test fun exif_placed_after_other_segments_is_still_removed() {
        val input = jpeg(soi, dqt, exif, sof, sos, scan, eoi)
        val out = JpegMetadata.strip(input)!!
        assertFalse(contains(out, "GPS"))
        assertArrayEquals(jpeg(soi, dqt, sof, sos, scan, eoi), out)
    }

    @Test fun anything_that_is_not_a_well_formed_jpeg_is_refused() {
        assertNull(JpegMetadata.strip(ByteArray(0)))
        assertNull(JpegMetadata.strip("not a jpeg".toByteArray()))
        assertNull(JpegMetadata.strip(jpeg(soi, exif.copyOf(exif.size - 5)))) // truncated segment
        assertNull(JpegMetadata.strip(jpeg(soi, jfif, dqt))) // no scan, no end
    }

    /** APPn markers (0xE1..0xEF) present in the header part of [jpeg], in order. */
    private fun appMarkersAfterJfif(jpeg: ByteArray): List<Int> {
        val found = mutableListOf<Int>()
        var i = 2
        while (i + 3 < jpeg.size && (jpeg[i].toInt() and 0xFF) == 0xFF) {
            val marker = jpeg[i + 1].toInt() and 0xFF
            if (marker == 0xDA) break
            if (marker in 0xE1..0xEF || marker == 0xFE) found += marker
            val length = ((jpeg[i + 2].toInt() and 0xFF) shl 8) or (jpeg[i + 3].toInt() and 0xFF)
            i += 2 + length
        }
        return found
    }
}

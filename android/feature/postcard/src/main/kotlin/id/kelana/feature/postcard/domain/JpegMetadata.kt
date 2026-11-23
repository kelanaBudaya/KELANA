package id.kelana.feature.postcard.domain

import java.io.ByteArrayOutputStream

/**
 * Removes every metadata segment from a JPEG: APP1 (EXIF, which carries GPS, and XMP), the other APPn segments
 * except the JFIF header (APP0), and comments. Pixels are untouched.
 *
 * This is the second line of defence. The first is that the photo is decoded and re-encoded with
 * `Bitmap.compress`, which writes no EXIF at all. Location must never leave the device through a photo, so a
 * result that is not a well-formed JPEG is refused ([strip] returns null) instead of being passed on.
 */
internal object JpegMetadata {
    private const val FF = 0xFF
    private const val SOI = 0xD8
    private const val EOI = 0xD9
    private const val SOS = 0xDA
    private const val APP0 = 0xE0
    private const val APP15 = 0xEF
    private const val COM = 0xFE

    fun strip(jpeg: ByteArray): ByteArray? {
        if (jpeg.size < 4 || jpeg[0].u() != FF || jpeg[1].u() != SOI) return null
        val out = ByteArrayOutputStream(jpeg.size)
        out.write(FF)
        out.write(SOI)
        var i = 2
        while (i < jpeg.size) {
            if (jpeg[i].u() != FF) return null
            // Fill bytes: any number of 0xFF before the marker code.
            var j = i + 1
            while (j < jpeg.size && jpeg[j].u() == FF) j++
            if (j >= jpeg.size) return null
            val marker = jpeg[j].u()
            if (marker == EOI) {
                out.write(FF)
                out.write(EOI)
                return out.toByteArray()
            }
            if (marker == SOS) {
                // Entropy-coded data runs to the end; copy it as is.
                out.write(jpeg, i, jpeg.size - i)
                return out.toByteArray()
            }
            // Standalone markers have no length (TEM and RSTn).
            if (marker == 0x01 || marker in 0xD0..0xD7) {
                out.write(FF)
                out.write(marker)
                i = j + 1
                continue
            }
            if (j + 2 >= jpeg.size) return null
            val length = (jpeg[j + 1].u() shl 8) or jpeg[j + 2].u()
            val end = j + 1 + length
            if (length < 2 || end > jpeg.size) return null
            val isMetadata = marker == COM || (marker in (APP0 + 1)..APP15)
            if (!isMetadata) out.write(jpeg, i, end - i)
            i = end
        }
        return null // no SOS or EOI: not a complete JPEG
    }

    private fun Byte.u(): Int = toInt() and 0xFF
}

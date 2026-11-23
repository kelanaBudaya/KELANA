package id.kelana.feature.postcard.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Pure size and orientation maths for the photo encoder (no Android classes, so it is unit tested on the JVM). */
internal object PhotoMath {
    /** Longest side of the uploaded photo, in pixels. */
    const val MAX_SIDE: Int = 1600

    /** Encoded size limit. The bucket accepts 5 MB; stay a little under. */
    const val MAX_BYTES: Int = 4_500_000

    /** Target size: the long side is at most [maxSide], the ratio is kept, a small photo is never enlarged. */
    fun fitWithin(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val longSide = max(width, height)
        if (longSide <= maxSide) return width to height
        val scale = maxSide.toDouble() / longSide
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    /** Largest power of two that keeps the decoded long side at or above [maxSide] (cheap first downscale). */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        var sample = 1
        var long = max(width, height)
        while (long / 2 >= maxSide) {
            long /= 2
            sample *= 2
        }
        return sample
    }

    /** Rotation in degrees (clockwise) followed by an optional horizontal flip. */
    data class Orientation(val rotationDegrees: Int, val flipHorizontal: Boolean)

    /** Maps the EXIF orientation tag (1..8) to the transform that makes the pixels upright. Unknown values: none. */
    fun orientationOf(exifOrientation: Int): Orientation = when (exifOrientation) {
        2 -> Orientation(0, true)
        3 -> Orientation(180, false)
        4 -> Orientation(180, true)
        5 -> Orientation(90, true)
        6 -> Orientation(90, false)
        7 -> Orientation(270, true)
        8 -> Orientation(270, false)
        else -> Orientation(0, false)
    }
}

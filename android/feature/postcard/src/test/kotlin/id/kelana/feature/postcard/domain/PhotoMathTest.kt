package id.kelana.feature.postcard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoMathTest {
    @Test fun long_side_is_capped_at_1600_and_the_ratio_is_kept() {
        assertEquals(1600 to 1200, PhotoMath.fitWithin(4000, 3000))
        assertEquals(1200 to 1600, PhotoMath.fitWithin(3000, 4000))
        assertEquals(1600 to 900, PhotoMath.fitWithin(1920, 1080))
    }

    @Test fun small_photos_are_not_enlarged() {
        assertEquals(800 to 600, PhotoMath.fitWithin(800, 600))
        assertEquals(1600 to 1600, PhotoMath.fitWithin(1600, 1600))
    }

    @Test fun extreme_ratios_never_collapse_to_zero() {
        assertEquals(1600 to 1, PhotoMath.fitWithin(100_000, 10))
    }

    @Test fun sample_size_keeps_the_decoded_photo_at_or_above_the_target() {
        assertEquals(1, PhotoMath.sampleSize(1600, 1200))
        assertEquals(1, PhotoMath.sampleSize(3000, 2000))
        assertEquals(2, PhotoMath.sampleSize(4000, 3000))
        assertEquals(4, PhotoMath.sampleSize(8000, 6000))
    }

    @Test fun exif_orientation_maps_to_rotation_and_flip() {
        assertEquals(PhotoMath.Orientation(0, false), PhotoMath.orientationOf(1))
        assertEquals(PhotoMath.Orientation(90, false), PhotoMath.orientationOf(6))
        assertEquals(PhotoMath.Orientation(180, false), PhotoMath.orientationOf(3))
        assertEquals(PhotoMath.Orientation(270, false), PhotoMath.orientationOf(8))
        assertEquals(PhotoMath.Orientation(0, true), PhotoMath.orientationOf(2))
        assertEquals(PhotoMath.Orientation(0, false), PhotoMath.orientationOf(0)) // unknown
    }
}

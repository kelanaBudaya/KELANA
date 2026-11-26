package id.kelana.feature.tempat.ui

import id.kelana.core.ui.tiktok.TikTokPlayerError
import id.kelana.feature.tempat.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceVideosSectionTest {
    @Test
    fun error_types_map_to_short_messages() {
        assertEquals(R.string.tempat_videos_card_error_unavailable, videoErrorMessageRes(TikTokPlayerError.InvalidVideo))
        assertEquals(R.string.tempat_videos_card_error_playback, videoErrorMessageRes(TikTokPlayerError.ServerError))
        assertEquals(R.string.tempat_videos_card_error_playback, videoErrorMessageRes(TikTokPlayerError.PlaybackError))
        assertEquals(R.string.tempat_videos_card_error, videoErrorMessageRes(TikTokPlayerError.AutoplayError))
        assertEquals(R.string.tempat_videos_card_error, videoErrorMessageRes(null))
    }
}

package id.kelana.core.ui.tiktok

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokEmbedTest {
    @Test
    fun default_url_uses_kelana_defaults() {
        assertEquals(
            "https://www.tiktok.com/player/v1/7123456789012345678?controls=1&progress_bar=1&play_button=1&" +
                "volume_control=1&fullscreen_button=1&timestamp=1&loop=0&autoplay=0&music_info=0&description=0&" +
                "rel=0&native_context_menu=0&closed_caption=1&force_muted=0",
            tikTokPlayerUrl("7123456789012345678"),
        )
    }

    @Test
    fun options_map_to_bits() {
        val url = tikTokPlayerUrl("1", TikTokPlayerOptions(autoplay = true, controls = false, forceMuted = true))
        assertTrue(url.contains("autoplay=1"))
        assertTrue(url.contains("controls=0"))
        assertTrue(url.contains("force_muted=1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_numeric_id_is_refused() {
        tikTokPlayerUrl("12\"><script>")
    }

    @Test
    fun video_id_validation() {
        assertTrue(isValidTikTokVideoId("123"))
        assertFalse(isValidTikTokVideoId(""))
        assertFalse(isValidTikTokVideoId("12a"))
        assertFalse(isValidTikTokVideoId("1".repeat(33)))
    }

    @Test
    fun wrapper_contains_iframe_permissions_and_message_filter() {
        val html = tikTokWrapperHtml("42")
        assertTrue(html.contains("src=\"https://www.tiktok.com/player/v1/42?controls=1&amp;progress_bar=1"))
        assertTrue(html.contains("allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\""))
        assertTrue(html.contains("e.source!==f.contentWindow"))
        assertTrue(html.contains("'x-tiktok-player'"))
        assertTrue(html.contains("window.KelanaTikTok"))
    }

    @Test
    fun command_scripts() {
        assertEquals("kelanaPost('play');", tikTokCommandScript(TikTokPlayerCommand.Play))
        assertEquals("kelanaPost('unMute');", tikTokCommandScript(TikTokPlayerCommand.UnMute))
    }

    @Test
    fun only_tiktok_hosts_stay_in_the_webview() {
        assertTrue(isTikTokUrl("https://www.tiktok.com/player/v1/1"))
        assertTrue(isTikTokUrl("https://tiktok.com/@a"))
        assertTrue(isTikTokUrl("https://m.tiktok.com/x"))
        assertFalse(isTikTokUrl("https://evil-tiktok.com/x"))
        assertFalse(isTikTokUrl("https://tiktok.com.evil.io/x"))
        assertFalse(isTikTokUrl("http://www.tiktok.com/x"))
        assertFalse(isTikTokUrl("intent://x"))
        assertFalse(isTikTokUrl("not a url"))
    }

    @Test
    fun message_codes_are_mapped() {
        assertEquals(TikTokPlayerState.Playing, TikTokPlayerState.fromCode(1))
        assertEquals(TikTokPlayerState.Init, TikTokPlayerState.fromCode(-1))
        assertNull(TikTokPlayerState.fromCode(9))
        assertEquals(TikTokPlayerError.InvalidVideo, TikTokPlayerError.fromCode(1001))
        assertEquals(TikTokPlayerError.ServerError, TikTokPlayerError.fromCode(2001))
        assertEquals(TikTokPlayerError.PlaybackError, TikTokPlayerError.fromCode(3001))
        assertEquals(TikTokPlayerError.Unknown, TikTokPlayerError.fromCode(5))
    }
}

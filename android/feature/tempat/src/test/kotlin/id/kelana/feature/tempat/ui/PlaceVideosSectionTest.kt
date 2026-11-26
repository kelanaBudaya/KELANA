package id.kelana.feature.tempat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceVideosSectionTest {
    @Test
    fun embed_url_uses_video_id() {
        assertEquals("https://www.tiktok.com/embed/v2/123", tiktokEmbedUrl("123"))
    }

    @Test
    fun only_tiktok_hosts_stay_in_the_webview() {
        assertTrue(isTikTokUrl("https://www.tiktok.com/embed/v2/1"))
        assertTrue(isTikTokUrl("https://tiktok.com/@a"))
        assertTrue(isTikTokUrl("https://m.tiktok.com/x"))
        assertFalse(isTikTokUrl("https://evil-tiktok.com/x"))
        assertFalse(isTikTokUrl("https://tiktok.com.evil.io/x"))
        assertFalse(isTikTokUrl("http://www.tiktok.com/x"))
        assertFalse(isTikTokUrl("intent://x"))
        assertFalse(isTikTokUrl("not a url"))
    }
}

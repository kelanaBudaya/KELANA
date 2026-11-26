package id.kelana.core.ui.tiktok

import java.net.URI

/*
 * Pure (Android-free) parts of the TikTok Embed Player container, kept separate so they can be unit tested.
 *
 * Notes for maintainers:
 * - TikTok may apply per-domain usage limits to the player; a heavy request volume can make it temporarily
 *   unavailable (it then reports onPlayerError). Always offer the "Buka di TikTok" fallback.
 * - The embed loads TikTok's own scripts and trackers inside the WebView (a third party, outside our control).
 *   Load it only when a card is actually composed, never in the background.
 * - Uses the current Embed Player (player/v1). The older "Embed Videos" embed/v2 is being deprecated by TikTok.
 */

/** Documented `player/v1` query options. Defaults here are KELANA's, not TikTok's. */
data class TikTokPlayerOptions(
    val controls: Boolean = true,
    val progressBar: Boolean = true,
    val playButton: Boolean = true,
    val volumeControl: Boolean = true,
    val fullscreenButton: Boolean = true,
    val timestamp: Boolean = true,
    val loop: Boolean = false,
    val autoplay: Boolean = false,
    val musicInfo: Boolean = false,
    val description: Boolean = false,
    /** false: related videos are the author's own (TikTok default is true). */
    val rel: Boolean = false,
    val nativeContextMenu: Boolean = false,
    val closedCaption: Boolean = true,
    val forceMuted: Boolean = false,
)

private const val PLAYER_BASE = "https://www.tiktok.com/player/v1/"

/** TikTok post ids are numeric. Anything else is refused so it can never reach a URL or the HTML wrapper. */
fun isValidTikTokVideoId(videoId: String): Boolean =
    videoId.isNotEmpty() && videoId.length <= 32 && videoId.all { it in '0'..'9' }

/** `https://www.tiktok.com/player/v1/{id}?controls=1&...`. Throws on a non-numeric id. */
fun tikTokPlayerUrl(videoId: String, options: TikTokPlayerOptions = TikTokPlayerOptions()): String {
    require(isValidTikTokVideoId(videoId)) { "Invalid TikTok video id" }
    fun Boolean.bit() = if (this) "1" else "0"
    val query = listOf(
        "controls" to options.controls,
        "progress_bar" to options.progressBar,
        "play_button" to options.playButton,
        "volume_control" to options.volumeControl,
        "fullscreen_button" to options.fullscreenButton,
        "timestamp" to options.timestamp,
        "loop" to options.loop,
        "autoplay" to options.autoplay,
        "music_info" to options.musicInfo,
        "description" to options.description,
        "rel" to options.rel,
        "native_context_menu" to options.nativeContextMenu,
        "closed_caption" to options.closedCaption,
        "force_muted" to options.forceMuted,
    ).joinToString("&") { (k, v) -> "$k=${v.bit()}" }
    return "$PLAYER_BASE$videoId?$query"
}

/** Name of the JS bridge object the wrapper calls. */
const val TIKTOK_BRIDGE_NAME = "KelanaTikTok"

/** Stable https origin for loadDataWithBaseURL, so the iframe has a real (non-null) embedding origin. */
const val TIKTOK_WRAPPER_BASE_URL = "https://kelana.id/"

/** Host-to-player commands documented by TikTok. */
enum class TikTokPlayerCommand(val type: String) { Play("play"), Pause("pause"), Mute("mute"), UnMute("unMute") }

/** JS snippet that posts [command] to the player iframe (`kelanaPost` is defined by the wrapper). */
fun tikTokCommandScript(command: TikTokPlayerCommand): String = "kelanaPost('${command.type}');"

/**
 * Wrapper page: one full-size player iframe, plus a listener that forwards only messages coming from that iframe
 * and carrying `x-tiktok-player: true` to the bridge (onPlayerReady, onStateChange, onPlayerError).
 */
fun tikTokWrapperHtml(videoId: String, options: TikTokPlayerOptions = TikTokPlayerOptions()): String {
    val src = tikTokPlayerUrl(videoId, options).replace("&", "&amp;")
    return """<!doctype html>
<html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>html,body{margin:0;height:100%;background:transparent;overflow:hidden}iframe{border:0;width:100%;height:100%}</style>
</head><body>
<iframe id="p" src="$src" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen title="TikTok"></iframe>
<script>
(function(){
var f=document.getElementById('p'),B=window.$TIKTOK_BRIDGE_NAME;
window.kelanaPost=function(t,v){f.contentWindow.postMessage({type:t,value:v,'x-tiktok-player':true},'*');};
window.addEventListener('message',function(e){
  if(e.source!==f.contentWindow||!B)return;
  var d=e.data;
  if(!d||d['x-tiktok-player']!==true)return;
  if(d.type==='onPlayerReady'){B.onPlayerReady();}
  else if(d.type==='onStateChange'){B.onStateChange(Number(d.value));}
  else if(d.type==='onPlayerError'){var v=d.value||{};B.onPlayerError(Number(v.errorCode)||0,String(v.errorType||''));}
});
})();
</script>
</body></html>"""
}

/** True only for https links on tiktok.com or one of its subdomains. Everything else leaves the WebView. */
fun isTikTokUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val host = uri.host?.lowercase() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) && (host == "tiktok.com" || host.endsWith(".tiktok.com"))
}

/** Player states from onStateChange. */
enum class TikTokPlayerState(val code: Int) {
    Init(-1), Ended(0), Playing(1), Paused(2), Buffering(3);

    companion object {
        fun fromCode(code: Int): TikTokPlayerState? = entries.firstOrNull { it.code == code }
    }
}

/** Player error from onPlayerError (1001 INVALID_VIDEO, 2001 SERVER_ERROR, 3001 PLAYBACK_ERROR, 3002 AUTOPLAY_ERROR). */
enum class TikTokPlayerError(val code: Int) {
    InvalidVideo(1001), ServerError(2001), PlaybackError(3001), AutoplayError(3002), Unknown(0);

    companion object {
        fun fromCode(code: Int): TikTokPlayerError = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

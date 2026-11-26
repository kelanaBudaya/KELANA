package id.kelana.core.ui.tiktok

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import id.kelana.core.ui.openCustomTab

/** Hoisted handle to drive one player: `controller.pause()` etc. No-op until the player view exists. */
@Stable
class TikTokPlayerController {
    private var webView: WebView? = null

    internal fun attach(view: WebView?) {
        webView = view
    }

    fun play() = send(TikTokPlayerCommand.Play)
    fun pause() = send(TikTokPlayerCommand.Pause)
    fun mute() = send(TikTokPlayerCommand.Mute)
    fun unMute() = send(TikTokPlayerCommand.UnMute)

    private fun send(command: TikTokPlayerCommand) {
        webView?.evaluateJavascript(tikTokCommandScript(command), null)
    }
}

@Composable
fun rememberTikTokPlayerController(): TikTokPlayerController = remember { TikTokPlayerController() }

/** Exposes exactly three methods to the page. Calls arrive on a WebView thread and are hopped to main. */
private class TikTokBridge(
    private val onReady: () -> Unit,
    private val onState: (TikTokPlayerState) -> Unit,
    private val onError: (TikTokPlayerError) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlayerReady() {
        main.post { onReady() }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        TikTokPlayerState.fromCode(state)?.let { main.post { onState(it) } }
    }

    @JavascriptInterface
    fun onPlayerError(errorCode: Int, errorType: String) {
        main.post { onError(TikTokPlayerError.fromCode(errorCode)) }
    }
}

/**
 * TikTok Embed Player (`player/v1`) in a locked-down WebView. Loads third-party TikTok content (and its trackers),
 * so compose it only when it is on screen. Call sites should offer an "open in TikTok" fallback for [onError] and
 * [onLoadFailed] (TikTok may rate-limit a domain; the player then reports an error).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TikTokEmbedPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    options: TikTokPlayerOptions = TikTokPlayerOptions(),
    controller: TikTokPlayerController = rememberTikTokPlayerController(),
    onReady: () -> Unit = {},
    onStateChange: (TikTokPlayerState) -> Unit = {},
    onError: (TikTokPlayerError) -> Unit = {},
    onLoadFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    val ready = rememberUpdatedState(onReady)
    val state = rememberUpdatedState(onStateChange)
    val error = rememberUpdatedState(onError)
    val loadFailed = rememberUpdatedState(onLoadFailed)

    if (!isValidTikTokVideoId(videoId)) {
        // A bad id never reaches the WebView.
        LaunchedEffect(videoId) { error.value(TikTokPlayerError.InvalidVideo) }
        return
    }

    val holder = remember(videoId) { arrayOfNulls<WebView>(1) }
    DisposableEffect(videoId) {
        onDispose {
            controller.attach(null)
            holder[0]?.let {
                it.removeJavascriptInterface(TIKTOK_BRIDGE_NAME)
                it.stopLoading()
                it.destroy()
            }
            holder[0] = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                // TikTok's player reads localStorage; without DOM storage it throws "Cannot read properties of null (reading 'getItem')" and never renders.
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mediaPlaybackRequiresUserGesture = !options.autoplay
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                addJavascriptInterface(
                    TikTokBridge(
                        onReady = { ready.value() },
                        onState = { state.value(it) },
                        onError = { error.value(it) },
                    ),
                    TIKTOK_BRIDGE_NAME,
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        if (isTikTokUrl(url)) return false
                        openCustomTab(context, url)
                        return true
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, err: WebResourceError) {
                        if (request.isForMainFrame) loadFailed.value()
                    }
                }
                holder[0] = this
                controller.attach(this)
                loadDataWithBaseURL(
                    TIKTOK_WRAPPER_BASE_URL,
                    tikTokWrapperHtml(videoId, options),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

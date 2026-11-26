package id.kelana.feature.tempat.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceVideo
import id.kelana.core.ui.openCustomTab
import id.kelana.feature.tempat.R
import java.net.URI

private val CardWidth = 200.dp

internal fun tiktokEmbedUrl(videoId: String) = "https://www.tiktok.com/embed/v2/$videoId"

/** True only for https links on tiktok.com or one of its subdomains. Everything else leaves the WebView. */
internal fun isTikTokUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val host = uri.host?.lowercase() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) && (host == "tiktok.com" || host.endsWith(".tiktok.com"))
}

/**
 * "Video pengunjung": curated TikTok videos in a horizontal 9:16 carousel. Stateful wrapper; a failure stays
 * inside this section. Privacy: each card loads the third-party TikTok embed only when it is composed (LazyRow).
 */
@Composable
internal fun PlaceVideosSection(modifier: Modifier = Modifier, viewModel: PlaceVideosViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    PlaceVideosContent(state = state, onRetry = viewModel::retry, modifier = modifier)
}

@Composable
internal fun PlaceVideosContent(state: PlaceVideosState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(modifier.padding(top = 26.dp)) {
        Text(
            text = stringResource(R.string.tempat_videos_title),
            modifier = Modifier.padding(horizontal = KelanaSpacing.gutter),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        when (state) {
            PlaceVideosState.Loading -> MessageCard(stringResource(R.string.tempat_videos_loading), null, null)
            PlaceVideosState.Empty -> MessageCard(
                stringResource(R.string.tempat_videos_empty_title),
                stringResource(R.string.tempat_videos_empty_body),
                null,
            )
            PlaceVideosState.Error -> MessageCard(
                stringResource(R.string.tempat_videos_error_title),
                stringResource(R.string.tempat_videos_error_body),
                onRetry,
            )
            is PlaceVideosState.Videos -> LazyRow(
                modifier = Modifier.padding(top = 14.dp),
                contentPadding = PaddingValues(horizontal = KelanaSpacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.id }) { VideoCard(it) }
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, body: String?, onRetry: (() -> Unit)?) {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 14.dp)
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.backgroundWarm)
            .padding(KelanaSpacing.gutter),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
        if (body != null) {
            Text(
                text = body,
                modifier = Modifier.padding(top = KelanaSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (onRetry != null) {
            Text(
                text = stringResource(R.string.tempat_videos_retry),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(top = 14.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.primary,
            )
        }
    }
}

/** One 9:16 card. The WebView exists only while the card is in composition and is destroyed when it leaves. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoCard(video: PlaceVideo) {
    val colors = KelanaTheme.colors
    val context = LocalContext.current
    var loading by remember(video.id) { mutableStateOf(true) }
    var failed by remember(video.id) { mutableStateOf(false) }
    var webView by remember(video.id) { mutableStateOf<WebView?>(null) }

    DisposableEffect(video.id) {
        onDispose {
            webView?.let {
                it.stopLoading()
                it.destroy()
            }
            webView = null
        }
    }

    Box(
        Modifier
            .width(CardWidth)
            .aspectRatio(9f / 16f)
            .clip(KelanaShapes.card)
            .background(colors.backgroundWarm),
    ) {
        if (!failed) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val url = request.url.toString()
                                if (isTikTokUrl(url)) return false
                                openCustomTab(context, url)
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) {
                                    failed = true
                                    loading = false
                                }
                            }
                        }
                        webView = this
                        loadUrl(tiktokEmbedUrl(video.tiktokVideoId))
                    }
                },
            )
        }
        if (loading && !failed) {
            Text(
                text = stringResource(R.string.tempat_videos_card_loading),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (failed) {
            Column(
                Modifier.fillMaxSize().padding(KelanaSpacing.gutter),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tempat_videos_card_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
                OpenOnTikTok(video, context)
            }
        }
    }
}

@Composable
private fun OpenOnTikTok(video: PlaceVideo, context: Context) {
    Text(
        text = stringResource(R.string.tempat_videos_open_tiktok),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { openCustomTab(context, video.tiktokUrl) }
            .padding(top = 12.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
        color = KelanaTheme.colors.primary,
    )
}

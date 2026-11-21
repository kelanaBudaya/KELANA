package id.kelana.feature.cerita.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.VideoStory
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.cerita.R

/**
 * Viewing a visitor video (artifact VideoPengunjung). PROVISIONAL: Media3 ExoPlayer is not in the version catalog,
 * so the video area is a clear placeholder. When the dependency exists, replace [VideoPlaceholder] with a PlayerView
 * that plays the signed URL of [VideoStory.videoPath].
 */
@Composable
internal fun VideoDetailScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: StoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    VideoDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSignIn = { requireLogin {} },
        onSeePlace = { placeId -> navigate(DetailTempatRoute(placeId)) },
        // Provisional: the existing report route reports a place, it does not carry the story id.
        onReport = { placeId -> requireLogin { navigate(LaporkanRoute(placeId)) } },
    )
}

@Composable
internal fun VideoDetailContent(
    state: StoryDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSeePlace: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val title = stringResource(R.string.video_detail_title)
    if (showStoryDetailFallback(state, title, onBack, onRetry, onSignIn)) return
    val story = (state as? StoryDetailUiState.Video)?.story ?: return
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.inkDeep)) {
        VideoPlaceholder(Modifier.align(Alignment.Center))
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 10.dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.cerita_ic_back), contentDescription = stringResource(R.string.cerita_back))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.galleryDark.copy(alpha = 0.92f))))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .background(colors.successSurface, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(painterResource(R.drawable.cerita_ic_stamp_ok), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = story.stampCode?.let { stringResource(R.string.video_recorded_at_stamp, story.placeName, it) }
                        ?: stringResource(R.string.video_recorded_at, story.placeName),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.successTextDeep,
                )
            }
            Text(
                text = authorLabel(story.authorName),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.white,
            )
            if (!story.caption.isNullOrBlank()) {
                Text(
                    text = story.caption!!,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.white,
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.white)
                        .clickable(onClick = { onSeePlace(story.placeId) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.cerita_see_place),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.navy,
                    )
                }
                ReportButton(
                    description = stringResource(R.string.cerita_report_video),
                    onClick = { onReport(story.placeId) },
                    light = true,
                    modifier = Modifier.border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun VideoPlaceholder(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.cerita_ic_play), contentDescription = null)
        }
        Text(
            text = stringResource(R.string.video_player_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = colors.skyText,
            textAlign = TextAlign.Center,
        )
    }
}

private val noop: () -> Unit = {}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VideoDetailPreview() {
    KelanaTheme {
        VideoDetailContent(StoryDetailUiState.Video(CeritaPreviewData.video), noop, noop, noop, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun VideoDetailErrorPreview() {
    KelanaTheme { VideoDetailContent(StoryDetailUiState.Error, noop, noop, noop, {}, {}) }
}

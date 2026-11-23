package id.kelana.feature.cerita.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.BannerTone
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.InfoBanner
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.theme.SerifItalic
import id.kelana.core.model.PostcardStory
import id.kelana.core.model.Story
import id.kelana.core.model.VideoStory
import id.kelana.core.navigation.CeritaPostcardRoute
import id.kelana.core.navigation.CeritaVideoRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.cerita.R

/**
 * Cerita tab: visitor postcards and videos, newest first (artifact UlasanPengunjung, 2-column wall).
 * [navigate] takes a route object from :core:navigation.
 */
@Composable
internal fun CeritaFeedScreen(navigate: (Any) -> Unit, viewModel: CeritaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    CeritaFeedContent(
        state = state,
        onSelectFilter = viewModel::select,
        onRetry = viewModel::retry,
        onOpen = { story ->
            navigate(
                when (story) {
                    is PostcardStory -> CeritaPostcardRoute(story.id)
                    is VideoStory -> CeritaVideoRoute(story.id)
                },
            )
        },
        // Provisional: the existing report route reports a place, it does not carry the story id.
        onReport = { story -> requireLogin { navigate(LaporkanRoute(story.placeId)) } },
        // Entry point to the creation flow, which belongs to :feature:postcard.
        onWritePostcard = { requireLogin { navigate(PostcardRoute()) } },
        // A guest is sent to Masuk; for a signed-in user the action does nothing.
        onSignIn = { requireLogin {} },
    )
}

@Composable
internal fun CeritaFeedContent(
    state: CeritaUiState,
    onSelectFilter: (StoryFilter) -> Unit,
    onRetry: () -> Unit,
    onOpen: (Story) -> Unit,
    onReport: (Story) -> Unit,
    onWritePostcard: () -> Unit,
    onSignIn: () -> Unit,
) {
    val colors = KelanaTheme.colors
    val showFeed = state.status == FeedStatus.Content || state.status == FeedStatus.Empty
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.cerita_title),
                modifier = Modifier.padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            when (state.status) {
                FeedStatus.Loading -> LoadingBox()
                FeedStatus.Guest -> GuestState(onSignIn)
                FeedStatus.Error -> ErrorState(onRetry)
                FeedStatus.Empty, FeedStatus.Content -> FeedBody(state, onSelectFilter, onOpen, onReport)
            }
        }
        if (showFeed) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.outline))
                    .padding(horizontal = KelanaSpacing.gutterWide, vertical = KelanaSpacing.md),
            ) {
                KelanaButton(
                    text = stringResource(R.string.cerita_write_postcard),
                    onClick = onWritePostcard,
                    modifier = Modifier.fillMaxWidth(),
                    variant = KelanaButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun FeedBody(
    state: CeritaUiState,
    onSelectFilter: (StoryFilter) -> Unit,
    onOpen: (Story) -> Unit,
    onReport: (Story) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (!state.feedIsEmpty) {
            InfoBanner(
                text = stringResource(R.string.cerita_summary_caption),
                title = listOf(
                    pluralStringResource(R.plurals.cerita_postcard_count, state.postcardCount, state.postcardCount),
                    pluralStringResource(R.plurals.cerita_video_count, state.videoCount, state.videoCount),
                ).joinToString(" · "),
                modifier = Modifier.padding(horizontal = KelanaSpacing.gutter),
                tone = BannerTone.Warning,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
        ) {
            FilterChoice(StoryFilter.All, R.string.cerita_filter_all, state.filter, onSelectFilter)
            FilterChoice(StoryFilter.Postcard, R.string.cerita_filter_postcard, state.filter, onSelectFilter)
            FilterChoice(StoryFilter.Video, R.string.cerita_filter_video, state.filter, onSelectFilter)
        }
        if (state.status == FeedStatus.Empty) {
            val noStoriesAtAll = state.feedIsEmpty
            EmptyState(
                title = stringResource(if (noStoriesAtAll) R.string.cerita_empty_title else R.string.cerita_empty_filter_title),
                body = stringResource(if (noStoriesAtAll) R.string.cerita_empty_body else R.string.cerita_empty_filter_body),
                modifier = Modifier.padding(top = KelanaSpacing.lg),
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(state.items, key = { _, story -> story.id }) { index, story ->
                    when (story) {
                        is PostcardStory -> PostcardTile(
                            story = story,
                            rotation = TileRotations[index % TileRotations.size],
                            onOpen = { onOpen(story) },
                            onReport = { onReport(story) },
                        )
                        is VideoStory -> VideoTile(story = story, onOpen = { onOpen(story) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChoice(
    value: StoryFilter,
    label: Int,
    selected: StoryFilter,
    onSelect: (StoryFilter) -> Unit,
) {
    KelanaChip(label = stringResource(label), selected = value == selected, onClick = { onSelect(value) })
}

/** Slight tilt of the polaroid tiles, as in the artifact (-2, 2, 2.5 and -2 degrees). */
private val TileRotations = listOf(-2f, 2f, 2.5f, -2f)

@Composable
private fun PostcardTile(story: PostcardStory, rotation: Float, onOpen: () -> Unit, onReport: () -> Unit) {
    val colors = KelanaTheme.colors
    val author = authorLabel(story.authorName)
    val openDescription = stringResource(R.string.cerita_open_postcard, author, story.placeName)
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier
            .graphicsLayer { rotationZ = rotation }
            .shadow(4.dp, shape)
            .background(colors.surface, shape),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .semantics { contentDescription = openDescription }
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            // PROVISIONAL: the photo is a tinted panel until an image loader (Coil) and signed URLs exist.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(placeholderTint(story.id)),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = story.placeName,
                    modifier = Modifier
                        .padding(6.dp)
                        .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StarRow(story.rating, starSize = 13, modifier = Modifier.padding(top = 8.dp))
            if (!story.message.isNullOrBlank()) {
                Text(
                    text = "“${story.message}”",
                    modifier = Modifier.padding(top = 4.dp),
                    style = SerifItalic,
                    color = colors.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 0.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = author,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ReportButton(
                description = stringResource(R.string.cerita_report_postcard),
                onClick = onReport,
                light = false,
            )
        }
    }
}

@Composable
private fun VideoTile(story: VideoStory, onOpen: () -> Unit) {
    val colors = KelanaTheme.colors
    val author = authorLabel(story.authorName)
    val playDescription = stringResource(R.string.cerita_play_video, author, story.placeName)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.inkDeep)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = playDescription },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.cerita_ic_play), contentDescription = null)
        }
        OnSiteBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                text = story.caption?.takeIf { it.isNotBlank() } ?: story.placeName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.white,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = author,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.skyText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "Di lokasi": the author holds a stamp for this place (enforced by RLS on insert). */
@Composable
internal fun OnSiteBadge(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier = modifier
            .background(colors.successSurface, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(painterResource(R.drawable.cerita_ic_stamp_ok), contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.cerita_badge_on_site),
            style = MaterialTheme.typography.labelSmall,
            color = colors.successTextDeep,
        )
    }
}

/** Flag button, 48 dp touch target. */
@Composable
internal fun ReportButton(description: String, onClick: () -> Unit, light: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (light) R.drawable.cerita_ic_flag_light else R.drawable.cerita_ic_flag),
            contentDescription = description,
            modifier = Modifier.size(if (light) 20.dp else 14.dp),
        )
    }
}

private val noop: () -> Unit = {}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FeedContentPreview() {
    KelanaTheme {
        val stories = CeritaPreviewData.stories
        CeritaFeedContent(
            CeritaUiState(FeedStatus.Content, items = stories, postcardCount = 2, videoCount = 2),
            {}, noop, {}, {}, noop, noop,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun FeedLoadingPreview() {
    KelanaTheme { CeritaFeedContent(CeritaUiState(FeedStatus.Loading), {}, noop, {}, {}, noop, noop) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun FeedEmptyPreview() {
    KelanaTheme { CeritaFeedContent(CeritaUiState(FeedStatus.Empty), {}, noop, {}, {}, noop, noop) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun FeedErrorPreview() {
    KelanaTheme { CeritaFeedContent(CeritaUiState(FeedStatus.Error), {}, noop, {}, {}, noop, noop) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun FeedGuestPreview() {
    KelanaTheme { CeritaFeedContent(CeritaUiState(FeedStatus.Guest), {}, noop, {}, {}, noop, noop) }
}

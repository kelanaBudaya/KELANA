package id.kelana.feature.cerita.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.theme.SerifHeadline
import id.kelana.core.designsystem.theme.SerifItalic
import id.kelana.core.model.PostcardStory
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.cerita.R

/** Viewing a visitor postcard (artifact Postcard, Jurnal style, 9:16 card). */
@Composable
internal fun PostcardDetailScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: StoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    PostcardDetailContent(
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
internal fun PostcardDetailContent(
    state: StoryDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSeePlace: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val title = stringResource(R.string.postcard_detail_title)
    if (showStoryDetailFallback(state, title, onBack, onRetry, onSignIn)) return
    val story = (state as? StoryDetailUiState.Postcard)?.story ?: return
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.backgroundWarm)) {
        KelanaBackTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PostcardCard(story, Modifier.padding(top = 10.dp))
            Text(
                text = stringResource(R.string.postcard_by_author, authorLabel(story.authorName)),
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KelanaButton(
                    text = stringResource(R.string.cerita_see_place),
                    onClick = { onSeePlace(story.placeId) },
                    modifier = Modifier.weight(1f),
                    variant = KelanaButtonVariant.Outline,
                )
                ReportButton(
                    description = stringResource(R.string.cerita_report_postcard),
                    onClick = { onReport(story.placeId) },
                    light = false,
                    modifier = Modifier.border(BorderStroke(1.5.dp, colors.outline), CircleShape),
                )
            }
        }
    }
}

/** The 270 x 480 dp Jurnal postcard (9:16). Colours and type from the artifact; the photo is a placeholder panel (PROVISIONAL). */
@Composable
private fun PostcardCard(story: PostcardStory, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Box(
        modifier = modifier
            .size(width = 270.dp, height = 480.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.navy),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.postcard_brand),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = colors.stampYellow,
            )
            Column(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .graphicsLayer { rotationZ = -3f }
                    .background(colors.backgroundWarm, RoundedCornerShape(4.dp))
                    .padding(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(placeholderTint(story.id)),
                ) {
                    Text(
                        text = stringResource(R.string.cerita_photo_unavailable),
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.navy,
                    )
                    story.visitedOn?.let { date ->
                        Text(
                            text = stringResource(R.string.postcard_visited_on, formatStoryDate(date).uppercase()),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(colors.navy, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = colors.white,
                        )
                    }
                }
                Text(
                    text = story.placeName,
                    modifier = Modifier.padding(top = 10.dp),
                    style = SerifHeadline.copy(fontSize = 22.sp, lineHeight = 26.sp),
                    color = colors.stampBrown,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StarRow(story.rating, starSize = 13, modifier = Modifier.padding(top = 6.dp))
                if (!story.message.isNullOrBlank()) {
                    Text(
                        text = "“${story.message}”",
                        modifier = Modifier.padding(top = 4.dp),
                        style = SerifItalic.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = colors.ink,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        story.stampCode?.let { code ->
            val description = stringResource(R.string.postcard_stamp_desc, code)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 22.dp)
                    .graphicsLayer { rotationZ = 10f }
                    .semantics { contentDescription = description }
                    .size(72.dp)
                    .background(colors.stampYellow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = code,
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.ink,
                )
            }
        }
    }
}

private val noop: () -> Unit = {}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PostcardDetailPreview() {
    KelanaTheme {
        PostcardDetailContent(
            StoryDetailUiState.Postcard(CeritaPreviewData.postcard), noop, noop, noop, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun PostcardDetailGuestPreview() {
    KelanaTheme { PostcardDetailContent(StoryDetailUiState.Guest, noop, noop, noop, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun PostcardDetailNotFoundPreview() {
    KelanaTheme { PostcardDetailContent(StoryDetailUiState.NotFound, noop, noop, noop, {}, {}) }
}

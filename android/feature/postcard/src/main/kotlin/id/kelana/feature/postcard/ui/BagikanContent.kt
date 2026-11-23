package id.kelana.feature.postcard.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.JakartaFontFamily
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.ShareFormat
import id.kelana.feature.postcard.ui.card.PostcardCardView
import java.time.Instant

internal data class BagikanActions(
    val onBack: () -> Unit = {},
    val onFormat: (ShareFormat) -> Unit = {},
    val onShare: (ShareTarget) -> Unit = {},
    val onSave: () -> Unit = {},
    val onWriteAgain: () -> Unit = {},
)

/** Bagikan postcard (Figma 7.2). Compared with BagikanPostcardIlustratif.dc.html. */
@Composable
internal fun BagikanContent(
    state: BagikanUiState,
    actions: BagikanActions,
    snackbarHost: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Artifact: padding 10 12 0, back button, title 17/800.
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = actions.onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id.kelana.core.designsystem.R.drawable.ic_back),
                        contentDescription = stringResource(R.string.postcard_back),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.postcard_bagikan_title),
                    modifier = Modifier.padding(start = 2.dp),
                    style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.ink,
                )
            }
            val card = state.card
            if (card == null) {
                EmptyState(
                    title = stringResource(R.string.postcard_gone_title),
                    body = stringResource(R.string.postcard_gone_body),
                    illustration = {
                        Image(painter = painterResource(R.drawable.postcard_art_camera), contentDescription = null, modifier = Modifier.size(72.dp))
                    },
                    actionLabel = stringResource(R.string.postcard_gone_action),
                    onAction = actions.onWriteAgain,
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Card preview: 270 wide, radius 18, shadow 0 10 28 rgba(27,54,113,.3).
                    PostcardCardView(
                        content = card,
                        format = state.format,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = colors.navy, spotColor = colors.navy)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KelanaChip(
                            label = stringResource(R.string.postcard_format_story),
                            selected = state.format == ShareFormat.Story,
                            onClick = { actions.onFormat(ShareFormat.Story) },
                        )
                        KelanaChip(
                            label = stringResource(R.string.postcard_format_feed),
                            selected = state.format == ShareFormat.Feed,
                            onClick = { actions.onFormat(ShareFormat.Feed) },
                        )
                    }
                    Box(Modifier.padding(top = 16.dp).height48(), contentAlignment = Alignment.Center) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(28.dp), color = colors.primary, strokeWidth = 3.dp)
                    }
                    // Share row: 56 square tiles, radius 18, 12/700 labels. TikTok is not offered.
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        ShareTile(R.drawable.postcard_ic_share, R.string.postcard_share_instagram, !state.busy) {
                            actions.onShare(ShareTarget.InstagramStory)
                        }
                        ShareTile(R.drawable.postcard_ic_share, R.string.postcard_share_whatsapp, !state.busy) {
                            actions.onShare(ShareTarget.WhatsApp)
                        }
                        ShareTile(R.drawable.postcard_ic_share, R.string.postcard_share_other, !state.busy) {
                            actions.onShare(ShareTarget.Other)
                        }
                        if (state.canSave) {
                            ShareTile(R.drawable.postcard_ic_download, R.string.postcard_share_save, !state.busy, actions.onSave)
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

private fun Modifier.height48(): Modifier = this.size(width = 48.dp, height = 48.dp)

@Composable
private fun androidx.compose.foundation.layout.RowScope.ShareTile(
    icon: Int,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.white)
                .border(1.dp, colors.outline, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                alpha = if (enabled) 1f else 0.4f,
            )
        }
        Text(
            text = stringResource(label),
            style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

// ---- previews --------------------------------------------------------------------------------------------------

private val PreviewCard = CardContent(
    placeName = "Museum Sonobudoyo",
    capCode = "YK-02",
    message = "Koleksi keris dan wayangnya lengkap. Datang pagi biar tidak ramai.",
    rating = 4,
    style = PostcardStyle.Jurnal,
    stampedAt = Instant.parse("2026-11-20T03:00:00Z"),
    photoJpeg = null,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun BagikanStoryPreview() = KelanaTheme {
    BagikanContent(BagikanUiState(card = PreviewCard, canSave = true), BagikanActions(), SnackbarHostState())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun BagikanFeedPreview() = KelanaTheme {
    BagikanContent(
        BagikanUiState(card = PreviewCard, format = ShareFormat.Feed, canSave = false),
        BagikanActions(),
        SnackbarHostState(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun BagikanBusyPreview() = KelanaTheme {
    BagikanContent(BagikanUiState(card = PreviewCard, busy = true, canSave = true), BagikanActions(), SnackbarHostState())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun BagikanGonePreview() = KelanaTheme {
    BagikanContent(BagikanUiState(card = null), BagikanActions(), SnackbarHostState())
}

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.BannerTone
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.InfoBanner
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.JakartaFontFamily
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.VIDEO_MAX_SECONDS_PROVISIONAL
import id.kelana.core.model.VideoUploadFailure
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.formatVideoDuration

/** Everything Unggah video can do, as plain callbacks (keeps [UnggahVideoContent] previewable). */
internal data class UnggahVideoActions(
    val onBack: () -> Unit = {},
    val onPick: () -> Unit = {},
    val onRemove: () -> Unit = {},
    val onCaption: (String) -> Unit = {},
    val onConsentArea: (Boolean) -> Unit = {},
    val onConsentPeople: (Boolean) -> Unit = {},
    val onConsentCeremony: (Boolean) -> Unit = {},
    val onUpload: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onOpenPaspor: () -> Unit = {},
    val onTakeStamp: (String) -> Unit = {},
)

/** Unggah video (Figma 7.3) and its guard states. Compared with UnggahVideoIlustratif.dc.html. */
@Composable
internal fun UnggahVideoContent(state: UnggahVideoUiState, actions: UnggahVideoActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        VideoTopRow(onBack = actions.onBack)
        when (val mode = state.mode) {
            UnggahMode.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            UnggahMode.Guest -> VideoGuard(
                title = stringResource(R.string.video_guest_title),
                body = stringResource(R.string.video_guest_body),
                action = stringResource(R.string.postcard_guest_action),
                onAction = actions.onLogin,
            )
            UnggahMode.PlaceUnknown -> VideoGuard(
                title = stringResource(R.string.video_unknown_title),
                body = stringResource(R.string.video_unknown_body),
                action = stringResource(R.string.video_unknown_action),
                onAction = actions.onBack,
            )
            is UnggahMode.NeedsStamp -> VideoGuard(
                title = stringResource(R.string.video_needsstamp_title, mode.place.name),
                body = stringResource(R.string.video_needsstamp_body),
                action = stringResource(R.string.postcard_needsstamp_action),
                onAction = { actions.onTakeStamp(mode.place.id) },
                secondaryAction = stringResource(R.string.postcard_open_paspor),
                onSecondaryAction = actions.onOpenPaspor,
            )
            is UnggahMode.Form -> VideoForm(state, mode.place, actions)
        }
    }
}

@Composable
private fun VideoTopRow(onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    // Artifact: padding 10 12 0, 44 px back button, title 17/800.
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id.kelana.core.designsystem.R.drawable.ic_back),
                contentDescription = stringResource(R.string.postcard_back),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(R.string.video_title),
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                fontFamily = JakartaFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
            ),
        )
    }
}

@Composable
private fun VideoGuard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            title = title,
            body = body,
            illustration = {
                Image(painter = painterResource(R.drawable.postcard_ic_play), contentDescription = null, modifier = Modifier.size(72.dp))
            },
            actionLabel = action,
            onAction = onAction,
        )
        if (secondaryAction != null) {
            KelanaButton(text = secondaryAction, onClick = onSecondaryAction, variant = KelanaButtonVariant.Text)
        }
    }
}

@Composable
private fun VideoForm(state: UnggahVideoUiState, place: Place, actions: UnggahVideoActions) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            VideoPreviewBox(state = state, placeName = place.name, onPick = actions.onPick)

            if (state.video != null || state.videoUnreadable) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.videoUnreadable) {
                        Text(
                            stringResource(R.string.video_unreadable),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                        )
                    } else {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        TextButton(onClick = actions.onPick) { Text(stringResource(R.string.video_change), color = colors.primary) }
                        TextButton(onClick = actions.onRemove) { Text(stringResource(R.string.video_remove), color = colors.primary) }
                    }
                }
            }

            if (state.tooLong) {
                Text(
                    text = stringResource(
                        R.string.video_too_long,
                        formatVideoDuration(state.video?.durationSeconds ?: 0),
                        VIDEO_MAX_SECONDS_PROVISIONAL,
                    ),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            }

            Text(
                text = stringResource(R.string.video_max_hint, VIDEO_MAX_SECONDS_PROVISIONAL),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
                style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp),
                color = colors.muted,
            )

            CaptionField(value = state.caption, onChange = actions.onCaption)

            ConsentPanel(state = state, actions = actions)
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        }
        VideoBottomBar(state = state, onUpload = actions.onUpload)
    }
}

/** Dark 300 dp panel, radius 24, play circle 72, place badge bottom-left, duration badge bottom-right (artifact). */
@Composable
private fun VideoPreviewBox(state: UnggahVideoUiState, placeName: String, onPick: () -> Unit) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val pickLabel = stringResource(if (state.video == null) R.string.video_pick_cd else R.string.video_chosen_cd)
    Box(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 6.dp)
            .fillMaxWidth()
            .height(300.dp)
            .clip(shape)
            .background(colors.inkDeep)
            .clickable(role = Role.Button, onClickLabel = pickLabel, onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (state.readingVideo) {
                    CircularProgressIndicator(color = colors.white, modifier = Modifier.size(30.dp))
                } else {
                    Image(painter = painterResource(R.drawable.postcard_ic_play_white), contentDescription = null, modifier = Modifier.size(30.dp))
                }
            }
            if (state.video == null) {
                Text(
                    stringResource(if (state.readingVideo) R.string.video_reading else R.string.video_pick_title),
                    style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.white,
                )
                if (!state.readingVideo) {
                    Text(
                        stringResource(R.string.video_pick_hint),
                        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp),
                        color = colors.skyText,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(colors.successSurface, RoundedCornerShape(KelanaRadii.pill))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(painter = painterResource(R.drawable.postcard_ic_leaf_stamp), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.video_recorded_at, placeName),
                style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.successTextDeep,
            )
        }
        if (state.video != null) {
            val duration = formatVideoDuration(state.video.durationSeconds)
            val durationCd = stringResource(R.string.video_duration_cd, duration)
            Text(
                text = duration,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .semantics { contentDescription = durationCd },
                style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = colors.white,
            )
        }
    }
}

@Composable
private fun CaptionField(value: String, onChange: (String) -> Unit) {
    val colors = KelanaTheme.colors
    val label = stringResource(R.string.video_caption_label)
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
        Text(
            text = label,
            style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.ink,
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = label },
            singleLine = true,
            textStyle = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp, color = colors.ink),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(KelanaRadii.input))
                        .background(colors.white)
                        .border(1.5.dp, colors.outline, RoundedCornerShape(KelanaRadii.input))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            stringResource(R.string.video_caption_hint),
                            style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp),
                            color = colors.muted,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** "Sebelum mengunggah": warm panel, radius 20, padding 14, gap 10, three checkboxes. All three must be ticked. */
@Composable
private fun ConsentPanel(state: UnggahVideoUiState, actions: UnggahVideoActions) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 16.dp)
            .fillMaxWidth()
            .background(colors.backgroundWarm, RoundedCornerShape(KelanaRadii.listCard))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painter = painterResource(R.drawable.postcard_ic_shield_check), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                stringResource(R.string.video_before_title),
                style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
        }
        ConsentRow(stringResource(R.string.video_consent_area), state.consentArea, actions.onConsentArea)
        ConsentRow(stringResource(R.string.video_consent_people), state.consentPeople, actions.onConsentPeople)
        ConsentRow(stringResource(R.string.video_consent_ceremony), state.consentCeremony, actions.onConsentCeremony)
    }
}

@Composable
private fun ConsentRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = colors.primary),
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = text,
            style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 13.sp, lineHeight = 19.sp),
            color = colors.ink,
        )
    }
}

@Composable
private fun VideoBottomBar(state: UnggahVideoUiState, onUpload: () -> Unit) {
    val colors = KelanaTheme.colors
    val uploading = state.upload as? UploadState.Uploading
    val failed = state.upload as? UploadState.Failed
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.white)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (failed != null) {
                InfoBanner(
                    text = stringResource(
                        when (failed.reason) {
                            VideoUploadFailure.TooLong -> R.string.video_error_too_long
                            VideoUploadFailure.NotConfigured -> R.string.video_error_not_configured
                            else -> R.string.video_error_network
                        },
                    ),
                    tone = BannerTone.Warning,
                )
            }
            if (uploading != null) {
                LinearProgressIndicator(
                    progress = { uploading.fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.outline,
                )
            }
            KelanaButton(
                text = when {
                    uploading != null -> stringResource(R.string.video_uploading, (uploading.fraction * 100).toInt())
                    failed != null && failed.reason != VideoUploadFailure.NotConfigured -> stringResource(R.string.video_retry)
                    else -> stringResource(R.string.video_upload)
                },
                onClick = onUpload,
                enabled = state.canUpload,
                variant = KelanaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- previews --------------------------------------------------------------------------------------------------

private val PreviewPlace = Place(
    "museum-sonobudoyo", "museum-sonobudoyo", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02",
    0.0, 0.0, "", "", "museum",
)
private val PreviewVideo = PickedVideo("content://video/1", 42)
private val FormMode = UnggahMode.Form(PreviewPlace)

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Tidak ada video")
@Composable
private fun UnggahEmptyPreview() = KelanaTheme { UnggahVideoContent(UnggahVideoUiState(mode = FormMode), UnggahVideoActions()) }

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Video dipilih")
@Composable
private fun UnggahPickedPreview() = KelanaTheme {
    UnggahVideoContent(
        UnggahVideoUiState(mode = FormMode, video = PreviewVideo, consentArea = true, consentPeople = true, consentCeremony = true),
        UnggahVideoActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Terlalu panjang")
@Composable
private fun UnggahTooLongPreview() = KelanaTheme {
    UnggahVideoContent(
        UnggahVideoUiState(mode = FormMode, video = PickedVideo("content://video/2", VIDEO_MAX_SECONDS_PROVISIONAL + 30)),
        UnggahVideoActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Mengunggah")
@Composable
private fun UnggahUploadingPreview() = KelanaTheme {
    UnggahVideoContent(
        UnggahVideoUiState(
            mode = FormMode, video = PreviewVideo, consentArea = true, consentPeople = true, consentCeremony = true,
            upload = UploadState.Uploading(0.4f),
        ),
        UnggahVideoActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900, name = "Gagal / offline")
@Composable
private fun UnggahFailedPreview() = KelanaTheme {
    UnggahVideoContent(
        UnggahVideoUiState(
            mode = FormMode, video = PreviewVideo, consentArea = true, consentPeople = true, consentCeremony = true,
            upload = UploadState.Failed(VideoUploadFailure.Network),
        ),
        UnggahVideoActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700, name = "Ambil cap dulu")
@Composable
private fun UnggahNeedsStampPreview() = KelanaTheme {
    UnggahVideoContent(UnggahVideoUiState(mode = UnggahMode.NeedsStamp(PreviewPlace)), UnggahVideoActions())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700, name = "Tamu")
@Composable
private fun UnggahGuestPreview() = KelanaTheme { UnggahVideoContent(UnggahVideoUiState(mode = UnggahMode.Guest), UnggahVideoActions()) }

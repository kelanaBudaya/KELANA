package id.kelana.feature.postcard.ui

import android.graphics.BitmapFactory
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.BannerTone
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.InfoBanner
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.JakartaFontFamily
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.POSTCARD_MESSAGE_MAX
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.StampedPlace
import id.kelana.feature.postcard.ui.card.PostcardArt
import java.time.Instant

/** Everything the Tulis screen can do, as plain callbacks (keeps [TulisContent] previewable). */
internal data class TulisActions(
    val onBack: () -> Unit = {},
    val onPickGallery: () -> Unit = {},
    val onPickCamera: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onRating: (Int) -> Unit = {},
    val onMessage: (String) -> Unit = {},
    val onStyle: (PostcardStyle) -> Unit = {},
    val onPublic: (Boolean) -> Unit = {},
    val onPublish: () -> Unit = {},
    val onPlace: (String) -> Unit = {},
    val onLogin: () -> Unit = {},
    val onOpenPaspor: () -> Unit = {},
    val onTakeStamp: (String) -> Unit = {},
    val onVideo: (String) -> Unit = {},
)

/** Tulis postcard (Figma 7.1) and its guard states. Compared with TulisPostcardIlustratif.dc.html. */
@Composable
internal fun TulisContent(state: TulisUiState, actions: TulisActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        TopRow(onBack = actions.onBack)
        when (val mode = state.mode) {
            TulisMode.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            TulisMode.Guest -> GuardState(
                title = stringResource(R.string.postcard_guest_title),
                body = stringResource(R.string.postcard_guest_body),
                action = stringResource(R.string.postcard_guest_action),
                onAction = actions.onLogin,
            )
            TulisMode.NoStamps -> GuardState(
                title = stringResource(R.string.postcard_nostamps_title),
                body = stringResource(R.string.postcard_nostamps_body),
                action = stringResource(R.string.postcard_open_paspor),
                onAction = actions.onOpenPaspor,
            )
            is TulisMode.NeedsStamp -> GuardState(
                title = stringResource(R.string.postcard_needsstamp_title, mode.place.name),
                body = stringResource(R.string.postcard_needsstamp_body),
                action = stringResource(R.string.postcard_needsstamp_action),
                onAction = { actions.onTakeStamp(mode.place.id) },
                secondaryAction = stringResource(R.string.postcard_open_paspor),
                onSecondaryAction = actions.onOpenPaspor,
            )
            is TulisMode.PickPlace -> PlacePicker(mode.places, actions.onPlace)
            is TulisMode.Form -> FormBody(state, mode.place, actions)
        }
    }
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    // Artifact: padding 10 12 0, 44 px back button, title 17/800.
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(KelanaRadii.pill))
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
            text = stringResource(R.string.postcard_tulis_title),
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
private fun GuardState(
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
                Image(painter = painterResource(R.drawable.postcard_art_camera), contentDescription = null, modifier = Modifier.size(72.dp))
            },
            actionLabel = action,
            onAction = onAction,
        )
        if (secondaryAction != null) {
            KelanaButton(
                text = secondaryAction,
                onClick = onSecondaryAction,
                variant = KelanaButtonVariant.Text,
            )
        }
    }
}

@Composable
private fun PlacePicker(places: List<StampedPlace>, onPlace: (String) -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.postcard_pick_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
        )
        Text(
            stringResource(R.string.postcard_pick_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        places.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(KelanaRadii.card))
                    .background(colors.white)
                    .border(1.dp, colors.outline, RoundedCornerShape(KelanaRadii.card))
                    .clickable(role = Role.Button) { onPlace(item.place.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.place.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text(item.place.stampLabel, style = MaterialTheme.typography.bodySmall, color = colors.muted)
                }
            }
        }
    }
}

@Composable
private fun FormBody(state: TulisUiState, place: Place, actions: TulisActions) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            PhotoBox(state = state, stampLabel = place.stampLabel, actions = actions)

            // Seberapa berkesan?
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                SectionTitle(stringResource(R.string.postcard_rating_title))
                RatingRow(rating = state.rating, onRating = actions.onRating)
            }

            // Pesanmu
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                SectionTitle(stringResource(R.string.postcard_message_label))
                MessageField(
                    value = state.message,
                    onChange = actions.onMessage,
                )
                Text(
                    text = stringResource(R.string.postcard_message_counter, state.messageLength, POSTCARD_MESSAGE_MAX),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }

            // Gaya kartu
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                SectionTitle(stringResource(R.string.postcard_style_title))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PostcardStyle.entries.forEach { style ->
                        KelanaChip(
                            label = stringResource(style.labelRes()),
                            selected = state.style == style,
                            onClick = { actions.onStyle(style) },
                        )
                    }
                }
            }

            // Tampilkan di halaman <tempat>
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .toggleable(
                        value = state.isPublic,
                        role = Role.Checkbox,
                        onValueChange = actions.onPublic,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.isPublic,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    text = stringResource(R.string.postcard_public_label, place.name),
                    style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 13.sp, lineHeight = 19.sp),
                    color = colors.ink,
                )
            }
        }
        BottomBar(state = state, onPublish = actions.onPublish, onVideo = { actions.onVideo(place.id) })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
        color = KelanaTheme.colors.ink,
    )
}

/** Photo area: 220 high, radius 20, dashed 2 px #B9C4DA border, white fill (artifact). With a photo it shows the photo. */
@Composable
private fun PhotoBox(state: TulisUiState, stampLabel: String, actions: TulisActions) {
    val colors = KelanaTheme.colors
    var chooseSource by remember { mutableStateOf(false) }
    val dashColor = colors.dashMuted
    val shape = RoundedCornerShape(20.dp)
    val photoBitmap = remember(state.photo) {
        state.photo?.let { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap() }
    }
    val boxDescription = stringResource(R.string.postcard_photo_cd)
    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape)
                .background(colors.white)
                .then(
                    if (photoBitmap == null) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = dashColor,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .clickable(role = Role.Button, onClickLabel = boxDescription) { chooseSource = true },
            contentAlignment = Alignment.Center,
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = stringResource(R.string.postcard_photo_chosen_cd),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (state.photoBusy) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = colors.primary)
                    Text(
                        stringResource(R.string.postcard_photo_processing),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painter = painterResource(R.drawable.postcard_art_camera), contentDescription = null, modifier = Modifier.size(56.dp))
                    Text(
                        stringResource(R.string.postcard_photo_title),
                        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.primary,
                    )
                    Text(
                        stringResource(R.string.postcard_photo_hint),
                        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 12.sp),
                        color = colors.muted,
                    )
                }
            }
        }
        CapSticker(
            stampLabel = stampLabel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-10).dp)
                .rotate(8f),
        )
    }
    if (photoBitmap != null || state.photoFailed) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.photoFailed) {
                Text(
                    stringResource(R.string.postcard_photo_failed),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                TextButton(onClick = { chooseSource = true }) { Text(stringResource(R.string.postcard_photo_change), color = colors.primary) }
                TextButton(onClick = actions.onRemovePhoto) { Text(stringResource(R.string.postcard_photo_remove), color = colors.primary) }
            }
        }
    }
    if (chooseSource) {
        AlertDialog(
            onDismissRequest = { chooseSource = false },
            title = { Text(stringResource(R.string.postcard_photo_source_title)) },
            text = {
                Column {
                    TextButton(onClick = { chooseSource = false; actions.onPickGallery() }) {
                        Text(stringResource(R.string.postcard_photo_gallery), color = colors.primary)
                    }
                    TextButton(onClick = { chooseSource = false; actions.onPickCamera() }) {
                        Text(stringResource(R.string.postcard_photo_camera), color = colors.primary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { chooseSource = false }) { Text(stringResource(R.string.postcard_cancel), color = colors.primary) }
            },
            containerColor = colors.white,
        )
    }
}

/**
 * "YK-02 / CAP" sticker (artifact: 78 x 56, perforated edge, white outline, blue fill #24458F, drop shadow).
 * The perforation is approximated with a rounded shape; the white outline and shadow are drawn.
 */
@Composable
private fun CapSticker(stampLabel: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val description = stringResource(R.string.postcard_sticker_cd, stampLabel)
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = description }
            .shadow(3.dp, shape)
            .width(78.dp)
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.primary)
            .border(BorderStroke(2.5.dp, colors.white), shape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
            val s = size.height / 60f
            scale(s, s, androidx.compose.ui.geometry.Offset.Zero) {
                TOWER.forEach { drawPath(it, colors.stampYellow) }
            }
        }
        Text(
            text = stampLabel,
            style = TextStyle(
                fontFamily = JakartaFontFamily,
                fontSize = if (stampLabel.length > 8) 8.sp else 11.sp,
                lineHeight = if (stampLabel.length > 8) 9.sp else 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = if (stampLabel.length > 8) 0.3.sp else 0.8.sp,
                textAlign = TextAlign.Center,
            ),
            color = colors.skyText,
            maxLines = 2,
        )
        Text(
            text = stringResource(R.string.postcard_sticker_cap),
            style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold),
            color = colors.skyText,
        )
    }
}

private val TOWER by lazy {
    PostcardArt.TOWER.map { androidx.compose.ui.graphics.vector.PathParser().parsePathString(it).toPath() }
}

@Composable
private fun RatingRow(rating: Int?, onRating: (Int) -> Unit) {
    // Artifact: five 34 px stars, 6 px apart, unselected ones grey at 35 percent. Touch target 48 dp (pitch 48 instead of 40).
    val grey = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val groupDescription = stringResource(R.string.postcard_rating_group_cd)
    Row(
        modifier = Modifier
            .padding(top = 2.dp)
            .offset(x = (-7).dp)
            .semantics { contentDescription = groupDescription },
    ) {
        for (star in 1..5) {
            val on = rating != null && star <= rating
            val description = stringResource(R.string.postcard_rating_star_cd, star)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .selectable(selected = rating == star, role = Role.RadioButton, onClick = { onRating(star) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.postcard_ic_star),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(if (on) 1f else 0.35f),
                    colorFilter = if (on) null else grey,
                )
            }
        }
    }
}

@Composable
private fun MessageField(value: String, onChange: (String) -> Unit) {
    val colors = KelanaTheme.colors
    val label = stringResource(R.string.postcard_message_label)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = label },
        textStyle = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp, color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(KelanaRadii.input))
                    .background(colors.white)
                    .border(1.5.dp, colors.outline, RoundedCornerShape(KelanaRadii.input))
                    .padding(12.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.postcard_message_hint),
                        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 14.sp),
                        color = colors.muted,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun BottomBar(state: TulisUiState, onPublish: () -> Unit, onVideo: () -> Unit) {
    val colors = KelanaTheme.colors
    val failed = state.publish as? PublishState.Failed
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.white)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (failed != null) {
                InfoBanner(
                    text = stringResource(
                        when (failed.error) {
                            PublishError.Network -> R.string.postcard_error_network
                            PublishError.Invalid -> R.string.postcard_error_invalid
                            PublishError.NotConfigured -> R.string.postcard_error_not_configured
                        },
                    ),
                    tone = BannerTone.Warning,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Artifact: "Video" outline pill, 52 high, padding 0 16, 1.5 px blue border, play icon + 15/800 label.
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(KelanaRadii.pill))
                        .border(1.5.dp, colors.primary, RoundedCornerShape(KelanaRadii.pill))
                        .clickable(role = Role.Button, onClick = onVideo)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Image(painter = painterResource(R.drawable.postcard_ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.postcard_video_button),
                        style = TextStyle(fontFamily = JakartaFontFamily, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.primary,
                    )
                }
                KelanaButton(
                    text = stringResource(
                        when {
                            state.publish is PublishState.Sending -> R.string.postcard_publishing
                            failed != null && failed.error != PublishError.NotConfigured -> R.string.postcard_retry
                            else -> R.string.postcard_publish
                        },
                    ),
                    onClick = onPublish,
                    enabled = state.canPublish,
                    variant = KelanaButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@StringRes
private fun PostcardStyle.labelRes(): Int = when (this) {
    PostcardStyle.Jurnal -> R.string.postcard_style_jurnal
    PostcardStyle.Cap -> R.string.postcard_style_cap
    PostcardStyle.Peta -> R.string.postcard_style_peta
}

// ---- previews --------------------------------------------------------------------------------------------------

private val PreviewPlace = Place(
    "museum-sonobudoyo", "museum-sonobudoyo", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02",
    0.0, 0.0, "", "", "museum",
)

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun TulisFormPreview() = KelanaTheme {
    TulisContent(TulisUiState(mode = TulisMode.Form(PreviewPlace), message = "Koleksi keris dan wayangnya lengkap."), TulisActions())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun TulisSendingPreview() = KelanaTheme {
    TulisContent(
        TulisUiState(mode = TulisMode.Form(PreviewPlace), message = "Halo", rating = 4, publish = PublishState.Sending),
        TulisActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun TulisErrorPreview() = KelanaTheme {
    TulisContent(
        TulisUiState(mode = TulisMode.Form(PreviewPlace), message = "Halo", publish = PublishState.Failed(PublishError.Network)),
        TulisActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun TulisPickPreview() = KelanaTheme {
    TulisContent(
        TulisUiState(mode = TulisMode.PickPlace(listOf(StampedPlace(PreviewPlace, Instant.parse("2026-11-20T03:00:00Z"))))),
        TulisActions(),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun TulisNoStampsPreview() = KelanaTheme { TulisContent(TulisUiState(mode = TulisMode.NoStamps), TulisActions()) }

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun TulisNeedsStampPreview() = KelanaTheme {
    TulisContent(TulisUiState(mode = TulisMode.NeedsStamp(PreviewPlace)), TulisActions())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun TulisGuestPreview() = KelanaTheme { TulisContent(TulisUiState(mode = TulisMode.Guest), TulisActions()) }

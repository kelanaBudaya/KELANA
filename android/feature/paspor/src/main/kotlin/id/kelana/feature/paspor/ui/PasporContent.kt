package id.kelana.feature.paspor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.feature.paspor.R
import id.kelana.feature.paspor.domain.PassportItem
import java.time.Instant
import java.util.Locale
import id.kelana.core.designsystem.R as DsR

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")
private val StickerWidth = 96.dp
private val StickerHeight = 80.dp

internal class PasporActions(
    val onBack: () -> Unit,
    val onProfile: () -> Unit,
    val onPlace: (Place) -> Unit,
    val onPostcard: () -> Unit,
    val onLogin: () -> Unit,
)

/** Paspor budaya (Figma 7.4). */
@Composable
internal fun PasporContent(state: PasporUiState, actions: PasporActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    when {
        state.isLoading -> Box(
            modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = colors.primary) }

        state.isGuest -> Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
            KelanaBackTopBar(title = stringResource(R.string.paspor_screen_title), onBack = actions.onBack)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.paspor_guest_title),
                    body = stringResource(R.string.paspor_guest_body),
                    actionLabel = stringResource(R.string.paspor_login),
                    onAction = actions.onLogin,
                )
            }
        }

        state.items.isEmpty() -> Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
            KelanaBackTopBar(title = stringResource(R.string.paspor_screen_title), onBack = actions.onBack)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.paspor_no_places_title),
                    body = stringResource(R.string.paspor_no_places_body),
                )
            }
        }

        else -> Column(
            modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()),
        ) {
            Header(state, actions)
            StampSection(state, actions)
            state.latestStamped?.let { PostcardPrompt(it.place, actions.onPostcard) }
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun Header(state: PasporUiState, actions: PasporActions) {
    val colors = KelanaTheme.colors
    val progress = if (state.total == 0) 0f else state.collected.toFloat() / state.total
    Box(Modifier.fillMaxWidth().background(colors.stampBrown)) {
        Image(
            painter = painterResource(R.drawable.paspor_bg_header_wave),
            contentDescription = null,
            modifier = Modifier.matchParentSizeTop(),
            contentScale = ContentScale.FillBounds,
        )
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val back = stringResource(DsR.string.ds_back)
                Box(
                    Modifier.size(40.dp).clickable(role = Role.Button, onClickLabel = back, onClick = actions.onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(DsR.drawable.ic_back),
                        contentDescription = back,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(colors.white),
                    )
                }
                Text(
                    text = state.regionName?.let { stringResource(R.string.paspor_kicker, it.uppercase(IdLocale)) }
                        ?: stringResource(R.string.paspor_kicker_plain),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                    color = colors.stampYellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val profile = stringResource(R.string.paspor_profile_cd)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.stampYellow)
                        .clickable(role = Role.Button, onClickLabel = profile, onClick = actions.onProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.initials,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.navy,
                    )
                }
            }
            Row(
                Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CapSeal(
                    top = stringResource(R.string.paspor_seal_name),
                    bottom = state.regionCode.orEmpty(),
                    center = "${state.collected}/${state.total}",
                    size = 96.dp,
                )
                Column {
                    Text(
                        stringResource(R.string.paspor_count_title, state.collected, state.total),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 30.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.white,
                    )
                    Text(
                        text = if (state.remaining > 0) {
                            stringResource(R.string.paspor_remaining, state.remaining)
                        } else {
                            stringResource(R.string.paspor_complete)
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = colors.peach,
                    )
                }
            }
            val progressCd = stringResource(R.string.paspor_progress_cd, state.collected, state.total)
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(colors.white.copy(alpha = 0.2f))
                    .semantics {
                        contentDescription = progressCd
                        progressBarRangeInfo = ProgressBarRangeInfo(state.collected.toFloat(), 0f..state.total.toFloat())
                    },
            ) {
                if (progress > 0f) {
                    Box(Modifier.fillMaxWidth(progress).height(10.dp).clip(CircleShape).background(colors.stampYellow))
                }
            }
        }
    }
}

/** The wave art is drawn over the top 240 dp of the header like the artifact; it never decides the header height. */
private fun Modifier.matchParentSizeTop(): Modifier = this.fillMaxWidth().height(240.dp)

@Composable
private fun StampSection(state: PasporUiState, actions: PasporActions) {
    val colors = KelanaTheme.colors
    Column(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 20.dp)) {
        Text(
            stringResource(R.string.paspor_section_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Text(
            text = if (state.collected == 0) {
                stringResource(R.string.paspor_section_empty)
            } else {
                stringResource(R.string.paspor_section_hint)
            },
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            val columns = 3
            val gap = 8.dp
            val cell = (maxWidth - gap * (columns - 1)) / columns
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.items.withIndex().toList().chunked(columns).forEach { row ->
                    // A last row with fewer items is centred, like the tenth stamp in the artifact.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { (index, item) ->
                            StampCell(item, index, Modifier.width(cell), onClick = { actions.onPlace(item.place) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampCell(item: PassportItem, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val place = item.place
    val description = if (item.isCollected) {
        stringResource(R.string.paspor_stamp_collected_cd, place.name)
    } else {
        stringResource(R.string.paspor_stamp_missing_cd, place.name)
    }
    Column(
        modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (item.isCollected) {
            CapSticker(
                label = stampLabel(place.name, IdLocale),
                code = stringResource(R.string.paspor_sticker_number, place.capCode),
                icon = place.category.stampIcon(),
                tone = stickerToneFor(index),
                width = StickerWidth,
                height = StickerHeight,
                iconSize = 18.dp,
                modifier = Modifier.rotate(if (index % 2 == 0) -5f else 4f),
                labelSize = stampLabelSize(stampLabel(place.name, IdLocale)),
            )
        } else {
            val dash = colors.divider
            Box(
                Modifier
                    .size(StickerWidth, StickerHeight)
                    .alpha(0.8f)
                    .drawBehind {
                        drawRoundRect(
                            color = dash,
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(place.category.stampIcon()),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp).alpha(0.4f),
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                )
            }
        }
        Text(
            place.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (item.isCollected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (item.isCollected) colors.ink else colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PostcardPrompt(place: Place, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        Modifier
            .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 20.dp)
            .fillMaxWidth()
            .clip(KelanaShapes.listCard)
            .background(colors.creamYellow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(R.drawable.paspor_art_postcard), contentDescription = null, modifier = Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.paspor_postcard_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Text(
                stringResource(R.string.paspor_postcard_subtitle, place.name),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.warnTextDeep,
            )
        }
        Image(
            painter = painterResource(DsR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(colors.warnTextDeep),
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------------

private fun previewPlace(slug: String, name: String, category: PlaceCategory, code: String) =
    Place(slug, slug, name, category, code, 0.0, 0.0, "", "", slug)

private val PreviewPlaces = listOf(
    previewPlace("keraton", "Keraton Yogyakarta", PlaceCategory.Lainnya, "YK-01"),
    previewPlace("sono", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02"),
    previewPlace("tamansari", "Tamansari", PlaceCategory.Lainnya, "YK-03"),
    previewPlace("kereta", "Museum Kereta Wahanarata", PlaceCategory.Museum, "YK-04"),
    previewPlace("vredeburg", "Museum Benteng Vredeburg", PlaceCategory.Museum, "YK-05"),
    previewPlace("puro", "Puro Pakualaman", PlaceCategory.Lainnya, "YK-06"),
    previewPlace("batik", "Museum Batik Yogyakarta", PlaceCategory.Museum, "YK-07"),
    previewPlace("tugu", "Tugu Yogyakarta", PlaceCategory.Lainnya, "YK-08"),
    previewPlace("kotagede", "Kotagede", PlaceCategory.Kerajinan, "YK-09"),
    previewPlace("prambanan", "Candi Prambanan", PlaceCategory.Candi, "YK-10"),
)

private val NoActions = PasporActions({}, {}, {}, {}, {})

@Preview(widthDp = 390, heightDp = 1220, name = "7.4 Paspor, 3 of 10")
@Composable
private fun PreviewPaspor() {
    val stamped = Instant.parse("2026-11-20T03:00:00Z")
    KelanaTheme {
        PasporContent(
            PasporUiState(
                isLoading = false,
                initials = "MH",
                regionName = "DI Yogyakarta",
                regionCode = "DIY",
                items = PreviewPlaces.mapIndexed { i, p -> PassportItem(p, if (i < 3) stamped.plusSeconds(i * 60L) else null) },
            ),
            NoActions,
        )
    }
}

@Preview(widthDp = 390, heightDp = 1000, name = "7.4 Paspor, no stamp yet")
@Composable
private fun PreviewPasporEmpty() {
    KelanaTheme {
        PasporContent(
            PasporUiState(isLoading = false, initials = "MH", regionName = "DI Yogyakarta", regionCode = "DIY",
                items = PreviewPlaces.map { PassportItem(it, null) }),
            NoActions,
        )
    }
}

@Preview(widthDp = 390, heightDp = 700, name = "7.4 Paspor, guest")
@Composable
private fun PreviewPasporGuest() {
    KelanaTheme { PasporContent(PasporUiState(isLoading = false, isGuest = true), NoActions) }
}

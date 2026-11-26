package id.kelana.feature.paspor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.R as DsR
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.feature.paspor.R
import id.kelana.feature.paspor.domain.StampFailure
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.ceil

/** What the user can do on the Ambil cap screens. */
internal class AmbilCapActions(
    val onBack: () -> Unit,
    val onAllowLocation: () -> Unit,
    val onCapNow: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onCancelPending: () -> Unit,
    val onPendingExpired: () -> Unit,
    val onDirections: () -> Unit,
    val onLogin: () -> Unit,
)

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** Stateless Ambil cap: one screen per [CapStep]. */
@Composable
internal fun AmbilCapContent(state: AmbilCapUiState, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val place = state.place
    val step = state.step
    when {
        step is CapStep.Explain && place != null -> IzinLokasiSheet(place, actions, modifier)
        state.placeStatus == PlaceStatus.Loading || step is CapStep.Checking -> Box(
            modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = colors.primary) }
        place == null -> MessageScreen(
            title = stringResource(R.string.paspor_place_missing_title),
            body = stringResource(R.string.paspor_place_missing_body),
            actions = actions,
            primary = null,
            modifier = modifier,
        )
        else -> when (step) {
            is CapStep.Denied -> DitolakContent(step, actions, modifier)
            is CapStep.Ready -> ReadyContent(place, working = null, actions, modifier)
            is CapStep.Working -> ReadyContent(place, working = step, actions, modifier)
            is CapStep.WaitingSignal -> PendingContent(place, step, actions, modifier)
            is CapStep.OutOfArea -> DiLuarContent(place, step, actions, modifier)
            is CapStep.Failed -> FailureContent(step.failure, actions, modifier)
            CapStep.Explain, CapStep.Checking -> Unit
        }
    }
}

@Composable
private fun ScreenFrame(
    actions: AmbilCapActions,
    modifier: Modifier = Modifier,
    /** 6.2 draws the bar as 16/700 (default); 6.4 and 6.5 as 17/800 with a smaller top inset and a wider gap. */
    wideTitleBar: Boolean = false,
    bottom: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize().background(KelanaTheme.colors.background).statusBarsPadding()) {
        AmbilCapTopBar(wideTitleBar, actions.onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { body() }
        if (bottom != null) {
            // Artifact: 6.2 button sits 28 from the bottom, 6.4 and 6.5 pad 12 / 20 / 26.
            Box(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = if (wideTitleBar) 26.dp else 28.dp)
                    .navigationBarsPadding(),
            ) { bottom() }
        }
    }
}

/**
 * Top bar of the Ambil cap screens. The design-system bar is 56 dp with a 16/800 title; the artifact draws 6.2 with
 * 12 dp inset and 16/700, and 6.4 / 6.5 with 10 dp inset, 6 dp gap and 17/800. The tap target stays 48 dp
 * (artifact 44), so the inset is reduced by 2 dp to keep the icon where the artifact has it.
 */
@Composable
private fun AmbilCapTopBar(wide: Boolean, onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    val back = stringResource(DsR.string.ds_back)
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = if (wide) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (wide) 4.dp else 2.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = back, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(DsR.drawable.ic_back), contentDescription = back, modifier = Modifier.size(22.dp))
        }
        Text(
            stringResource(R.string.paspor_ambil_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (wide) 17.sp else 16.sp,
                fontWeight = if (wide) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            color = colors.ink,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 6.1 Izin lokasi
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun IzinLokasiSheet(place: Place, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Box(modifier.fillMaxSize().background(colors.primary)) {
        Box(Modifier.fillMaxSize().background(colors.inkDeep.copy(alpha = 0.55f)))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(PanelRadius))
                .background(colors.background),
        ) {
            Box {
                Scene(366f, 250f, Modifier.background(colors.stampYellow)) { s ->
                    SceneBackground(R.drawable.paspor_bg_izin)
                    SceneIzinRoute(s, colors.primaryDeepAlt)
                    ScenePiece(s, R.drawable.paspor_art_tugu, 40f, 58f, 110f, -6f)
                    ScenePiece(s, R.drawable.paspor_art_footprint, 150f, 150f, 58f, 170f)
                    ScenePiece(s, R.drawable.paspor_art_museum, 214f, 36f, 92f, 5f)
                    CapSticker(
                        label = stampLabel(place.name, IdLocale),
                        code = stringResource(R.string.paspor_sticker_word),
                        icon = place.category.stampIcon(),
                        tone = StickerTone.Blue,
                        width = s.dp(86f),
                        height = s.dp(62f),
                        iconSize = 18.dp,
                        modifier = Modifier.offset(s.dp(236f), s.dp(150f)).rotate(9f),
                    )
                }
                val closeLabel = stringResource(R.string.paspor_close)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.white.copy(alpha = 0.9f))
                        .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = actions.onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.paspor_ic_close), contentDescription = closeLabel)
                }
            }
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp)) {
                Text(
                    stringResource(R.string.paspor_izin_title),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.paspor_izin_body),
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PasporButton(
                        stringResource(R.string.paspor_izin_later),
                        actions.onBack,
                        Modifier.weight(1f),
                        style = PasporButtonStyle.Outlined,
                        height = 48.dp,
                        fontSize = 15.sp,
                    )
                    PasporButton(
                        stringResource(R.string.paspor_izin_allow),
                        actions.onAllowLocation,
                        Modifier.weight(1f),
                        style = PasporButtonStyle.Primary,
                        height = 48.dp,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 6.2 Ambil cap
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun ReadyContent(place: Place, working: CapStep.Working?, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    ScreenFrame(
        actions,
        modifier,
        bottom = {
            PasporButton(
                text = when {
                    working == null -> stringResource(R.string.paspor_ambil_button)
                    working.accuracyM == null -> stringResource(R.string.paspor_ambil_locating)
                    else -> stringResource(R.string.paspor_ambil_sending)
                },
                onClick = actions.onCapNow,
                modifier = Modifier.fillMaxWidth(),
                enabled = working == null,
            )
        },
    ) {
        PanelFrame(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 4.dp)) {
            Scene(358f, 300f, Modifier.background(colors.successSurface)) { s ->
                SceneBackground(R.drawable.paspor_bg_ambil)
                SceneAreaCircle(s, 180f, 128f, 104f, colors.successText, 0.08f)
                SceneLocationDot(s, 150f, 200f, colors.primaryBright, halo = true)
                ScenePiece(s, R.drawable.paspor_art_museum, 132f, 80f, 96f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 60f, 230f, 30f, 200f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 96f, 196f, 30f, 210f)
                // The radius is not known on the client (the server decides), so the badge carries no number.
                Text(
                    stringResource(R.string.paspor_ambil_area_badge),
                    Modifier
                        .offset(s.dp(14f), s.dp(14f))
                        .clip(KelanaShapes.chip)
                        .background(colors.white)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.successText,
                )
            }
        }
        Row(
            Modifier
                .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 14.dp)
                .fillMaxWidth()
                .clip(KelanaShapes.listCard)
                .background(colors.successSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.paspor_art_check), contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(
                    stringResource(R.string.paspor_ambil_banner_title, place.name),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.successTextDeep,
                )
                // Accuracy only from the single fix, and only while it is being sent. Otherwise the address.
                Text(
                    text = if (working?.accuracyM != null) {
                        stringResource(R.string.paspor_ambil_banner_accuracy, working.accuracyM)
                    } else {
                        place.address
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                    color = colors.successTextDeep,
                )
            }
        }
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CapSticker(
                label = stampLabel(place.name, IdLocale),
                code = stringResource(R.string.paspor_sticker_number, place.capCode),
                icon = place.category.stampIcon(),
                tone = StickerTone.Blue,
                width = 112.dp,
                height = 82.dp,
                iconSize = 24.dp,
                modifier = Modifier.rotate(-5f),
                labelSize = stampLabelSize(stampLabel(place.name, IdLocale)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.paspor_ambil_sticker_title, place.name),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                )
                Text(
                    stringResource(R.string.paspor_ambil_sticker_body),
                    Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(R.drawable.paspor_art_shield), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.paspor_ambil_privacy),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                color = colors.muted,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// 6.4 Di luar area
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun DiLuarContent(place: Place, step: CapStep.OutOfArea, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    ScreenFrame(
        actions,
        modifier,
        wideTitleBar = true,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PasporButton(
                    stringResource(R.string.paspor_diluar_directions),
                    actions.onDirections,
                    Modifier.weight(1f),
                    style = PasporButtonStyle.Outlined,
                )
                PasporButton(stringResource(R.string.paspor_retry), actions.onRetry, Modifier.weight(1f))
            }
        },
    ) {
        PanelFrame(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 4.dp)) {
            Scene(358f, 300f, Modifier.background(colors.creamYellow)) { s ->
                SceneBackground(R.drawable.paspor_bg_diluar)
                SceneAreaCircle(s, 266f, 96f, 76f, colors.primary, 0.06f)
                SceneLocationDot(s, 90f, 250f, colors.primaryBright, halo = false)
                ScenePiece(s, R.drawable.paspor_art_museum, 226f, 50f, 80f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 70f, 220f, 34f, 150f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 120f, 180f, 34f, 140f)
            }
        }
        Text(
            stringResource(R.string.paspor_diluar_title),
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        // The distance is the server's number. Without one the sentence names no number.
        Text(
            text = if (step.distanceM != null) {
                stringResource(R.string.paspor_diluar_body_distance, formatCount(step.distanceM), place.name)
            } else {
                stringResource(R.string.paspor_diluar_body_unknown, place.name)
            },
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance(IdLocale).format(value)

// ---------------------------------------------------------------------------------------------------------------
// 6.5 Lokasi ditolak
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun DitolakPanel() {
    val colors = KelanaTheme.colors
    PanelFrame(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 4.dp)) {
        Scene(358f, 220f, Modifier.background(colors.skyText)) { s ->
            SceneBackground(R.drawable.paspor_bg_ditolak)
            ScenePiece(s, R.drawable.paspor_art_tugu, 60f, 40f, 90f, -8f)
            ScenePiece(s, R.drawable.paspor_art_shield, 220f, 70f, 70f, 8f)
        }
    }
}

@Composable
private fun DitolakContent(step: CapStep.Denied, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    ScreenFrame(
        actions,
        modifier,
        wideTitleBar = true,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step.canAskAgain) {
                    PasporButton(
                        stringResource(R.string.paspor_open_settings),
                        actions.onOpenSettings,
                        Modifier.weight(1f),
                        style = PasporButtonStyle.Outlined,
                    )
                    PasporButton(stringResource(R.string.paspor_retry), actions.onAllowLocation, Modifier.weight(1f))
                } else {
                    PasporButton(
                        stringResource(R.string.paspor_open_settings),
                        actions.onOpenSettings,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        DitolakPanel()
        Text(
            stringResource(R.string.paspor_ditolak_title),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.ink,
        )
        Text(
            stringResource(R.string.paspor_ditolak_body),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                R.string.paspor_ditolak_step1,
                R.string.paspor_ditolak_step2,
                R.string.paspor_ditolak_step3,
            ).forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                            color = colors.white,
                        )
                    }
                    Text(
                        AnnotatedString.fromHtml(stringResource(step)),
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Errors (no artifact screen): same panel and type as 6.4 and 6.5, one copy per failure.
// ---------------------------------------------------------------------------------------------------------------

private class FailureCopy(@param:StringRes val title: Int, @param:StringRes val body: Int)

private fun StampFailure.copy(): FailureCopy = when (this) {
    StampFailure.LowAccuracy -> FailureCopy(R.string.paspor_err_accuracy_title, R.string.paspor_err_accuracy_body)
    StampFailure.Expired -> FailureCopy(R.string.paspor_err_expired_title, R.string.paspor_err_expired_body)
    StampFailure.ClockOff -> FailureCopy(R.string.paspor_err_clock_title, R.string.paspor_err_clock_body)
    StampFailure.IntegrityFailed -> FailureCopy(R.string.paspor_err_integrity_title, R.string.paspor_err_integrity_body)
    StampFailure.RadiusNotSet -> FailureCopy(R.string.paspor_err_radius_title, R.string.paspor_err_radius_body)
    StampFailure.MockLocation -> FailureCopy(R.string.paspor_err_mock_title, R.string.paspor_err_mock_body)
    StampFailure.PlaceUnknown -> FailureCopy(R.string.paspor_err_place_title, R.string.paspor_err_place_body)
    StampFailure.BadRequest -> FailureCopy(R.string.paspor_err_request_title, R.string.paspor_err_request_body)
    StampFailure.NotConfigured -> FailureCopy(R.string.paspor_err_config_title, R.string.paspor_err_config_body)
    StampFailure.NoFix -> FailureCopy(R.string.paspor_err_nofix_title, R.string.paspor_err_nofix_body)
    StampFailure.NeedsLogin -> FailureCopy(R.string.paspor_err_login_title, R.string.paspor_err_login_body)
    StampFailure.Unconfirmed -> FailureCopy(R.string.paspor_err_unconfirmed_title, R.string.paspor_err_unconfirmed_body)
}

/** Failures where trying again cannot help in this build or for this place. */
private fun StampFailure.canRetry(): Boolean = when (this) {
    StampFailure.NotConfigured, StampFailure.PlaceUnknown, StampFailure.RadiusNotSet, StampFailure.NeedsLogin -> false
    else -> true
}

@Composable
private fun FailureContent(failure: StampFailure, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val copy = failure.copy()
    MessageScreen(
        title = stringResource(copy.title),
        body = stringResource(copy.body),
        actions = actions,
        primary = when {
            failure == StampFailure.NeedsLogin -> stringResource(R.string.paspor_login) to actions.onLogin
            failure.canRetry() -> stringResource(R.string.paspor_retry) to actions.onRetry
            else -> null
        },
        modifier = modifier,
    )
}

@Composable
private fun MessageScreen(
    title: String,
    body: String,
    actions: AmbilCapActions,
    primary: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    ScreenFrame(
        actions,
        modifier,
        wideTitleBar = true,
        bottom = primary?.let { (label, onClick) -> { PasporButton(label, onClick, Modifier.fillMaxWidth()) } },
    ) {
        DitolakPanel()
        Text(
            title,
            Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.ink,
        )
        Text(
            body,
            Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// PROVISIONAL: "Menunggu sinyal". Not in the artifact; built from the 6.2 / 6.4 tokens and drawables.
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun PendingContent(place: Place, step: CapStep.WaitingSignal, actions: AmbilCapActions, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val now by produceState(Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(REMAINING_TICK_MS)
        }
    }
    val remaining = Duration.between(now, step.expiresAt)
    val expired = remaining.isNegative || remaining.isZero
    LaunchedEffect(expired) { if (expired) actions.onPendingExpired() }
    val minutes = ceil(remaining.seconds / 60.0).toInt()

    ScreenFrame(
        actions,
        modifier,
        wideTitleBar = true,
        bottom = {
            PasporButton(
                stringResource(R.string.paspor_pending_cancel),
                actions.onCancelPending,
                Modifier.fillMaxWidth(),
                style = PasporButtonStyle.Outlined,
            )
        },
    ) {
        PanelFrame(Modifier.padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 4.dp)) {
            Scene(358f, 220f, Modifier.background(colors.skyText)) { s ->
                SceneBackground(R.drawable.paspor_bg_ditolak)
                ScenePiece(s, R.drawable.paspor_art_footprint, 40f, 120f, 40f, 200f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 92f, 90f, 40f, 210f)
                ScenePiece(s, R.drawable.paspor_art_footprint, 144f, 60f, 40f, 200f)
                CapSticker(
                    label = stampLabel(place.name, IdLocale),
                    code = stringResource(R.string.paspor_sticker_number, place.capCode),
                    icon = place.category.stampIcon(),
                    tone = StickerTone.Blue,
                    width = s.dp(112f),
                    height = s.dp(82f),
                    iconSize = 24.dp,
                    modifier = Modifier.offset(s.dp(210f), s.dp(60f)).rotate(6f),
                    labelSize = stampLabelSize(stampLabel(place.name, IdLocale)),
                )
            }
        }
        Text(
            stringResource(R.string.paspor_pending_title),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.ink,
        )
        Text(
            stringResource(R.string.paspor_pending_body),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Text(
            text = if (minutes <= 1) {
                stringResource(R.string.paspor_pending_less_than_minute)
            } else {
                pluralStringResource(R.plurals.paspor_pending_remaining, minutes, minutes)
            },
            modifier = Modifier
                .padding(start = 24.dp, top = 14.dp, bottom = 16.dp)
                .clip(KelanaShapes.chip)
                .background(colors.creamYellow)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.warnTextDeep,
        )
    }
}

private const val REMAINING_TICK_MS = 15_000L

// ---------------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------------

private val PreviewPlace = Place(
    id = "museum-sonobudoyo",
    slug = "museum-sonobudoyo",
    name = "Museum Sonobudoyo",
    category = PlaceCategory.Museum,
    capCode = "YK-02",
    lat = 0.0,
    lng = 0.0,
    address = "Jl. Pangurakan No. 6, Ngupasan, Gondomanan",
    description = "",
    coverKey = "sonobudoyo",
)

private val NoActions = AmbilCapActions({}, {}, {}, {}, {}, {}, {}, {}, {})

@Composable
private fun PreviewStep(step: CapStep) {
    KelanaTheme {
        AmbilCapContent(AmbilCapUiState(PlaceStatus.Loaded, PreviewPlace, step), NoActions)
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "6.1 Izin lokasi")
@Composable
private fun PreviewExplain() = PreviewStep(CapStep.Explain)

@Preview(widthDp = 390, heightDp = 844, name = "6.2 Ambil cap")
@Composable
private fun PreviewReady() = PreviewStep(CapStep.Ready)

@Preview(widthDp = 390, heightDp = 844, name = "6.2 Ambil cap, sending")
@Composable
private fun PreviewWorking() = PreviewStep(CapStep.Working(accuracyM = 18))

@Preview(widthDp = 390, heightDp = 844, name = "6.4 Di luar area")
@Composable
private fun PreviewOutOfArea() = PreviewStep(CapStep.OutOfArea(distanceM = 420))

@Preview(widthDp = 390, heightDp = 844, name = "6.4 Di luar area, no distance")
@Composable
private fun PreviewOutOfAreaUnknown() = PreviewStep(CapStep.OutOfArea(distanceM = null))

@Preview(widthDp = 390, heightDp = 844, name = "6.5 Lokasi ditolak")
@Composable
private fun PreviewDenied() = PreviewStep(CapStep.Denied(canAskAgain = true))

@Preview(widthDp = 390, heightDp = 844, name = "6.5 Lokasi ditolak, dont ask again")
@Composable
private fun PreviewDeniedPermanent() = PreviewStep(CapStep.Denied(canAskAgain = false))

@Preview(widthDp = 390, heightDp = 844, name = "PROVISIONAL Menunggu sinyal")
@Composable
private fun PreviewWaiting() = PreviewStep(CapStep.WaitingSignal(capturedAt = Instant.now().minusSeconds(5 * 60)))

@Preview(widthDp = 390, heightDp = 844, name = "Error: akurasi")
@Composable
private fun PreviewFailureAccuracy() = PreviewStep(CapStep.Failed(StampFailure.LowAccuracy))

@Preview(widthDp = 390, heightDp = 844, name = "Error: login")
@Composable
private fun PreviewFailureLogin() = PreviewStep(CapStep.Failed(StampFailure.NeedsLogin))

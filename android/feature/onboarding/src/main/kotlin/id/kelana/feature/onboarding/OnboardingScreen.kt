package id.kelana.feature.onboarding

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus
import id.kelana.core.navigation.IntroRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PilihDaerahRoute

/**
 * Registers this feature's destinations: Mulai + Kenalan (`IntroRoute` pages 0..3) and Pilih daerah.
 * Called by :app only; this signature is stable. [navigate] takes a route object from :core:navigation;
 * [onFinished] ends onboarding (the app stores the flag and opens Jelajah).
 */
fun NavGraphBuilder.onboardingScreens(navigate: (Any) -> Unit, onFinished: () -> Unit) {
    composable<IntroRoute> { entry -> IntroScreen(route = entry.toRoute(), navigate = navigate, onFinished = onFinished) }
    composable<PilihDaerahRoute> { PilihDaerahScreen(navigate = navigate, onFinished = onFinished) }
}

// ---------------------------------------------------------------------------------------------------------
// Mulai + Kenalan
// ---------------------------------------------------------------------------------------------------------

@Composable
internal fun IntroScreen(
    route: IntroRoute,
    navigate: (Any) -> Unit,
    onFinished: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel(),
) {
    val finished by viewModel.finished.collectAsState()
    LaunchedEffect(finished) {
        if (finished) {
            viewModel.onFinishedHandled()
            onFinished()
        }
    }
    val state = IntroUiState.forPage(route.page)
    IntroContent(
        state = state,
        onNext = { next -> navigate(IntroRoute(next)) },
        onSkip = { navigate(PilihDaerahRoute) },
        onStartFromJogja = viewModel::startFromJogja,
        onOtherRegion = { navigate(PilihDaerahRoute) },
        onHaveAccount = { navigate(MasukRoute) },
    )
}

@Composable
internal fun IntroContent(
    state: IntroUiState,
    onNext: (Int) -> Unit,
    onSkip: () -> Unit,
    onStartFromJogja: () -> Unit,
    onOtherRegion: () -> Unit,
    onHaveAccount: () -> Unit,
) {
    val intro = state.intro
    if (state.isMulai || intro == null) {
        MulaiContent(onStart = { onNext(IntroPage.OfficialInfo.page) }, onHaveAccount = onHaveAccount)
    } else {
        KenalanContent(intro, onNext, onSkip, onStartFromJogja, onOtherRegion, state.stepCount)
    }
}

/** Frame 01.01. The logo mark and the curved background swirl are PROVISIONAL shapes. */
@Composable
private fun MulaiContent(onStart: () -> Unit, onHaveAccount: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.primary)) {
        // PROVISIONAL swirl behind the logo.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(colors.primaryBrightAlt.copy(alpha = 0.5f), radius = size.width * 0.55f, center = Offset(size.width * 0.5f, size.height * 0.45f))
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = KelanaSpacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // PROVISIONAL logo mark: yellow dot over a white shape.
            val logoDescription = stringResource(R.string.mulai_logo_description)
            Box(Modifier.size(120.dp).semantics { contentDescription = logoDescription }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(88.dp).clip(RoundedCornerShape(32.dp)).background(colors.background))
                Box(Modifier.size(28.dp).clip(CircleShape).background(colors.stampYellow).align(Alignment.TopStart))
            }
            Spacer(Modifier.height(KelanaSpacing.md))
            Text(
                text = stringResource(R.string.mulai_wordmark),
                color = colors.white,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(KelanaSpacing.sm))
            Text(
                text = stringResource(R.string.mulai_tagline),
                color = colors.stampYellow,
                fontFamily = FrauncesFontFamily,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.2f))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.stampYellow, contentColor = colors.navy),
            ) { Text(stringResource(R.string.mulai_start), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            TextButton(onClick = onHaveAccount, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.onboarding_have_account), color = colors.white, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(KelanaSpacing.sm))
        }
    }
}

/** Frames 01.02 to 01.04. The illustration panel is a PROVISIONAL placeholder (no SVG drawable in :core:designsystem yet). */
@Composable
private fun KenalanContent(
    intro: IntroPage,
    onNext: (Int) -> Unit,
    onSkip: () -> Unit,
    onStartFromJogja: () -> Unit,
    onOtherRegion: () -> Unit,
    stepCount: Int,
) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepIndicator(current = intro.step, count = stepCount)
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.intro_skip), color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        IntroIllustration(intro, Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter).height(400.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = KelanaSpacing.lg),
        ) {
            Text(
                stringResource(intro.title),
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(KelanaSpacing.sm))
            Text(stringResource(intro.body), color = colors.muted, style = MaterialTheme.typography.bodyMedium, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter).padding(bottom = KelanaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (intro.isLast) {
                RoundButton(stringResource(R.string.intro3_start_jogja), onStartFromJogja)
                TextButton(onClick = onOtherRegion, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.intro3_other_region), color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                RoundButton(stringResource(R.string.intro_next), onClick = { onNext(intro.page + 1) })
            }
        }
    }
}

@Composable
private fun RoundButton(text: String, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.white),
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/** Three-step indicator: the active step is a wide pill. */
@Composable
internal fun StepIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.intro_progress, current + 1, count)
    Row(
        modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == current) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == current) colors.primary else colors.outline),
            )
        }
    }
}

/** PROVISIONAL placeholder panel: tinted background, a soft blob and a simple motif. Replace with the SVG drawables. */
@Composable
private fun IntroIllustration(intro: IntroPage, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val (panel, blob, motif) = when (intro) {
        IntroPage.OfficialInfo -> Triple(colors.skyText, colors.skyText.copy(alpha = 0.6f), colors.primaryDeep)
        IntroPage.ShowSchedule -> Triple(colors.accentOrange.copy(alpha = 0.15f), colors.accentOrangeBright.copy(alpha = 0.25f), colors.accentOrange)
        IntroPage.Stamp -> Triple(colors.creamYellow, colors.stampYellow.copy(alpha = 0.4f), colors.primary)
    }
    val description = stringResource(
        when (intro) {
            IntroPage.OfficialInfo -> R.string.intro1_illustration
            IntroPage.ShowSchedule -> R.string.intro2_illustration
            IntroPage.Stamp -> R.string.intro3_illustration
        },
    )
    Canvas(modifier.semantics { contentDescription = description }) {
        drawRect(panel)
        drawOval(blob, topLeft = Offset(size.width * 0.08f, size.height * 0.1f), size = Size(size.width * 0.82f, size.height * 0.82f))
        drawRoundRect(
            motif,
            topLeft = Offset(size.width * 0.34f, size.height * 0.3f),
            size = Size(size.width * 0.32f, size.height * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
        )
        drawCircle(colors.stampYellow, radius = size.width * 0.05f, center = Offset(size.width * 0.2f, size.height * 0.72f))
    }
}

// ---------------------------------------------------------------------------------------------------------
// Pilih daerah
// ---------------------------------------------------------------------------------------------------------

@Composable
internal fun PilihDaerahScreen(
    navigate: (Any) -> Unit,
    onFinished: () -> Unit,
    viewModel: PilihDaerahViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.finished) {
        if (state.finished) {
            viewModel.onFinishedHandled()
            onFinished()
        }
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    PilihDaerahContent(
        state = state,
        onBack = { backDispatcher?.onBackPressed() },
        onPinTapped = viewModel::onPinTapped,
        onChoose = viewModel::choose,
    )
}

@Composable
internal fun PilihDaerahContent(
    state: PilihDaerahUiState,
    onBack: () -> Unit,
    onPinTapped: (String) -> Unit,
    onChoose: (String) -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        KelanaBackTopBar(title = stringResource(R.string.pilih_title), onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KelanaSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            Text(stringResource(R.string.pilih_intro), color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp)
            when {
                state.loading -> Text(stringResource(R.string.pilih_loading), color = colors.muted)
                state.regions.isEmpty() -> Text(stringResource(R.string.pilih_empty), color = colors.muted)
                else -> {
                    MapPanel(state, onPinTapped, onChoose)
                    Text(stringResource(R.string.pilih_map_hint), color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
                    if (state.availableRegions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.pilih_available_heading),
                            color = colors.muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                        state.availableRegions.forEach { AvailableRegionCard(it, state, onChoose) }
                    }
                    Text(
                        stringResource(R.string.pilih_footer, state.upcomingCount, state.notOpenCount),
                        color = colors.muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    // Visual only: no suggestion flow exists yet.
                    SuggestCard()
                    Spacer(Modifier.height(KelanaSpacing.lg))
                }
            }
        }
    }
}

@Composable
private fun MapPanel(state: PilihDaerahUiState, onPinTapped: (String) -> Unit, onChoose: (String) -> Unit) {
    val colors = KelanaTheme.colors
    val mapDescription = stringResource(R.string.pilih_map_description)
    Box(
        Modifier
            .fillMaxWidth()
            .height(326.dp) // PROVISIONAL: height measured from the 390 px frame
            .clip(RoundedCornerShape(20.dp))
            .semantics { contentDescription = mapDescription },
    ) {
        ArchipelagoBackdrop(Modifier.fillMaxSize())
        val pins = state.regions.mapIndexed { index, region ->
            RegionPin(region, pinPositionFor(region.id, index), selected = region.id == state.selectedRegionId)
        }
        RegionPins(pins, onPinTapped, Modifier.fillMaxSize())

        // "Lihat seluruh Indonesia" chip and zoom buttons are visual only (no map SDK yet).
        Text(
            stringResource(R.string.pilih_all_indonesia),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(colors.white)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.navy,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
        Column(Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 8.dp)).background(colors.white)) {
            ZoomButton("+", stringResource(R.string.pilih_zoom_in))
            ZoomButton("−", stringResource(R.string.pilih_zoom_out))
        }

        state.selectedRegion?.let { region ->
            Callout(
                region = region,
                state = state,
                onChoose = onChoose,
                modifier = Modifier.align(Alignment.Center).padding(top = 60.dp, start = 60.dp, end = 60.dp).fillMaxWidth(),
            )
        }
        Legend(Modifier.align(Alignment.BottomStart).padding(10.dp))
    }
}

@Composable
private fun ZoomButton(symbol: String, description: String) {
    // Visual only for now; it still meets the 48 dp touch size.
    Box(
        Modifier.size(48.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = KelanaTheme.colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
private fun Callout(region: Region, state: PilihDaerahUiState, onChoose: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.white).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(region.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            StatusBadge(region.status)
        }
        islandLabel(region)?.let { Text(it, color = colors.muted, fontSize = 11.sp) }
        when (region.status) {
            RegionStatus.Available -> {
                placesText(state)?.let { Text(it, color = colors.ink, fontSize = 12.sp, lineHeight = 17.sp) }
                Spacer(Modifier.height(KelanaSpacing.xs))
                KelanaButton(
                    text = stringResource(R.string.pilih_choose_here),
                    onClick = { onChoose(region.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Not choosable: status text only, no button.
            RegionStatus.Upcoming -> Text(stringResource(R.string.status_upcoming_text), color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
            RegionStatus.NotOpen -> Text(stringResource(R.string.status_not_open_text), color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun StatusBadge(status: RegionStatus) {
    val colors = KelanaTheme.colors
    val (bg, fg) = when (status) {
        RegionStatus.Available -> colors.successSurface to colors.successText
        RegionStatus.Upcoming -> colors.skyText to colors.navy
        RegionStatus.NotOpen -> colors.backgroundWarm to colors.muted
    }
    Text(
        statusLabel(status),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
        color = fg,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier.clip(CircleShape).background(colors.white).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(RegionStatus.Available, R.string.pilih_legend_available)
        LegendItem(RegionStatus.Upcoming, R.string.pilih_legend_upcoming)
        LegendItem(RegionStatus.NotOpen, R.string.pilih_legend_not_open)
    }
}

@Composable
private fun LegendItem(status: RegionStatus, label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(statusColor(status), CircleShape))
        Text(stringResource(label), color = KelanaTheme.colors.ink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun AvailableRegionCard(region: Region, state: PilihDaerahUiState, onChoose: (String) -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.primary).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
    ) {
        Text(region.name, color = colors.white, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            stringResource(R.string.region_places_meta, islandLabel(region).orEmpty(), state.placeCount),
            color = colors.skyText,
            fontSize = 12.sp,
        )
        placesText(state)?.let { Text(it, color = colors.skyText, fontSize = 14.sp, lineHeight = 21.sp) }
        Button(
            onClick = { onChoose(region.id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = KelanaShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = colors.white, contentColor = colors.navy),
        ) { Text(stringResource(R.string.pilih_choose_region, region.name), fontWeight = FontWeight.Bold) }
    }
}

/** Visual only: the suggestion flow does not exist yet, so the card does nothing when tapped. */
@Composable
private fun SuggestCard() {
    val colors = KelanaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.white)
            .clickable(enabled = false) {}
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.pilih_suggest_title), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(stringResource(R.string.pilih_suggest_body), color = colors.muted, fontSize = 11.sp)
        }
    }
}

/** PROVISIONAL: Region has no island field; known only for DIY for now. */
@Composable
private fun islandLabel(region: Region): String? =
    if (region.id == DefaultRegionId) stringResource(R.string.island_java) else null

@Composable
private fun placesText(state: PilihDaerahUiState): String? {
    if (state.placeNames.isEmpty()) return null
    val names = state.placeNames.joinToString(", ")
    return if (state.moreCount > 0) stringResource(R.string.region_places_list, names, state.moreCount)
    else stringResource(R.string.region_places_list_all, names)
}

// ---------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------

private val previewRegions = listOf(
    Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1),
    Region("JTG", "Jawa Tengah", RegionStatus.Upcoming, 2),
    Region("PAPUA", "Papua", RegionStatus.NotOpen, 3),
)

private fun previewState(selected: String) = PilihDaerahUiState(
    loading = false,
    regions = previewRegions,
    selectedRegionId = selected,
    placeNames = listOf("Keraton Yogyakarta", "Taman Sari", "Museum Sonobudoyo", "Candi Prambanan"),
    placeCount = 10,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MulaiPreview() = KelanaTheme {
    IntroContent(IntroUiState.forPage(MulaiPage), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Kenalan1Preview() = KelanaTheme {
    IntroContent(IntroUiState.forPage(1), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Kenalan2Preview() = KelanaTheme {
    IntroContent(IntroUiState.forPage(2), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Kenalan3Preview() = KelanaTheme {
    IntroContent(IntroUiState.forPage(3), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun PilihDaerahAvailablePreview() = KelanaTheme {
    PilihDaerahContent(previewState("DIY"), {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun PilihDaerahUpcomingPreview() = KelanaTheme {
    PilihDaerahContent(previewState("JTG"), {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun PilihDaerahLoadingPreview() = KelanaTheme {
    PilihDaerahContent(PilihDaerahUiState(), {}, {}, {})
}

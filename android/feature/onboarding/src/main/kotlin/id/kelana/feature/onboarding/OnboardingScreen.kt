package id.kelana.feature.onboarding

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus
import id.kelana.core.navigation.IntroRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.navigation.PilihDaerahRoute
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

/**
 * Screen 1.1 (MulaiIlustratif.dc.html). The artifact places the logo block at top 280 of 844; here the space above
 * and below the block is split 1.32 : 1 (the same ratio) so other screen heights keep the block centered-ish.
 */
@Composable
private fun MulaiContent(onStart: () -> Unit, onHaveAccount: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.primary)) {
        Image(
            painter = painterResource(R.drawable.onboarding_mulai_swirl),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1.32f))
            Image(
                painter = painterResource(R.drawable.onboarding_mulai_logo),
                contentDescription = stringResource(R.string.mulai_logo_description),
                modifier = Modifier.size(width = 141.dp, height = 112.dp),
            )
            Spacer(Modifier.height(30.dp))
            Image(
                painter = painterResource(R.drawable.onboarding_mulai_wordmark),
                contentDescription = stringResource(R.string.mulai_wordmark),
                modifier = Modifier.size(width = 140.dp, height = 38.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.mulai_tagline),
                color = colors.stampYellow,
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutterWide).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = KelanaShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.stampYellow, contentColor = colors.navy),
                ) { Text(stringResource(R.string.mulai_start), style = MaterialTheme.typography.labelLarge) }
                TextButton(onClick = onHaveAccount, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.onboarding_have_account),
                        color = colors.white,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/** Screens 1.2 to 1.4 (Kenalan1/2/3.dc.html). */
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
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        // PROVISIONAL: the artifact row is 44 tall under 14 of padding; 48 keeps the 48 dp touch target.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutterWide).padding(top = 12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepIndicator(current = intro.step, count = stepCount)
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.intro_skip), color = colors.muted, style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        IntroIllustration(
            intro,
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter).height(400.dp).clip(KelanaShapes.imagePanel),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KelanaSpacing.gutterText),
        ) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(intro.title), color = colors.ink, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(intro.body), color = colors.muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(KelanaSpacing.md))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutterWide).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (intro.isLast) {
                RoundButton(stringResource(R.string.intro3_start_jogja), onStartFromJogja)
                TextButton(onClick = onOtherRegion, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.intro3_other_region), color = colors.primary, style = MaterialTheme.typography.titleSmall)
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
        shape = KelanaShapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.white),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Three-step indicator: the active step is a 22 x 8 pill, the others 8 x 8 dots (`#D5DBE6`). */
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
                    .background(if (i == current) colors.primary else colors.divider),
            )
        }
    }
}

/** One piece of the Kenalan collage: a vector drawable at an offset inside the 358 x 400 panel. */
private data class Piece(@DrawableRes val res: Int, val x: Int, val y: Int, val rotation: Float, val size: Int)

private val Kenalan1Pieces = listOf(
    Piece(R.drawable.onboarding_kenalan1_museum, 110, 120, 0f, 130),
    Piece(R.drawable.onboarding_kenalan1_check, 240, 70, 12f, 70),
    Piece(R.drawable.onboarding_kenalan1_info, 48, 80, -10f, 52),
    Piece(R.drawable.onboarding_kenalan1_ticket, 60, 280, -8f, 58),
)

private val Kenalan2Pieces = listOf(
    Piece(R.drawable.onboarding_kenalan2_dancer, 150, 90, 4f, 150),
    Piece(R.drawable.onboarding_kenalan2_mask, 40, 70, -10f, 84),
    Piece(R.drawable.onboarding_kenalan2_gong, 56, 250, 8f, 72),
    Piece(R.drawable.onboarding_kenalan2_badge, 280, 40, 10f, 46),
)

private val Kenalan3Pieces = listOf(
    Piece(R.drawable.onboarding_kenalan3_route, 40, 40, -8f, 96),
    Piece(R.drawable.onboarding_kenalan3_footprint, 120, 170, -12f, 54),
    Piece(R.drawable.onboarding_kenalan3_footprint, 70, 260, 6f, 46),
)

/**
 * Illustration panel (358 x 400, radius 32): a tinted panel with a blob and sparkles, then the pieces at the
 * artifact's offsets. The drop shadow (`drop-shadow(3px 4px 0 ...)`) is skipped. Offsets are in dp from the panel
 * start, so on screens narrower or wider than 390 the collage keeps its size and shifts with the panel edge.
 */
@Composable
private fun IntroIllustration(intro: IntroPage, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val (panel, background, pieces) = when (intro) {
        IntroPage.OfficialInfo -> Triple(colors.skyText, R.drawable.onboarding_kenalan1_bg, Kenalan1Pieces)
        IntroPage.ShowSchedule -> Triple(colors.peach, R.drawable.onboarding_kenalan2_bg, Kenalan2Pieces)
        IntroPage.Stamp -> Triple(colors.creamYellow, R.drawable.onboarding_kenalan3_bg, Kenalan3Pieces)
    }
    val description = stringResource(
        when (intro) {
            IntroPage.OfficialInfo -> R.string.intro1_illustration
            IntroPage.ShowSchedule -> R.string.intro2_illustration
            IntroPage.Stamp -> R.string.intro3_illustration
        },
    )
    Box(modifier.background(panel).semantics { contentDescription = description }) {
        Image(painterResource(background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        pieces.forEach { p ->
            Image(
                painter = painterResource(p.res),
                contentDescription = null,
                modifier = Modifier.offset(p.x.dp, p.y.dp).size(p.size.dp).rotate(p.rotation),
            )
        }
        if (intro == IntroPage.Stamp) {
            SonobudoyoSticker(Modifier.offset(168.dp, 160.dp).rotate(-7f))
            VisitedStamp(Modifier.offset(250.dp, 250.dp).rotate(10f).alpha(0.88f))
        }
    }
}

/** Sticker of Kenalan 3: sample art with a place name and cap code (decorative, not data). */
@Composable
private fun SonobudoyoSticker(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val description = stringResource(R.string.intro3_sticker_description)
    Box(modifier.size(width = 150.dp, height = 108.dp).semantics { contentDescription = description }) {
        Image(painterResource(R.drawable.onboarding_kenalan3_sticker), null, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.onboarding_kenalan3_sticker_icon), null, Modifier.size(26.dp))
            Text(
                stringResource(R.string.intro3_sticker_name),
                color = colors.backgroundWarm,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                ),
            )
            Text(
                stringResource(R.string.intro3_sticker_code),
                color = colors.backgroundWarm.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
            )
        }
    }
}

/**
 * "Cap DIKUNJUNGI" of Kenalan 3: two rings, text on the top and bottom arcs and "YK" in the middle.
 * Text on a path cannot be a vector drawable, so this one is drawn with the canvas (84 x 84 artifact units).
 */
@Composable
private fun VisitedStamp(modifier: Modifier = Modifier) {
    val color = KelanaTheme.colors.accentOrange
    val visited = stringResource(R.string.stamp_visited)
    val region = stringResource(R.string.stamp_region_code)
    val code = stringResource(R.string.stamp_place_code)
    val description = stringResource(R.string.intro3_stamp_description)
    Canvas(modifier.size(84.dp).semantics { contentDescription = description }) {
        val u = size.width / 84f
        val c = Offset(42f * u, 42f * u)
        drawCircle(color, radius = 40f * u, center = c, style = Stroke(width = 2.4f * u))
        drawCircle(color, radius = 25.2f * u, center = c, style = Stroke(width = 1.4f * u))
        drawIntoNativeCanvas(color, u, visited, region, code)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoNativeCanvas(
    color: Color,
    u: Float,
    top: String,
    bottom: String,
    center: String,
) {
    drawContext.canvas.nativeCanvas.apply {
        fun paint(sizeUnits: Float, spacingPx: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textSize = sizeUnits * u
            letterSpacing = spacingPx / sizeUnits
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }

        fun onArc(text: String, p: android.graphics.Paint, radius: Float, sweep: Float) {
            val oval = android.graphics.RectF(
                (42f - radius) * u, (42f - radius) * u, (42f + radius) * u, (42f + radius) * u,
            )
            val path = android.graphics.Path().apply { addArc(oval, 180f, sweep) }
            val length = android.graphics.PathMeasure(path, false).length
            val offset = (length - p.measureText(text)) / 2f
            drawTextOnPath(text, path, offset, 0f, p)
        }
        onArc(top, paint(8f, 1.5f), radius = 30.24f, sweep = 180f)
        onArc(bottom, paint(6.7f, 1f), radius = 33.6f, sweep = -180f)
        val p = paint(10.1f, 0f).apply { textAlign = android.graphics.Paint.Align.CENTER }
        drawText(center, 42f * u, 45.8f * u, p)
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

/** Screen 1.5 (PilihDaerah.dc.html). */
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
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KelanaSpacing.gutterWide),
        ) {
            Text(stringResource(R.string.pilih_intro), color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            when {
                state.loading -> {
                    Spacer(Modifier.height(KelanaSpacing.md))
                    Text(stringResource(R.string.pilih_loading), color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                }
                state.regions.isEmpty() -> {
                    Spacer(Modifier.height(KelanaSpacing.md))
                    Text(stringResource(R.string.pilih_empty), color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    Spacer(Modifier.height(14.dp))
                    MapPanel(state, onPinTapped, onChoose)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.pilih_map_hint), color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp))
                    if (state.availableRegions.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.pilih_available_heading),
                            color = colors.muted,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.availableRegions.forEach { AvailableRegionCard(it, state, onChoose) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.pilih_footer, state.upcomingCount, state.notOpenCount),
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    )
                    Spacer(Modifier.height(16.dp))
                    // Visual only: no suggestion flow exists yet.
                    SuggestCard()
                    Spacer(Modifier.height(KelanaSpacing.lg))
                }
            }
        }
    }
}

/** Dp-offset helper: shifts the placed child left by [fraction] of its own width (CSS translateX(-fraction)). */
private fun Modifier.shiftX(fraction: Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    layout(p.width, p.height) { p.place(-(p.width * fraction).roundToInt(), 0) }
}

/**
 * The map: the Natural Earth drawing (vector), pins, zoom buttons, "Lihat seluruh Indonesia" (only when zoomed),
 * the legend and the tooltip of the selected region. Positions and the zoom formula follow the artifact script:
 * zoom 1x, 2x, 3.2x, centered on the selected pin; starts at 2x with the first available region selected.
 * Pin positions are PROVISIONAL (keyed by region code, see [pinPositionFor]).
 */
@Composable
private fun MapPanel(state: PilihDaerahUiState, onPinTapped: (String) -> Unit, onChoose: (String) -> Unit) {
    val colors = KelanaTheme.colors
    val density = LocalDensity.current.density
    val mapDescription = stringResource(R.string.pilih_map_description)
    val shape = RoundedCornerShape(20.dp)
    var zoomIndex by rememberSaveable { mutableIntStateOf(1) }

    val selected = state.selectedRegion
    val selectedPos = selected?.let { r -> pinPositionFor(r.id, state.regions.indexOf(r)) }
    val targetScale = MapZoomScales[zoomIndex]
    val targetX = if (selectedPos == null || zoomIndex == 0) 0f else 175f - targetScale * selectedPos.x
    val targetY = if (selectedPos == null || zoomIndex == 0) 0f else (96f - MapTopUnits) - targetScale * selectedPos.y
    val spec = tween<Float>(320)
    val s by animateFloatAsState(targetScale, spec, label = "mapScale")
    val cx by animateFloatAsState(targetX, spec, label = "mapX")
    val cy by animateFloatAsState(targetY, spec, label = "mapY")

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(MapPanelHeightUnits.dp) // 360 in PilihDaerah.dc.html
            .clip(shape)
            .background(colors.skyText)
            .border(1.dp, colors.outline, shape)
            .semantics { contentDescription = mapDescription },
    ) {
        val u = maxWidth.value / MapUnitsWidth // dp per artifact unit (1 at 390 dp)

        // Map drawing: scaled and moved like `transform: translate(cx, cy) scale(s)` with origin 0 0.
        Image(
            painter = painterResource(R.drawable.onboarding_pilih_map),
            contentDescription = null,
            modifier = Modifier
                .offset(y = (MapTopUnits * u).dp)
                .size(width = (MapUnitsWidth * u).dp, height = (MapUnitsHeight * u).dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = s
                    scaleY = s
                    translationX = cx * u * density
                    translationY = cy * u * density
                },
            contentScale = ContentScale.FillBounds,
        )

        // Pin tip positions on screen, in artifact units.
        val placed = state.regions.mapIndexed { index, region ->
            val pos = pinPositionFor(region.id, index)
            PlacedPin(region, pos, cx + s * pos.x, MapTopUnits + cy + s * pos.y)
        }

        // Labels: the selected region, and every region at the highest zoom.
        placed.forEach { p ->
            val isSelected = p.region.id == state.selectedRegionId
            if (isSelected || s > 2f) {
                val lx = if (p.pos.below) p.sx else if (p.pos.left) p.sx - 12f else p.sx + 12f
                val ly = if (p.pos.below) p.sy + 8f else p.sy - 22f
                val shift = if (p.pos.below) 0.5f else if (p.pos.left) 1f else 0f
                Text(
                    text = p.region.name,
                    modifier = Modifier.offset((lx * u).dp, (ly * u).dp).shiftX(shift),
                    color = colors.ink,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        shadow = Shadow(colors.white, blurRadius = 6f),
                    ),
                )
            }
        }

        // Pins: the selected one is drawn last, on top.
        (placed.filter { it.region.id != state.selectedRegionId } + placed.filter { it.region.id == state.selectedRegionId })
            .forEach { p ->
                RegionPinMarker(
                    region = p.region,
                    selected = p.region.id == state.selectedRegionId,
                    onClick = {
                        onPinTapped(p.region.id)
                        zoomIndex = max(zoomIndex, 1)
                    },
                    modifier = Modifier.offset((p.sx * u).dp - 24.dp, ((p.sy - 14f) * u).dp - 24.dp),
                )
            }

        // Tooltip of the selected region, below the pin in the top half of the panel and above it otherwise.
        placed.firstOrNull { it.region.id == state.selectedRegionId }?.let { p ->
            RegionTooltip(region = p.region, state = state, sx = p.sx, sy = p.sy, u = u, onChoose = onChoose)
        }

        // Zoom buttons (artifact 40 x 40; 48 here for the touch target).
        val zoomShape = RoundedCornerShape(10.dp)
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .shadow(2.dp, zoomShape)
                .clip(zoomShape)
                .background(colors.white),
        ) {
            ZoomButton("+", stringResource(R.string.pilih_zoom_in)) { zoomIndex = min(zoomIndex + 1, 2) }
            Box(Modifier.width(48.dp).height(1.dp).background(colors.outline))
            ZoomButton("−", stringResource(R.string.pilih_zoom_out)) { zoomIndex = max(zoomIndex - 1, 0) }
        }

        if (zoomIndex > 0) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 2.dp)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { zoomIndex = 0 },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.pilih_all_indonesia),
                    modifier = Modifier
                        .height(32.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(colors.white)
                        .padding(horizontal = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    color = colors.navy,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Legend(Modifier.align(Alignment.BottomStart).padding(10.dp))
    }
}

private data class PlacedPin(val region: Region, val pos: PinPosition, val sx: Float, val sy: Float)

@Composable
private fun ZoomButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = KelanaTheme.colors.ink,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * Tooltip (228 wide, radius 14, white, shadow) with a caret pointing at the pin: name and badge, island, a short
 * description and, for an available region, "Berkelana di sini". The "Kabari saya saat ... dibuka" button of
 * closed regions is not built (needs login and a notify-me flow, see "Needs from planner").
 */
@Composable
private fun RegionTooltip(
    region: Region,
    state: PilihDaerahUiState,
    sx: Float,
    sy: Float,
    u: Float,
    onChoose: (String) -> Unit,
) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val below = sy < MapPanelHeightUnits * 0.55f
    val tx = max(8f, min(sx - 114f, MapUnitsWidth - 236f))
    val caretX = max(14f, min(sx - tx - 7f, 200f))
    val density = LocalDensity.current.density
    Box(
        Modifier
            .width(228.dp)
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) {
                    val x = (tx * u * density).roundToInt()
                    val y = if (below) {
                        ((sy + 14f) * u * density).roundToInt()
                    } else {
                        ((sy - 34f) * u * density).roundToInt() - p.height
                    }
                    p.place(x, y)
                }
            }
            .shadow(6.dp, shape)
            .background(colors.white, shape),
    ) {
        Box(
            Modifier
                .align(if (below) Alignment.TopStart else Alignment.BottomStart)
                .offset(x = caretX.dp, y = if (below) (-7).dp else 7.dp)
                .size(14.dp)
                .rotate(45f)
                .background(colors.white),
        )
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(
                    region.name,
                    modifier = Modifier.weight(1f),
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold),
                )
                StatusBadge(region.status)
            }
            islandLabel(region)?.let {
                Text(it, color = colors.muted, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal))
            }
            Spacer(Modifier.height(6.dp))
            val body = when (region.status) {
                RegionStatus.Available -> placesText(state)
                RegionStatus.Upcoming -> stringResource(R.string.status_upcoming_text)
                RegionStatus.NotOpen -> stringResource(R.string.status_not_open_text)
            }
            body?.let {
                Text(it, color = colors.ink, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp))
            }
            if (region.status == RegionStatus.Available) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onChoose(region.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.white),
                ) { Text(stringResource(R.string.pilih_choose_here), style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp)) }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RegionStatus) {
    val colors = KelanaTheme.colors
    val (bg, fg) = when (status) {
        RegionStatus.Available -> colors.successSurface to colors.successText
        RegionStatus.Upcoming -> colors.creamYellow to colors.warnText
        RegionStatus.NotOpen -> colors.badgeClosedBackground to colors.muted
    }
    Text(
        statusLabel(status),
        modifier = Modifier.clip(KelanaShapes.badge).background(bg).padding(horizontal = 7.dp, vertical = 2.dp),
        color = fg,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).background(colors.white.copy(alpha = 0.92f)).padding(horizontal = 8.dp, vertical = 4.dp),
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
        Text(stringResource(label), color = KelanaTheme.colors.ink, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 14.sp))
    }
}

/** "Sudah bisa dijelajahi" card: primary background, radius 16, padding 16, gap 10. */
@Composable
private fun AvailableRegionCard(region: Region, state: PilihDaerahUiState, onChoose: (String) -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(KelanaShapes.card).background(colors.primary).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.onboarding_pilih_region_mark), null, Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(region.name, color = colors.white, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp))
                val island = islandLabel(region)
                if (island != null) {
                    Text(
                        stringResource(R.string.region_places_meta, island, state.placeCount),
                        color = colors.skyText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    )
                }
            }
        }
        placesText(state)?.let { Text(it, color = colors.skyText, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)) }
        Button(
            onClick = { onChoose(region.id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = KelanaShapes.input,
            colors = ButtonDefaults.buttonColors(containerColor = colors.white, contentColor = colors.navy),
        ) { Text(stringResource(R.string.pilih_choose_region, region.name), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) }
    }
}

/** Dashed "Usulkan daerahmu" card. Visual only: the suggestion flow does not exist yet, so a tap does nothing. */
@Composable
private fun SuggestCard() {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = colors.dashWarm,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.onboarding_pilih_suggest_mark), null, Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.pilih_suggest_title), color = colors.ink, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(stringResource(R.string.pilih_suggest_body), color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal))
        }
        Image(painterResource(R.drawable.onboarding_chevron_right), null, Modifier.size(18.dp))
    }
}

@Composable
private fun placesText(state: PilihDaerahUiState): String? {
    if (state.placeNames.isEmpty()) return null
    val names = state.placeNames.joinToString(", ")
    return if (state.moreCount > 0) {
        stringResource(R.string.region_places_list, names, state.moreCount)
    } else {
        stringResource(R.string.region_places_list_all, names)
    }
}

// ---------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------

private val previewRegions = listOf(
    Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1),
    Region("JTG", "Jawa Tengah", RegionStatus.Upcoming, 2),
    Region("BALI", "Bali", RegionStatus.Upcoming, 3),
    Region("JTM", "Jawa Timur", RegionStatus.NotOpen, 4),
    Region("PAPUA", "Papua", RegionStatus.NotOpen, 5),
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
    PilihDaerahContent(previewState("BALI"), {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun PilihDaerahNotOpenPreview() = KelanaTheme {
    PilihDaerahContent(previewState("PAPUA"), {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun PilihDaerahLoadingPreview() = KelanaTheme {
    PilihDaerahContent(PilihDaerahUiState(), {}, {}, {})
}

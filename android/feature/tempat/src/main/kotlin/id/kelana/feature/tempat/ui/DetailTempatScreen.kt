package id.kelana.feature.tempat.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import id.kelana.core.ui.openInGoogleMaps
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Closure
import id.kelana.core.model.Event
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.TicketPrice
import id.kelana.core.navigation.AmbilCapRoute
import id.kelana.core.navigation.CeritaRoute
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.core.ui.formatTimeRange
import id.kelana.core.ui.label
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.tempat.R
import id.kelana.feature.tempat.domain.HoursSummary
import id.kelana.feature.tempat.domain.closedDaysFact
import id.kelana.feature.tempat.domain.hoursSummary
import id.kelana.feature.tempat.domain.officialSellerUrl
import id.kelana.feature.tempat.domain.todayHours
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// Sizes read from DetailIlustratif.dc.html that have no token in :core:designsystem.
private val HeaderHeight = 250.dp
private val HeaderOverlap = 18.dp
private val TitleCardRadius = 24.dp
private val BannerHeight = 112.dp
internal val RowTile = 40.dp

@Composable
internal fun DetailTempatScreen(
    route: DetailTempatRoute,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailTempatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)

    when (state.status) {
        PlaceLoadStatus.Loading -> LoadingBody()
        PlaceLoadStatus.NotFound -> MessageScreen(
            title = stringResource(R.string.tempat_not_found_title),
            body = stringResource(R.string.tempat_not_found_body),
            onBack = onBack,
        )
        PlaceLoadStatus.Error -> MessageScreen(
            title = stringResource(R.string.tempat_error_title),
            body = stringResource(R.string.tempat_error_body),
            onBack = onBack,
            actionLabel = stringResource(R.string.tempat_retry),
            onAction = viewModel::retry,
        )
        PlaceLoadStatus.Loaded -> {
            val place = state.place ?: return
            val inPlan by viewModel.inPlan.collectAsState()
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val addedMessage = stringResource(R.string.tempat_plan_added)
            val removedMessage = stringResource(R.string.tempat_plan_removed)
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = KelanaTheme.colors.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    BottomActions(
                        // Postcard needs an account (product rule); "Masuk rencana" is open to guests.
                        onPostcard = { requireLogin { navigate(PostcardRoute(place.id)) } },
                        inPlan = inPlan,
                        onAddPlan = {
                            val message = if (inPlan) removedMessage else addedMessage
                            viewModel.togglePlan()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(message)
                            }
                        },
                    )
                },
            ) { padding ->
                DetailTempatContent(
                    place = place,
                    today = LocalDate.now().dayOfWeek,
                    onBack = onBack,
                    onPhotos = { navigate(FotoResmiRoute(place.id)) },
                    // "Ambil cap" needs an account: a guest is sent to Masuk first, then Ambil cap for this place.
                    onStamp = { requireLogin { navigate(AmbilCapRoute(place.id)) } },
                    onTicketSite = { url -> navigate(OfficialSiteRoute(url, place.name)) },
                    onReport = { navigate(LaporkanRoute(place.id)) },
                    onAllStories = { requireLogin { navigate(CeritaRoute) } },
                    events = state.events,
                    photoCount = state.photoCount,
                    coverUrl = state.coverUrl,
                    onOpenEvent = { navigate(DetailPentasRoute(it)) },
                    videosSlot = { PlaceVideosSection() },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
internal fun DetailTempatContent(
    place: Place,
    today: DayOfWeek,
    onBack: () -> Unit,
    onPhotos: () -> Unit,
    onStamp: () -> Unit,
    onTicketSite: (String) -> Unit,
    onReport: () -> Unit,
    onAllStories: () -> Unit,
    modifier: Modifier = Modifier,
    events: List<Event> = emptyList(),
    onOpenEvent: (String) -> Unit = {},
    videosSlot: @Composable () -> Unit = {},
    photoCount: Int = 0,
    coverUrl: String? = null,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PlaceHeader(place = place, photoCount = photoCount, coverUrl = coverUrl, onBack = onBack, onPhotos = onPhotos)
        TitleCard(place)
        CapBanner(capCode = place.capCode, placeName = place.name, onStamp = onStamp)
        InfoResmiCard(place = place, today = today, onTicketSite = onTicketSite)
        PlaceEventsSection(events = events, onOpenEvent = onOpenEvent)
        StoriesSection(onAllStories = onAllStories)
        videosSlot()
        ReportRow(onReport = onReport)
        Spacer(Modifier.height(KelanaSpacing.lg))
    }
}

/** Illustrated header (250 dp) with the round back and save buttons and the "Lihat N foto" pill (only when there are photos). */
@Composable
private fun PlaceHeader(place: Place, photoCount: Int, coverUrl: String?, onBack: () -> Unit, onPhotos: () -> Unit) {
    val colors = KelanaTheme.colors
    // PROVISIONAL: visual-only toggle. Saving places is not wired to any storage yet.
    var saved by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(HeaderHeight).background(colors.skyText)) {
        // Cover = the place's first gallery photo; the shared illustration stays as placeholder, error and no-photo
        // fallback. Attribution is deliberately NOT drawn here (clean header): CC licences require it where the image
        // is used, so the cover opens the gallery (same destination as the chip), which shows it with every photo.
        val fallback = painterResource(R.drawable.tempat_cover_art)
        AsyncImage(
            model = coverUrl,
            contentDescription = stringResource(R.string.tempat_header_cover, place.name),
            modifier = Modifier.fillMaxSize().then(
                if (coverUrl != null && photoCount > 0) Modifier.clickable(role = Role.Button, onClick = onPhotos) else Modifier,
            ),
            contentScale = ContentScale.Crop,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
        )
        if (coverUrl != null) {
            // Scrims keep the buttons (top) and the photo chip (bottom) legible on any photo.
            Box(
                Modifier.fillMaxWidth().height(96.dp).align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(colors.inkDeep.copy(alpha = 0.55f), Color.Transparent))),
            )
            Box(
                Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colors.inkDeep.copy(alpha = 0.55f)))),
            )
        }
        RoundIconButton(
            iconRes = R.drawable.tempat_ic_chevron_back,
            description = stringResource(R.string.tempat_back),
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp),
        )
        Row(Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 12.dp)) {
            val context = LocalContext.current
            RoundIconButton(
                iconRes = R.drawable.tempat_ic_map_pin,
                description = stringResource(R.string.tempat_open_map),
                onClick = { openInGoogleMaps(context, place.lat, place.lng, place.name) },
                iconSize = 22.dp,
            )
            RoundIconButton(
                iconRes = if (saved) R.drawable.ic_bookmark else R.drawable.tempat_ic_bookmark_art,
                description = stringResource(if (saved) R.string.tempat_unsave else R.string.tempat_save),
                onClick = { saved = !saved },
                iconSize = 22.dp,
            )
        }
        // 48 dp touch target around the 32 dp pill.
        if (photoCount > 0) Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = KelanaSpacing.gutter, bottom = 6.dp)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onPhotos),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                Modifier
                    .height(32.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.navy.copy(alpha = 0.88f))
                    .padding(horizontal = KelanaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.tempat_ic_media), contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.tempat_photos_chip, photoCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.white,
                )
            }
        }
    }
}

/** White 44 dp round button on the header, inside a 48 dp touch target. */
@Composable
private fun RoundIconButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(
        modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(KelanaSizes.backButton).clip(CircleShape).background(KelanaTheme.colors.white),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = description,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Category and cap badges, name, address and description on a card that overlaps the header by 18 dp. */
@Composable
private fun TitleCard(place: Place) {
    val colors = KelanaTheme.colors
    val overlap = HeaderOverlap
    Column(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val pull = overlap.roundToPx()
                layout(placeable.width, placeable.height - pull) { placeable.place(0, -pull) }
            }
            .padding(horizontal = KelanaSpacing.gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(TitleCardRadius))
            .background(colors.background)
            .padding(KelanaSpacing.gutter),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(text = place.category.label(), background = colors.skyText, content = colors.navy)
            Badge(
                text = stringResource(R.string.tempat_cap_badge, place.name),
                modifier = Modifier.weight(1f, fill = false),
                background = colors.backgroundWarm,
                content = colors.stampBrown,
            )
        }
        Text(
            text = place.name,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
        )
        Text(
            text = place.address,
            modifier = Modifier.padding(top = KelanaSpacing.xs),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Text(
            text = place.description,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
            color = colors.ink,
            textAlign = TextAlign.Justify,
        )
    }
}

@Composable
private fun Badge(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier
            .clip(KelanaShapes.badge)
            .background(background)
            .padding(horizontal = KelanaSpacing.sm, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = content,
    )
}

/**
 * Orange "Cap YK-02 menunggumu" banner (358 x 112, radius 20) with the sticker. The whole banner is the
 * "Ambil cap" button. The sticker drop shadow is skipped.
 */
@Composable
private fun CapBanner(capCode: String, placeName: String, onStamp: () -> Unit) {
    val colors = KelanaTheme.colors
    val stampButtonLabel = stringResource(R.string.tempat_cap_button)
    Box(
        Modifier
            .padding(top = KelanaSpacing.sm, start = KelanaSpacing.gutter, end = KelanaSpacing.gutter)
            .fillMaxWidth()
            .height(BannerHeight)
            .clip(KelanaShapes.listCard)
            .background(colors.stampBrown)
            .clickable(onClickLabel = stampButtonLabel, role = Role.Button, onClick = onStamp),
    ) {
        Image(
            painter = painterResource(R.drawable.tempat_cap_banner_art),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Row(Modifier.fillMaxSize().padding(KelanaSpacing.gutter)) {
            Column(Modifier.width(192.dp)) {
                Text(
                    text = stringResource(R.string.tempat_cap_label, placeName).uppercase(),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = colors.stampYellow,
                )
                Text(
                    text = stringResource(R.string.tempat_cap_headline),
                    modifier = Modifier.padding(top = KelanaSpacing.xs),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
                    color = colors.white,
                )
            }
            Spacer(Modifier.width(KelanaSpacing.md))
            Sticker(capCode = capCode, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

/** Perforated cap sticker with the region code and place number, tilted -8 degrees. */
@Composable
private fun Sticker(capCode: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val region = capCode.substringBefore('-')
    val number = capCode.substringAfter('-', "")
    val stickerDescription = stringResource(R.string.tempat_sticker_desc, region)
    Box(
        modifier
            .size(width = 68.dp, height = 52.dp)
            .rotate(-8f)
            .semantics { contentDescription = stickerDescription },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.tempat_sticker_shape),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            Modifier.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Image(painterResource(R.drawable.tempat_ic_kawung), contentDescription = null, Modifier.size(14.dp))
            Text(
                text = region,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                ),
                color = colors.stampBrown,
            )
            if (number.isNotEmpty()) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    color = colors.stampBrown.copy(alpha = 0.9f),
                )
            }
        }
    }
}

/**
 * "Info resmi" card: hours, tickets, closed days. Every value is shown only when its fact has a check date;
 * otherwise the row says "sedang dicek". Check dates and sources stay in the data and are not displayed.
 */
@Composable
private fun InfoResmiCard(place: Place, today: DayOfWeek, onTicketSite: (String) -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .padding(top = 14.dp, start = KelanaSpacing.gutter, end = KelanaSpacing.gutter)
            .fillMaxWidth()
            .clip(KelanaShapes.listCard)
            .background(colors.surface)
            .border(1.dp, colors.outline, KelanaShapes.listCard)
            .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = KelanaSpacing.md, bottom = KelanaSpacing.xs),
    ) {
        InfoHeader()

        // Jam buka: weekly summary when the hours form one run, otherwise today's hours.
        val summary = hoursSummary(place.hours)
        if (summary != null) {
            FactRow(R.drawable.tempat_ic_clock, colors.skyText, stringResource(R.string.tempat_hours), summary) {
                formatHoursSummary(it)
            }
        } else {
            val todayLabel = stringResource(R.string.tempat_hours_today, dayName(today))
            val todayFact = todayHours(place.hours, today)
            if (todayFact != null) {
                FactRow(R.drawable.tempat_ic_clock, colors.skyText, todayLabel, todayFact) { formatHours(it) }
            } else {
                PendingRow(R.drawable.tempat_ic_clock, colors.skyText, todayLabel, stringResource(R.string.tempat_info_checking))
            }
        }

        // Tiket masuk: one row, one entry per ticket kind.
        TicketsRow(place.tickets)

        // Hari tutup
        val closedLabel = stringResource(R.string.tempat_closed_days)
        val closedFact = closedDaysFact(place.hours)
        if (closedFact != null) {
            FactRow(R.drawable.tempat_ic_closed, colors.peach, closedLabel, closedFact) { days ->
                if (days.isEmpty()) {
                    stringResource(R.string.tempat_open_every_day)
                } else {
                    days.map { dayName(it) }.joinToString(", ")
                }
            }
        } else {
            PendingRow(R.drawable.tempat_ic_closed, colors.peach, closedLabel, stringResource(R.string.tempat_info_checking))
        }
        place.closures.forEach { closure ->
            FactRow(R.drawable.tempat_ic_closed, colors.peach, stringResource(R.string.tempat_closure), closure) {
                formatClosure(it)
            }
        }

        officialSellerUrl(place)?.let { url ->
            KelanaButton(
                text = stringResource(R.string.tempat_ticket_site),
                onClick = { onTicketSite(url) },
                variant = KelanaButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth().padding(vertical = KelanaSpacing.md),
            )
        }
    }
}

@Composable
internal fun RowDivider() = HorizontalDivider(color = KelanaTheme.colors.outline)

/** One info row: icon tile, label, value. The value is rendered only for a checked fact. */
@Composable
private fun <T> FactRow(
    @DrawableRes icon: Int,
    tile: Color,
    label: String,
    fact: OfficialFact<T>,
    format: @Composable (T) -> String,
) {
    when (val display = fact.display()) {
        is FactDisplay.Checked -> ValueRow(icon, tile, label, format(display.value), pending = false)
        FactDisplay.Checking -> PendingRow(icon, tile, label, stringResource(R.string.tempat_info_checking))
    }
}

@Composable
internal fun PendingRow(@DrawableRes icon: Int, tile: Color, label: String, text: String) =
    ValueRow(icon, tile, label, text, pending = true)

@Composable
internal fun ValueRow(@DrawableRes icon: Int, tile: Color, label: String, value: String, pending: Boolean) {
    val colors = KelanaTheme.colors
    RowDivider()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(RowTile).clip(KelanaShapes.input).background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), contentDescription = null, Modifier.size(26.dp))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        Text(
            text = value,
            modifier = Modifier.widthIn(max = 190.dp),
            style = MaterialTheme.typography.titleSmall,
            color = if (pending) colors.warnText else colors.ink,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Visitor stories (Cerita feature). No visitor content exists yet, so the section shows its heading, the rule
 * and an empty state, never invented cards.
 */
@Composable
private fun StoriesSection(onAllStories: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(Modifier.padding(top = 26.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tempat_stories_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.tempat_stories_all),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onAllStories)
                    .padding(vertical = 14.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.primary,
            )
        }
        Row(
            Modifier.padding(horizontal = KelanaSpacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(painterResource(R.drawable.tempat_ic_check), contentDescription = null, Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.tempat_stories_note),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                color = colors.muted,
            )
        }
        Column(
            Modifier
                .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = 14.dp)
                .fillMaxWidth()
                .clip(KelanaShapes.card)
                .background(colors.backgroundWarm)
                .padding(KelanaSpacing.gutter),
        ) {
            Text(
                text = stringResource(R.string.tempat_stories_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.tempat_stories_empty_body),
                modifier = Modifier.padding(top = KelanaSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
}

/** "Ada info yang salah? Laporkan" row with a top hairline. */
@Composable
private fun ReportRow(onReport: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(Modifier.padding(top = KelanaSpacing.gutter, start = KelanaSpacing.gutter, end = KelanaSpacing.gutter)) {
        RowDivider()
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(R.drawable.tempat_ic_report), contentDescription = null, Modifier.size(22.dp))
            Text(
                text = stringResource(R.string.tempat_report_prompt),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Text(
                text = stringResource(R.string.tempat_report_action),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onReport)
                    .padding(vertical = 14.dp),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
            )
        }
    }
}

/** Fixed bottom bar: outlined "Postcard" and the orange "Masuk rencana" (52 dp pills). */
@Composable
private fun BottomActions(onPostcard: () -> Unit, inPlan: Boolean, onAddPlan: () -> Unit) {
    val colors = KelanaTheme.colors
    val label = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
    Column(Modifier.background(colors.surface)) {
        RowDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = KelanaSpacing.gutter, end = KelanaSpacing.gutter, top = KelanaSpacing.md, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier
                    .height(KelanaSizes.buttonHeight)
                    .clip(KelanaShapes.button)
                    .border(1.5.dp, colors.primary, KelanaShapes.button)
                    .clickable(role = Role.Button, onClick = onPostcard)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(painterResource(R.drawable.tempat_ic_media), contentDescription = null, Modifier.size(22.dp))
                Text(text = stringResource(R.string.tempat_postcard), style = label, color = colors.primary)
            }
            Row(
                Modifier
                    .weight(1f)
                    .height(KelanaSizes.buttonHeight)
                    .clip(KelanaShapes.button)
                    .background(if (inPlan) colors.successSurface else colors.accentOrange)
                    .then(if (inPlan) Modifier.border(1.5.dp, colors.success, KelanaShapes.button) else Modifier)
                    .toggleable(value = inPlan, role = Role.Switch, onValueChange = { onAddPlan() }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    painterResource(if (inPlan) R.drawable.tempat_ic_check_line else R.drawable.tempat_ic_plus),
                    contentDescription = null,
                    Modifier.size(20.dp),
                    colorFilter = if (inPlan) ColorFilter.tint(colors.successTextDeep) else null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(if (inPlan) R.string.tempat_in_plan else R.string.tempat_add_plan),
                    style = label,
                    color = if (inPlan) colors.successTextDeep else colors.white,
                )
            }
        }
    }
}

@Composable
private fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
private fun formatHours(hours: OpeningHours): String {
    val opens = hours.opens
    return if (hours.isClosed || opens == null) {
        stringResource(R.string.tempat_closed)
    } else {
        formatTimeRange(opens, hours.closes)
    }
}

@Composable
private fun formatHoursSummary(summary: HoursSummary): String {
    val days = if (summary.firstDay == summary.lastDay) {
        dayName(summary.firstDay)
    } else {
        stringResource(R.string.tempat_days_range, dayName(summary.firstDay), dayName(summary.lastDay))
    }
    return stringResource(R.string.tempat_hours_range, days, formatTimeRange(summary.opens, summary.closes))
}

@Composable
private fun formatClosure(closure: Closure): String {
    val range = if (closure.startsOn == closure.endsOn) {
        formatCheckedDate(closure.startsOn)
    } else {
        stringResource(
            R.string.tempat_closure_range,
            formatCheckedDate(closure.startsOn),
            formatCheckedDate(closure.endsOn),
        )
    }
    val reason = closure.reason ?: return range
    return stringResource(R.string.tempat_closure_with_reason, range, reason)
}

// --- Previews (sample data is local to the previews; screens never hardcode data) ---

private val PreviewDate: LocalDate = LocalDate.of(2026, 11, 20)

private fun previewPlace(checked: Boolean) = Place(
    id = "museum-sonobudoyo",
    slug = "museum-sonobudoyo",
    name = "Museum Sonobudoyo",
    category = PlaceCategory.Museum,
    capCode = "YK-02",
    lat = 0.0,
    lng = 0.0,
    address = "Jl. Pangurakan No. 6, Gondomanan, Kota Yogyakarta",
    description = "Museum budaya Jawa: wayang, keris, topeng, dan batik.",
    coverKey = "museum-sonobudoyo",
    hours = DayOfWeek.entries.map {
        OfficialFact(
            OpeningHours(
                it,
                it == DayOfWeek.MONDAY,
                if (it == DayOfWeek.MONDAY) null else LocalTime.of(8, 0),
                if (it == DayOfWeek.MONDAY) null else LocalTime.of(21, 0),
            ),
            "https://example.org/sample",
            if (checked) PreviewDate else null,
        )
    },
    tickets = listOf(
        OfficialFact(TicketPrice("Dewasa", 10_000, "https://example.org/tiket"), "https://example.org/sample", if (checked) PreviewDate else null),
        OfficialFact(TicketPrice("Anak-anak", 5_000, null), "https://example.org/sample", if (checked) PreviewDate else null),
    ),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 1500)
@Composable
private fun DetailTempatCheckedPreview() {
    KelanaTheme {
        DetailTempatContent(previewPlace(true), DayOfWeek.TUESDAY, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1500)
@Composable
private fun DetailTempatCheckingPreview() {
    KelanaTheme {
        DetailTempatContent(previewPlace(false), DayOfWeek.MONDAY, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun DetailTempatNotFoundPreview() {
    KelanaTheme {
        MessageScreen(
            title = stringResource(R.string.tempat_not_found_title),
            body = stringResource(R.string.tempat_not_found_body),
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun DetailTempatErrorPreview() {
    KelanaTheme {
        MessageScreen(
            title = stringResource(R.string.tempat_error_title),
            body = stringResource(R.string.tempat_error_body),
            onBack = {},
            actionLabel = stringResource(R.string.tempat_retry),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun DetailTempatLoadingPreview() {
    KelanaTheme { LoadingBody() }
}

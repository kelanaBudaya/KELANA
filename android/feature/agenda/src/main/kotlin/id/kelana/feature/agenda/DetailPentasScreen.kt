package id.kelana.feature.agenda

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Recurrence
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.ui.formatClock
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.label
import java.time.Duration

@Composable
internal fun DetailPentasScreen(
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailPentasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    DetailPentasContent(
        state = state,
        onBack = onBack,
        // The sheet "Ke situs tiket resmi" is the OfficialSiteRoute dialog owned by :app; it opens the Custom Tab.
        onBuyTicket = { url, label -> navigate(OfficialSiteRoute(url = url, label = label)) },
    )
}

// Layout from docs/design/artifact/DetailPentasIlustratif.dc.html (screen 3.2), compared as code, not as a render.
@Composable
internal fun DetailPentasContent(
    state: DetailPentasUiState,
    onBack: () -> Unit,
    onBuyTicket: (url: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    when (state) {
        is DetailPentasUiState.Found -> FoundContent(state, onBack, onBuyTicket, modifier)
        else -> Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
            // Loading and not-found are not drawn in the artifact: they keep the plain top bar.
            KelanaBackTopBar(title = stringResource(R.string.detail_title), onBack = onBack)
            if (state == DetailPentasUiState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                EmptyState(
                    title = stringResource(R.string.detail_not_found_title),
                    body = stringResource(R.string.detail_not_found_body),
                    modifier = Modifier.padding(top = KelanaSpacing.lg),
                )
            }
        }
    }
}

/** Fraunces 30/34 w600 in the artifact; the bundled static Fraunces has Regular and Bold, Bold is the closest. */
private val DetailTitle = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
)

// 24 dp content card radius is KelanaRadii.sheet.
private val SheetShape = RoundedCornerShape(KelanaRadii.sheet)

@Composable
private fun FoundContent(
    state: DetailPentasUiState.Found,
    onBack: () -> Unit,
    onBuyTicket: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val event = state.event
    Column(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Hero(onBack)
            InfoSheet(state, Modifier.padding(horizontal = KelanaSpacing.gutter).pullUp(20.dp))
            if (event.description.isNotBlank()) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                    Text(
                        stringResource(R.string.detail_story_heading),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                    )
                    Text(
                        event.description,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = colors.ink,
                    )
                }
            }
            MannersSection(event)
            AfterShowNote(event.placeName)
            Box(Modifier.height(KelanaSpacing.lg))
        }
        state.ticketUrl?.let { url -> BuyBar(onClick = { onBuyTicket(url, event.placeName) }) }
    }
}

/** 270 dp orange header with illustration marks and a floating white back button (the save button is not built). */
@Composable
private fun Hero(onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxWidth().height(270.dp).background(colors.accentOrange)) {
        Image(
            painter = painterResource(R.drawable.agenda_detail_hero_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        // Offsets from the artifact (390 dp frame): tari 140 at (right 50, top 40), stamp 70 at (left 60, top 120),
        // wayang 54 at (left 120, top 40). Drop shadows are skipped.
        Image(
            painter = painterResource(R.drawable.agenda_art_tari),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 50.dp, top = 40.dp).size(140.dp).rotate(-4f),
        )
        Image(
            painter = painterResource(R.drawable.agenda_art_stamp),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 120.dp).size(70.dp).rotate(6f),
        )
        Image(
            painter = painterResource(R.drawable.agenda_art_wayang),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 120.dp, top = 40.dp).size(54.dp).rotate(-10f),
        )
        val backDescription = stringResource(R.string.detail_back_cd)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 14.dp)
                .size(KelanaSizes.backButton)
                .clip(CircleShape)
                .background(colors.surface)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backDescription },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.agenda_ic_back), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InfoSheet(state: DetailPentasUiState.Found, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val event = state.event
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background, SheetShape)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = event.category.label(),
                modifier = Modifier
                    .background(colors.creamYellow, KelanaShapes.badge)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.warnText,
            )
        }
        Text(
            event.title,
            modifier = Modifier.padding(top = 10.dp),
            style = DetailTitle,
            color = colors.ink,
        )
        PlaceAndDuration(event)
        Box(Modifier.padding(top = 4.dp))
        FactRow(R.drawable.agenda_ic_calendar, stringResource(R.string.detail_schedule_label), event.schedule) {
            scheduleText(it)
        }
        FactRow(R.drawable.agenda_ic_ticket, stringResource(R.string.detail_price_label), event.ticket) {
            priceText(it)
        }
        if (event.performers.isNotEmpty()) {
            PlainRow(R.drawable.agenda_art_tari, stringResource(R.string.detail_performers_label), event.performers.joinToString(", "))
        }
    }
}

/** Hairline on the top edge (artifact: `border-top: 1px solid #E7E0D2`). */
@Composable
private fun Modifier.divider(): Modifier = drawTopLine()

@Composable
private fun Modifier.drawTopLine(): Modifier {
    val color = KelanaTheme.colors.outline
    return drawBehind { drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
}

/**
 * Label/value row. The value is built only when the fact has a check date; otherwise "Sedang dicek" is shown.
 * The check date itself is not rendered (product decision: no "Diperiksa" label, no source line).
 */
@Composable
private fun <T> FactRow(
    @DrawableRes icon: Int,
    label: String,
    fact: OfficialFact<T>,
    format: @Composable (T) -> String,
) {
    val colors = KelanaTheme.colors
    val display = fact.display()
    Row(
        modifier = Modifier.fillMaxWidth().divider().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.muted)
        Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.End) {
            when (display) {
                is FactDisplay.Checked -> Text(
                    format(display.value),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                    textAlign = TextAlign.End,
                )
                FactDisplay.Checking -> Text(
                    stringResource(R.string.detail_checking),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.muted,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun PlainRow(@DrawableRes icon: Int, label: String, value: String) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().divider().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.muted)
        Text(
            value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            textAlign = TextAlign.End,
        )
    }
}

/** "Tata krama menonton": horizontally scrolling 150 dp cards. Only cards backed by data are drawn. */
@Composable
private fun MannersSection(event: Event) {
    val colors = KelanaTheme.colors
    Column(Modifier.padding(top = 22.dp)) {
        Text(
            stringResource(R.string.detail_manners_heading),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Max)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            arrivalCardText(event.arrivalAdviceMin, event.arrivalAdviceMax)?.let {
                MannerCard(R.drawable.agenda_ic_calendar, colors.creamYellow, stringResource(R.string.detail_arrival_title), it)
            }
            // The artifact shows an [ISI] box here: the app says the rule is still being checked.
            MannerCard(
                R.drawable.agenda_ic_postcard,
                colors.successSurface,
                stringResource(R.string.detail_photo_title),
                stringResource(R.string.detail_photo_checking),
            )
        }
    }
}

@Composable
private fun MannerCard(@DrawableRes icon: Int, tile: Color, title: String, body: String) {
    val colors = KelanaTheme.colors
    Column(
        modifier = Modifier
            .width(150.dp)
            .fillMaxHeight()
            .background(tile, KelanaShapes.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(34.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
            color = colors.ink,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = colors.ink,
        )
    }
}

@Composable
private fun AfterShowNote(placeName: String) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 22.dp)
            .fillMaxWidth()
            .background(colors.successSurface, KelanaShapes.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(painterResource(R.drawable.agenda_ic_postcard), contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            stringResource(R.string.detail_after_show, placeName),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = colors.successTextDeep,
        )
    }
}

/** Bottom bar: white, hairline on top, 52 dp orange pill "Beli tiket resmi". The Ingatkan button needs a route (see report). */
@Composable
private fun BuyBar(onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawTopLine()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(KelanaSizes.buttonHeight)
                .clip(KelanaShapes.button)
                .background(colors.accentOrange)
                .clickable(role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.agenda_ic_ticket), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                stringResource(R.string.detail_buy_ticket),
                style = MaterialTheme.typography.labelLarge,
                color = colors.white,
            )
        }
    }
}

/**
 * "Disarankan tiba" card text: "30–45 menit sebelum mulai." Equal bounds read "Sekitar 30 menit"; a single bound is
 * used as it is; null when no advice is known (the card is then not drawn).
 */
@Composable
private fun arrivalCardText(min: Int?, max: Int?): String? = when (val advice = arrivalAdvice(min, max)) {
    is ArrivalAdvice.Range -> stringResource(R.string.detail_arrival_card_range, advice.min, advice.max)
    is ArrivalAdvice.About -> stringResource(R.string.detail_arrival_card_single, advice.minutes)
    null -> null
}

/** "Candi Prambanan · sekitar 1 jam"; the duration is part of the schedule fact, so it is hidden when that is unchecked. */
@Composable
private fun PlaceAndDuration(event: Event) {
    val schedule = event.schedule.takeIf { it.checkedAt != null }?.value
    val minutes = schedule?.end?.let { Duration.between(schedule.start, it).toMinutes().toInt() }?.takeIf { it > 0 }
    val text = if (minutes == null) {
        event.placeName
    } else {
        stringResource(R.string.detail_place_and_duration, event.placeName, durationText(minutes))
    }
    Text(
        text,
        modifier = Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = KelanaTheme.colors.muted,
    )
}

@Composable
private fun durationText(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.detail_duration_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.detail_duration_hours, hours)
        else -> stringResource(R.string.detail_duration_minutes, minutes)
    }
}

/** "Setiap Jumat, 19.30 sampai 20.30". */
@Composable
private fun scheduleText(schedule: EventSchedule): String {
    val start = formatClock(schedule.start)
    val end = schedule.end?.let(::formatClock)
    return when (val recurrence = schedule.recurrence) {
        is Recurrence.Weekly -> {
            val day = weekdayName(recurrence.weekday)
            if (end == null) {
                stringResource(R.string.detail_schedule_weekly_no_end, day, start)
            } else {
                stringResource(R.string.detail_schedule_weekly, day, start, end)
            }
        }
        Recurrence.Daily ->
            if (end == null) {
                stringResource(R.string.detail_schedule_daily_no_end, start)
            } else {
                stringResource(R.string.detail_schedule_daily, start, end)
            }
        is Recurrence.Once -> {
            val day = formatDayLong(recurrence.date)
            if (end == null) {
                stringResource(R.string.detail_schedule_once_no_end, day, start)
            } else {
                stringResource(R.string.detail_schedule_once, day, start, end)
            }
        }
    }
}

@Composable
private fun priceText(ticket: EventTicket): String =
    ticket.priceFromIdr?.let { stringResource(R.string.detail_price_from, formatRupiah(it)) }
        ?: stringResource(R.string.detail_price_free)

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun DetailPentasPreview() {
    KelanaTheme {
        DetailPentasContent(
            DetailPentasUiState.Found(
                PreviewData.roro,
                SourceNote("example.org", PreviewData.today),
                "https://example.org/tiket",
            ),
            onBack = {},
            onBuyTicket = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun DetailPentasUncheckedPreview() {
    val unchecked = PreviewData.roro.copy(
        schedule = PreviewData.roro.schedule.copy(checkedAt = null),
        ticket = PreviewData.roro.ticket.copy(checkedAt = null),
        arrivalAdviceMin = null,
        arrivalAdviceMax = null,
    )
    KelanaTheme {
        DetailPentasContent(toFound(unchecked), onBack = {}, onBuyTicket = { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun DetailPentasLoadingPreview() {
    KelanaTheme { DetailPentasContent(DetailPentasUiState.Loading, onBack = {}, onBuyTicket = { _, _ -> }) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun DetailPentasNotFoundPreview() {
    KelanaTheme { DetailPentasContent(DetailPentasUiState.NotFound, onBack = {}, onBuyTicket = { _, _ -> }) }
}

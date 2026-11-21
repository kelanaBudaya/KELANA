package id.kelana.feature.tempat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.component.KelanaCard
import id.kelana.core.designsystem.component.OfficialFactRow as PendingFactRow
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Closure
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.TicketPrice
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.navigation.FotoResmiRoute
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.navigation.OfficialSiteRoute
import id.kelana.core.ui.OfficialFactRow
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.formatTimeRange
import id.kelana.core.ui.label
import id.kelana.core.ui.session.rememberRequireLogin
import id.kelana.feature.tempat.R
import id.kelana.feature.tempat.domain.OFFICIAL_PHOTO_COUNT
import id.kelana.feature.tempat.domain.closedDaysFact
import id.kelana.feature.tempat.domain.officialSellerUrl
import id.kelana.feature.tempat.domain.sourceHost
import id.kelana.feature.tempat.domain.todayHours
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import id.kelana.core.designsystem.R as DsR

@Composable
internal fun DetailTempatScreen(
    route: DetailTempatRoute,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailTempatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val requireLogin = rememberRequireLogin(navigate)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val capSoon = stringResource(R.string.tempat_cap_soon)

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
            Scaffold(
                containerColor = KelanaTheme.colors.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                DetailTempatContent(
                    place = place,
                    today = LocalDate.now().dayOfWeek,
                    onBack = onBack,
                    onPhotos = { navigate(FotoResmiRoute(place.id)) },
                    // "Ambil cap" is Phase 3: a guest is sent to Masuk first, a signed-in user sees "segera hadir".
                    onStamp = { requireLogin { scope.launch { snackbar.showSnackbar(capSoon) } } },
                    onTicketSite = { url -> navigate(OfficialSiteRoute(url, place.name)) },
                    onReport = { navigate(LaporkanRoute(place.id)) },
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
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PlaceHeader(place = place, onBack = onBack, onPhotos = onPhotos)
        Column(
            Modifier.padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm)) {
                Pill(text = place.category.label(), background = colors.skyText, content = colors.navy)
                Pill(text = place.capCode, background = colors.creamYellow, content = colors.warnText)
            }
            Text(text = place.name, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(text = place.address, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            Text(text = place.description, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            CapBanner(capCode = place.capCode, onStamp = onStamp)
            InfoResmiCard(place = place, today = today, onTicketSite = onTicketSite)
            StoriesEntry()
            KelanaButton(
                text = stringResource(R.string.tempat_report_link),
                onClick = onReport,
                variant = KelanaButtonVariant.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Illustrated header, 240 dp tall (PROVISIONAL), with back, save and the "Lihat N foto resmi" chip. */
@Composable
private fun PlaceHeader(place: Place, onBack: () -> Unit, onPhotos: () -> Unit) {
    val colors = KelanaTheme.colors
    // PROVISIONAL: visual-only toggle. Saving places is not wired to any storage yet.
    var saved by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        CoverPlaceholder(
            coverKey = place.coverKey,
            modifier = Modifier.fillMaxSize(),
            description = stringResource(R.string.tempat_header_cover, place.name),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.sm, vertical = KelanaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                painterRes = DsR.drawable.ic_back,
                description = stringResource(R.string.tempat_back),
                onClick = onBack,
            )
            Box(Modifier.weight(1f))
            RoundIconButton(
                painterRes = if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline,
                description = stringResource(if (saved) R.string.tempat_unsave else R.string.tempat_save),
                onClick = { saved = !saved },
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(KelanaSpacing.gutter)
                .heightIn(min = 48.dp)
                .clip(KelanaShapes.chip)
                .background(colors.white.copy(alpha = 0.92f))
                .clickable(role = Role.Button, onClick = onPhotos)
                .padding(horizontal = KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tempat_photos_chip, OFFICIAL_PHOTO_COUNT),
                style = MaterialTheme.typography.labelLarge,
                color = colors.navy,
            )
        }
    }
}

@Composable
private fun RoundIconButton(painterRes: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(KelanaTheme.colors.white.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(painterRes),
                contentDescription = description,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun Pill(text: String, background: Color, content: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(KelanaShapes.chip)
            .background(background)
            .padding(horizontal = KelanaSpacing.md, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = content,
    )
}

/** Orange "Cap YK-02 menunggumu" banner. Layout PROVISIONAL (no frame PNG). */
@Composable
private fun CapBanner(capCode: String, onStamp: () -> Unit) {
    val colors = KelanaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.accentOrange)
            .padding(KelanaSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tempat_cap_title, capCode),
                style = MaterialTheme.typography.titleSmall,
                color = colors.white,
            )
            Text(
                text = stringResource(R.string.tempat_cap_body),
                modifier = Modifier.padding(top = KelanaSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.white,
            )
        }
        Button(
            onClick = onStamp,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = KelanaShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = colors.white, contentColor = colors.accentOrange),
        ) { Text(stringResource(R.string.tempat_cap_button)) }
    }
}

/**
 * "Info resmi": hours, tickets and closed days. Every value goes through an OfficialFact row, so a fact without
 * a check date shows "Sedang dicek" and a dated one shows "Diperiksa <tanggal>". Today's hours are emphasized.
 */
@Composable
private fun InfoResmiCard(place: Place, today: DayOfWeek, onTicketSite: (String) -> Unit) {
    val colors = KelanaTheme.colors
    KelanaCard(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.tempat_info_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Spacer12()
        // Jam buka (today, emphasized)
        val todayFact = todayHours(place.hours, today)
        val todayLabel = stringResource(R.string.tempat_hours_today, dayName(today))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(KelanaShapes.card)
                .background(colors.creamYellowAlt)
                .padding(KelanaSpacing.md),
        ) {
            if (todayFact != null) {
                OfficialFactRow(label = todayLabel, fact = todayFact) { formatHours(it) }
            } else {
                PendingFactRow(label = todayLabel, value = null, checkedAt = null)
            }
        }
        // Tiket
        place.tickets.forEach { ticket ->
            Divider12()
            OfficialFactRow(
                label = stringResource(R.string.tempat_ticket, ticket.value.label),
                fact = ticket,
            ) { formatTicket(it) }
        }
        // Hari tutup
        Divider12()
        val closedLabel = stringResource(R.string.tempat_closed_days)
        val closedFact = closedDaysFact(place.hours)
        if (closedFact != null) {
            OfficialFactRow(label = closedLabel, fact = closedFact) { days ->
                if (days.isEmpty()) {
                    stringResource(R.string.tempat_open_every_day)
                } else {
                    days.map { dayName(it) }.joinToString(", ")
                }
            }
        } else {
            PendingFactRow(label = closedLabel, value = null, checkedAt = null)
        }
        place.closures.forEach { closure ->
            Divider12()
            OfficialFactRow(label = stringResource(R.string.tempat_closure), fact = closure) { formatClosure(it) }
        }
        // Source note
        sourceHost(place)?.let { host ->
            Divider12()
            Text(
                text = stringResource(R.string.tempat_source, host),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        officialSellerUrl(place)?.let { url ->
            KelanaButton(
                text = stringResource(R.string.tempat_ticket_site),
                onClick = { onTicketSite(url) },
                variant = KelanaButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth().padding(top = KelanaSpacing.md),
            )
        }
    }
}

@Composable
private fun Spacer12() = Box(Modifier.height(KelanaSpacing.md))

@Composable
private fun Divider12() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = KelanaSpacing.md),
        color = KelanaTheme.colors.outline,
    )
}

/** Visitor stories are the Cerita feature (Phase 3). PROVISIONAL: only an entry row, no data. */
@Composable
private fun StoriesEntry() {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(KelanaShapes.card)
            .background(colors.backgroundWarm)
            .padding(KelanaSpacing.gutter),
    ) {
        Text(
            text = stringResource(R.string.tempat_stories_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
        )
        Text(
            text = stringResource(R.string.tempat_stories_body),
            modifier = Modifier.padding(top = KelanaSpacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
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
private fun formatTicket(ticket: TicketPrice): String {
    val price = ticket.priceIdr ?: return stringResource(R.string.tempat_ticket_pending)
    return formatRupiah(price)
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
    address = "Jl. Pangurakan No. 6, Ngupasan, Gondomanan",
    description = "Museum budaya Jawa: wayang, keris, topeng, dan batik.",
    coverKey = "museum-sonobudoyo",
    hours = DayOfWeek.entries.map {
        OfficialFact(
            OpeningHours(
                it,
                it == DayOfWeek.MONDAY,
                if (it == DayOfWeek.MONDAY) null else java.time.LocalTime.of(8, 0),
                if (it == DayOfWeek.MONDAY) null else java.time.LocalTime.of(21, 0),
            ),
            "https://example.org/sample",
            if (checked) PreviewDate else null,
        )
    },
    tickets = listOf(
        OfficialFact(TicketPrice("Dewasa", 10_000, "https://example.org/tiket"), "https://example.org/sample", if (checked) PreviewDate else null),
        OfficialFact(TicketPrice("Anak-anak", null, null), null, null),
    ),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun DetailTempatCheckedPreview() {
    KelanaTheme {
        DetailTempatContent(previewPlace(true), DayOfWeek.TUESDAY, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun DetailTempatCheckingPreview() {
    KelanaTheme {
        DetailTempatContent(previewPlace(false), DayOfWeek.MONDAY, {}, {}, {}, {}, {})
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

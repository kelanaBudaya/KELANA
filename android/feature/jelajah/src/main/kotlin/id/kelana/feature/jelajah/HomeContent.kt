package id.kelana.feature.jelajah

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.KelanaCard
import id.kelana.core.designsystem.component.PlaceCardSize
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import id.kelana.core.ui.formatTimeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Category tiles and quick actions of the home screen. */
internal enum class HomeTile(@StringRes val label: Int) {
    Museum(R.string.tile_museum),
    Candi(R.string.tile_candi),
    Tari(R.string.tile_tari),
    Topeng(R.string.tile_topeng),
    Gamelan(R.string.tile_gamelan),
    Tiket(R.string.tile_tiket),
    Peta(R.string.tile_peta),
    Semua(R.string.tile_semua),
}

internal enum class QuickAction(@StringRes val label: Int, val needsAccount: Boolean) {
    AmbilCap(R.string.action_stamp, true),
    Rencana(R.string.action_plan, false),
    Agenda(R.string.action_agenda, false),
    Postcard(R.string.action_postcard, true),
}

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "Jumat, 20 November" */
internal fun formatHeaderDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", IdLocale).format(date)

internal class HomeCallbacks(
    val onSearch: () -> Unit,
    val onBell: () -> Unit,
    val onAvatar: () -> Unit,
    val onPassport: () -> Unit,
    val onQuickAction: (QuickAction) -> Unit,
    val onTile: (HomeTile) -> Unit,
    val onEvent: (EventOccurrence) -> Unit,
    val onPlace: (Place) -> Unit,
    val onSeeAll: () -> Unit,
)

/**
 * Jelajah home, compared with docs/design/frames/01.06-jelajah.png. All sizes PROVISIONAL (estimated from the PNG).
 * Tile icons and quick-action icons are placeholder shapes until the illustration drawables exist in :core:designsystem.
 */
@Composable
internal fun HomeContent(
    state: JelajahUiState,
    avatarInitials: String,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxWidth().background(colors.background),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.lg),
    ) {
        item { HomeHeader(state, avatarInitials, callbacks) }
        item { TileGrid(onTile = callbacks.onTile) }
        state.tonightEvent?.let { occurrence ->
            item {
                TonightBanner(
                    occurrence = occurrence,
                    onClick = { callbacks.onEvent(occurrence) },
                    modifier = Modifier.padding(horizontal = KelanaSpacing.gutter),
                )
            }
        }
        item { PicksSection(state.featuredPlaces, callbacks.onPlace, callbacks.onSeeAll) }
        item {
            ChallengeCard(Modifier.padding(horizontal = KelanaSpacing.gutter))
            Spacer(Modifier.height(KelanaSpacing.lg))
        }
    }
}

@Composable
private fun HomeHeader(state: JelajahUiState, avatarInitials: String, callbacks: HomeCallbacks) {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.navy)
            .padding(horizontal = KelanaSpacing.gutter, vertical = KelanaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.sm)) {
            val searchCd = stringResource(R.string.home_search_cd)
            Surface(
                onClick = callbacks.onSearch,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = CircleShape,
                color = colors.white,
            ) {
                Row(
                    Modifier.padding(horizontal = KelanaSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = searchCd,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(colors.muted),
                    )
                    Text(
                        text = stringResource(R.string.home_search_hint),
                        modifier = Modifier.padding(start = KelanaSpacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        maxLines = 1,
                    )
                }
            }
            // Bell is visual-only for now (brief criterion 3).
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(colors.primaryBrightAlt).clickable(
                    role = Role.Button,
                    onClick = callbacks.onBell,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = stringResource(R.string.home_bell_cd),
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(colors.white),
                )
            }
            val avatarCd = stringResource(R.string.home_avatar_cd)
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(colors.stampYellow)
                    .semantics { contentDescription = avatarCd }.clickable(
                    role = Role.Button,
                    onClick = callbacks.onAvatar,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatarInitials,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.navy,
                )
            }
        }
        val date = state.date?.let(::formatHeaderDate)
        val dateLine = when {
            date == null -> null
            state.regionName != null -> stringResource(R.string.home_date_line, state.regionName, date)
            else -> date
        }
        if (dateLine != null) {
            Text(dateLine, style = MaterialTheme.typography.bodyMedium, color = colors.skyText)
        }
        Text(
            text = stringResource(R.string.home_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.white,
        )
        PassportCard(state, callbacks)
    }
}

@Composable
private fun PassportCard(state: JelajahUiState, callbacks: HomeCallbacks) {
    val colors = KelanaTheme.colors
    Surface(shape = KelanaShapes.card, color = colors.primaryDeep, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(KelanaSpacing.md), verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = callbacks.onPassport),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // PLACEHOLDER for the stamp illustration "YK".
                Box(Modifier.size(56.dp).clip(KelanaShapes.card).background(colors.stampYellow))
                Column(Modifier.weight(1f).padding(horizontal = KelanaSpacing.md)) {
                    Text(
                        stringResource(R.string.home_passport_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.skyText,
                    )
                    // PROVISIONAL placeholder count; real stamp data is Phase 3.
                    Text(
                        stringResource(R.string.home_passport_count, state.stampCount, state.stampTotal),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.white,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colors.white),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction.entries.forEach { action ->
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { callbacks.onQuickAction(action) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs),
                    ) {
                        // PLACEHOLDER icon tile (white rounded square, 48 dp).
                        Box(Modifier.size(48.dp).clip(KelanaShapes.input).background(colors.white))
                        Text(
                            stringResource(action.label),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.white,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileGrid(onTile: (HomeTile) -> Unit) {
    val colors = KelanaTheme.colors
    val tints = listOf(
        colors.skyText, colors.backgroundWarm, colors.creamYellow, colors.backgroundWarm,
        colors.creamYellow, colors.skyText, colors.successSurface, colors.backgroundWarm,
    )
    Column(
        Modifier.padding(horizontal = KelanaSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        HomeTile.entries.chunked(4).forEachIndexed { row, tiles ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md)) {
                tiles.forEachIndexed { col, tile ->
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onTile(tile) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.sm),
                    ) {
                        // PLACEHOLDER tile illustration (about 76 dp square, PROVISIONAL).
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(KelanaShapes.card)
                                .background(tints[row * 4 + col]),
                        )
                        Text(
                            stringResource(tile.label),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TonightBanner(occurrence: EventOccurrence, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    val time = (event.schedule.display() as? FactDisplay.Checked)?.value?.let { formatTimeRange(it.start, it.end) }
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = KelanaShapes.card, color = colors.accentOrange) {
        Column(Modifier.padding(KelanaSpacing.gutter), verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs)) {
            Text(
                stringResource(R.string.banner_tonight_at, event.placeName).uppercase(IdLocale),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.creamYellow,
            )
            Text(
                event.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FrauncesFontFamily),
                color = colors.white,
            )
            if (time != null) {
                Text(
                    stringResource(R.string.banner_time, time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.white,
                )
            }
            Spacer(Modifier.height(KelanaSpacing.sm))
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.white)
                    .padding(horizontal = KelanaSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.banner_cta),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accentOrange,
                )
            }
        }
    }
}

@Composable
private fun PicksSection(places: List<Place>, onPlace: (Place) -> Unit, onSeeAll: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(KelanaSpacing.md)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = KelanaSpacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_picks_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onSeeAll), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                )
            }
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = KelanaSpacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
        ) {
            items(places, key = { it.id }) { place ->
                PlaceItem(
                    place = place,
                    onClick = { onPlace(place) },
                    modifier = Modifier.width(156.dp),
                    size = PlaceCardSize.Grid,
                )
            }
        }
    }
}

/** Weekly challenge card. PROVISIONAL static content: stamps and challenges are Phase 3. */
@Composable
private fun ChallengeCard(modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    KelanaCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(KelanaShapes.card).background(colors.creamYellow))
            Column(Modifier.weight(1f).padding(start = KelanaSpacing.md)) {
                Text(
                    stringResource(R.string.challenge_label).uppercase(IdLocale),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.warnText,
                )
                Text(
                    stringResource(R.string.challenge_title, PLACEHOLDER_CHALLENGE_GOAL),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                )
                LinearProgressIndicator(
                    progress = { PLACEHOLDER_CHALLENGE_DONE.toFloat() / PLACEHOLDER_CHALLENGE_GOAL },
                    modifier = Modifier.fillMaxWidth().padding(top = KelanaSpacing.sm).height(8.dp).clip(CircleShape),
                    color = colors.accentOrange,
                    trackColor = colors.backgroundWarm,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text(
                    stringResource(R.string.challenge_progress, PLACEHOLDER_CHALLENGE_DONE, PLACEHOLDER_CHALLENGE_GOAL),
                    modifier = Modifier.padding(top = KelanaSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HomeContentPreview() {
    KelanaTheme {
        HomeContent(
            state = PreviewData.homeState(),
            avatarInitials = "MH",
            callbacks = HomeCallbacks({}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeContentNoEventPreview() {
    KelanaTheme {
        HomeContent(
            state = PreviewData.homeState().copy(tonightEvent = null),
            avatarInitials = "?",
            callbacks = HomeCallbacks({}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}

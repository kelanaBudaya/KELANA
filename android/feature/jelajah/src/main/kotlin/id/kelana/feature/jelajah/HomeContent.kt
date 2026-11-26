package id.kelana.feature.jelajah

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.designsystem.theme.SerifHeadline
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import id.kelana.core.model.SessionState
import id.kelana.core.ui.formatTimeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Category tiles and quick actions of the home screen. */
internal enum class HomeTile(@StringRes val label: Int) {
    Museum(R.string.tile_museum),
    Candi(R.string.tile_candi),
    Tari(R.string.tile_tari),
    Pentas(R.string.tile_pentas),
    SeniMusik(R.string.tile_seni_musik),
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

/** The greeting line above the headline. Guests and a session still loading get no greeting at all. */
internal sealed interface GreetingName {
    data object None : GreetingName
    /** Signed in without a usable display name: plain "Halo!" (the email is never used). */
    data object Anonymous : GreetingName
    data class Named(val firstName: String) : GreetingName
}

/** First word of the display name of a signed-in user. */
internal fun greetingNameOf(state: SessionState): GreetingName = when (state) {
    is SessionState.SignedIn -> state.user.displayName
        ?.trim()?.split(Regex("""\s+"""))?.firstOrNull { it.isNotEmpty() }
        ?.let { GreetingName.Named(it) } ?: GreetingName.Anonymous
    else -> GreetingName.None
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

/** The passport card and everything below it overlap the header by this much (artifact: `margin: -48px 16px 0`). */
private val Overlap = (-48).dp

/** Moves the item up by [by] (negative value) and shrinks its layout height by the same amount. */
private fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = by.roundToPx()
    layout(placeable.width, placeable.height + shift) { placeable.place(0, shift) }
}

/**
 * Jelajah home, compared with docs/design/artifact/JelajahIlustratif.dc.html (code against the HTML, no render seen).
 * The bottom navigation is owned by :app and is not drawn here.
 */
@Composable
internal fun HomeContent(
    state: JelajahUiState,
    avatarInitials: String,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    greeting: GreetingName = GreetingName.None,
) {
    val colors = KelanaTheme.colors
    var showChallenge by rememberSaveable { mutableStateOf(false) }
    if (showChallenge) {
        ChallengeSheet(
            weekly = state.weekly,
            onDismiss = { showChallenge = false },
            onStamp = { showChallenge = false; callbacks.onQuickAction(QuickAction.AmbilCap) },
            onPassport = { showChallenge = false; callbacks.onPassport() },
        )
    }
    LazyColumn(modifier = modifier.fillMaxWidth().background(colors.background)) {
        item { HomeHeader(state, avatarInitials, greeting, callbacks) }
        item { PassportCard(state, callbacks, Modifier.pullUp(Overlap).padding(horizontal = 16.dp)) }
        item { TileGrid(onTile = callbacks.onTile, modifier = Modifier) }
        state.tonightEvent?.let { occurrence ->
            item {
                TonightBanner(
                    occurrence = occurrence,
                    onClick = { callbacks.onEvent(occurrence) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
                )
            }
        }
        item { PicksSection(state.featuredPlaces, callbacks.onPlace, callbacks.onSeeAll, Modifier) }
        item {
            ChallengeCard(
                state.weekly,
                onClick = { showChallenge = true },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(state: JelajahUiState, avatarInitials: String, greeting: GreetingName, callbacks: HomeCallbacks) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxWidth().background(colors.primary)) {
        Image(
            painter = painterResource(R.drawable.jelajah_art_home_header_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(390f / 220f),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopStart,
        )
        // The header grows with the content (the greeting line adds height), so the passport card, pulled up 48 dp over
        // the 64 dp bottom padding, always clears the headline.
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 64.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = callbacks.onSearch,
                    modifier = Modifier.weight(1f).height(KelanaSizes.inputHeight),
                    shape = CircleShape,
                    color = colors.white,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.home_search_cd),
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(colors.muted),
                        )
                        Text(
                            text = stringResource(R.string.home_search_hint),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // The bell opens the notification center (NotifikasiRoute).
                Box(
                    Modifier.size(KelanaSizes.inputHeight).clip(CircleShape).background(colors.white.copy(alpha = 0.16f))
                        .clickable(role = Role.Button, onClick = callbacks.onBell),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = stringResource(R.string.home_bell_cd),
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(colors.white),
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(8.dp)
                            .clip(CircleShape).background(colors.stampYellow),
                    )
                }
                val avatarCd = stringResource(R.string.home_avatar_cd)
                Box(
                    Modifier.size(KelanaSizes.inputHeight).clip(CircleShape).background(colors.stampYellow)
                        .semantics { contentDescription = avatarCd }
                        .clickable(role = Role.Button, onClick = callbacks.onAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = avatarInitials,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
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
            Column(Modifier.padding(top = 16.dp)) {
                val greetingText = when (greeting) {
                    GreetingName.None -> null
                    GreetingName.Anonymous -> stringResource(R.string.home_greeting)
                    is GreetingName.Named -> stringResource(R.string.home_greeting_name, greeting.firstName)
                }
                if (greetingText != null) {
                    Text(
                        text = greetingText,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.white,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (dateLine != null) {
                    Text(dateLine, style = MaterialTheme.typography.bodySmall, color = colors.skyText)
                }
                Text(
                    text = stringResource(R.string.home_headline),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.white,
                )
            }
        }
    }
}

/** Passport card: stamp sticker, count, chevron and the four quick actions. */
@Composable
private fun PassportCard(state: JelajahUiState, callbacks: HomeCallbacks, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(KelanaRadii.listCard)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = colors.navy, spotColor = colors.navy)
            .clip(shape)
            .background(colors.navy)
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = callbacks.onPassport),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StampSticker(state.stampCount, state.stampTotal, Modifier.rotate(-6f))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_passport_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = colors.skyText,
                )
                // Real counts from the user's confirmed stamps; no number until the places are loaded.
                Text(
                    if (state.stampTotal > 0) {
                        stringResource(R.string.home_passport_count, state.stampCount, state.stampTotal)
                    } else {
                        stringResource(R.string.home_passport_empty)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = colors.white,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(colors.white),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickAction.entries.forEach { action ->
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { callbacks.onQuickAction(action) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).clip(KelanaShapes.input).background(colors.background),
                        contentAlignment = Alignment.Center,
                    ) { Art(action.art, 26.dp) }
                    Text(
                        stringResource(action.label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.white,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val QuickAction.art: Int
    get() = when (this) {
        QuickAction.AmbilCap -> R.drawable.jelajah_art_badge_stamp
        QuickAction.Rencana -> R.drawable.jelajah_art_rencana
        QuickAction.Agenda -> R.drawable.jelajah_art_agenda
        QuickAction.Postcard -> R.drawable.jelajah_art_postcard
    }

/** Yellow passport sticker with a white outline, 56 x 42 dp. */
@Composable
private fun StampSticker(count: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val cd = stringResource(R.string.home_stamp_cd)
    Box(
        modifier.size(width = 56.dp, height = 42.dp).semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.jelajah_art_stamp_shape),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Art(R.drawable.jelajah_art_museum_navy, 16.dp)
            Text(
                stringResource(R.string.home_stamp_code),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
                color = colors.navy,
            )
            Text(
                if (total > 0) "$count/$total" else "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                color = colors.navy,
            )
        }
    }
}

@Composable
private fun TileGrid(onTile: (HomeTile) -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(
        modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeTile.entries.chunked(4).forEach { tiles ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEach { tile ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { onTile(tile) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier.size(62.dp).clip(RoundedCornerShape(KelanaRadii.listCard)).background(tile.tint(colors)),
                            contentAlignment = Alignment.Center,
                        ) { Art(tile.art, 38.dp) }
                        Text(
                            stringResource(tile.label),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

private fun HomeTile.tint(colors: KelanaColors): Color = when (this) {
    HomeTile.Museum, HomeTile.Tiket -> colors.skyText
    HomeTile.Candi, HomeTile.Pentas -> colors.peach
    HomeTile.Tari, HomeTile.SeniMusik -> colors.creamYellow
    HomeTile.Peta -> colors.successSurface
    HomeTile.Semua -> colors.backgroundWarm
}

private val HomeTile.art: Int
    get() = when (this) {
        HomeTile.Museum -> R.drawable.jelajah_art_museum
        HomeTile.Candi -> R.drawable.jelajah_art_candi
        HomeTile.Tari -> R.drawable.jelajah_art_tari
        HomeTile.Pentas -> R.drawable.jelajah_art_pentas
        HomeTile.SeniMusik -> R.drawable.jelajah_art_seni_musik
        HomeTile.Tiket -> R.drawable.jelajah_art_tiket
        HomeTile.Peta -> R.drawable.jelajah_art_peta
        HomeTile.Semua -> R.drawable.jelajah_art_semua
    }

/** "Malam ini di ..." banner: 170 dp tall, orange, illustration pieces on the right. */
@Composable
private fun TonightBanner(occurrence: EventOccurrence, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val event = occurrence.event
    val time = (event.schedule.display() as? FactDisplay.Checked)?.value?.let { formatTimeRange(it.start, it.end) }
    Box(
        modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(KelanaRadii.listCard))
            .background(colors.accentOrange).clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.jelajah_art_banner_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        ArtPiece(R.drawable.jelajah_art_tari, 96.dp, 214.dp, 30.dp, -6f)
        ArtPiece(R.drawable.jelajah_art_seni_musik, 58.dp, 290.dp, 96.dp, 8f)
        ArtPiece(R.drawable.jelajah_art_agenda, 40.dp, 182.dp, 108.dp, -10f)
        Column(Modifier.fillMaxHeight().padding(start = 18.dp, top = 18.dp, end = 170.dp)) {
            Text(
                stringResource(R.string.banner_tonight_at, event.placeName).uppercase(IdLocale),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                color = colors.creamYellow,
            )
            Text(
                event.title,
                modifier = Modifier.padding(top = 6.dp),
                style = SerifHeadline,
                color = colors.white,
            )
            if (time != null) {
                Text(
                    stringResource(R.string.banner_time, time),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.peachSoft,
                )
            }
            Box(
                Modifier.padding(top = 12.dp).height(32.dp).clip(KelanaShapes.chip)
                    .background(colors.white).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.banner_cta),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.accentOrange,
                )
            }
        }
    }
}

@Composable
private fun PicksSection(places: List<Place>, onPlace: (Place) -> Unit, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(modifier.padding(top = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_picks_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            Box(
                Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onSeeAll),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(places, key = { it.id }) { place -> PickCard(place, onClick = { onPlace(place) }) }
        }
    }
}

/** 156 dp wide pick: 156 x 112 cover, name 14/800, subtitle 12 muted. No check badge here, as in the artifact. */
@Composable
private fun PickCard(place: Place, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    Column(
        Modifier.width(156.dp).clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(156f / 112f).clip(KelanaShapes.card)) {
            PlaceCoverPhoto(place, compact = false)
        }
        Text(
            place.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp),
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            place.description,
            modifier = Modifier.offset(y = (-4).dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Weekly challenge card: real progress (server-confirmed stamps this week). Tapping opens [ChallengeSheet]. */
@Composable
private fun ChallengeCard(weekly: WeeklyProgress, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(KelanaRadii.listCard)
    Row(
        modifier.fillMaxWidth().clip(shape).background(colors.white).border(1.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(76.dp).clip(KelanaShapes.card).background(colors.creamYellow)) {
            Image(
                painter = painterResource(R.drawable.jelajah_art_challenge_bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            ArtPiece(R.drawable.jelajah_art_badge_stamp, 40.dp, 18.dp, 18.dp, -8f)
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.challenge_label).uppercase(IdLocale),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
                color = colors.warnText,
            )
            Text(
                stringResource(R.string.challenge_title, weekly.target),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            val progressCd = stringResource(R.string.challenge_progress_cd)
            Box(
                Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.backgroundWarm)
                    .semantics { contentDescription = progressCd },
            ) {
                Box(
                    Modifier.fillMaxWidth(weekly.fraction)
                        .height(8.dp).clip(CircleShape).background(colors.accentOrange),
                )
            }
            Text(
                stringResource(R.string.challenge_progress, weekly.done, weekly.target),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.muted,
            )
        }
    }
}

/** Weekly challenge sheet: goal, real progress and two actions. No reward line: no badge system exists yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeSheet(weekly: WeeklyProgress, onDismiss: () -> Unit, onStamp: () -> Unit, onPassport: () -> Unit) {
    val colors = KelanaTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.challenge_label),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.challenge_title, weekly.target),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            val progressCd = stringResource(R.string.challenge_progress_cd)
            Box(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(10.dp).clip(CircleShape).background(colors.backgroundWarm)
                    .semantics { contentDescription = progressCd },
            ) {
                Box(Modifier.fillMaxWidth(weekly.fraction).height(10.dp).clip(CircleShape).background(colors.accentOrange))
            }
            Text(
                stringResource(R.string.challenge_sheet_progress, weekly.done, weekly.target),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            if (weekly.isComplete) {
                Text(
                    stringResource(R.string.challenge_sheet_done),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
            }
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KelanaButton(stringResource(R.string.action_stamp), onStamp, Modifier.weight(1f))
                KelanaButton(
                    stringResource(R.string.challenge_sheet_passport), onPassport, Modifier.weight(1f),
                    variant = KelanaButtonVariant.Outline,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1420)
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

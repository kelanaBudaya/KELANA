package id.kelana.feature.notifikasi.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaChip
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.NotificationCategory
import id.kelana.core.navigation.DetailPentasRoute
import id.kelana.core.navigation.DetailTempatRoute
import id.kelana.core.ui.formatClock
import id.kelana.feature.notifikasi.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun NotifikasiScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    navigate: (Any) -> Unit,
    viewModel: NotifikasiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    NotifikasiContent(
        state = state,
        onBack = onBack,
        onSettings = onSettings,
        onFilter = viewModel::selectFilter,
        onOpen = { item ->
            // A show leads to Detail pentas, a place to Detail tempat; entries with neither just stay.
            when {
                item.eventId != null -> navigate(DetailPentasRoute(item.eventId))
                item.placeId != null -> navigate(DetailTempatRoute(item.placeId))
            }
        },
    )
}

@StringRes
private fun NotificationFilter.labelRes(): Int = when (this) {
    NotificationFilter.All -> R.string.notifikasi_chip_all
    NotificationFilter.Rencana -> R.string.notifikasi_chip_rencana
    NotificationFilter.Paspor -> R.string.notifikasi_chip_paspor
    NotificationFilter.Cerita -> R.string.notifikasi_chip_cerita
}

@DrawableRes
private fun NotificationCategory.iconRes(): Int = when (this) {
    NotificationCategory.Rencana -> R.drawable.notifikasi_cat_rencana
    NotificationCategory.Paspor -> R.drawable.notifikasi_cat_paspor
    NotificationCategory.Info -> R.drawable.notifikasi_cat_info
    NotificationCategory.Cerita -> R.drawable.notifikasi_cat_cerita
}

@Composable
private fun NotificationCategory.tileColor(): Color {
    val c = KelanaTheme.colors
    return when (this) {
        NotificationCategory.Rencana -> c.creamYellow
        NotificationCategory.Paspor -> c.successSurface
        NotificationCategory.Info -> c.peach
        NotificationCategory.Cerita -> c.skyText
    }
}

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")
private val DateHeader: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", IdLocale)

private fun formatDateHeader(date: LocalDate): String = DateHeader.format(date).uppercase(IdLocale)

// Layout from NotifikasiIlustratif.dc.html. Compared as code, not as a render.
@Composable
internal fun NotifikasiContent(
    state: NotifikasiUiState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onFilter: (NotificationFilter) -> Unit,
    onOpen: (NotificationItem) -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.notifikasi_ic_back),
                    contentDescription = stringResource(R.string.notifikasi_back),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(R.string.notifikasi_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Box(
                Modifier.size(48.dp).clickable(role = Role.Button, onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.notifikasi_ic_settings),
                    contentDescription = stringResource(R.string.notifikasi_settings_cd),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationFilter.entries.forEach { f ->
                KelanaChip(
                    label = stringResource(f.labelRes()),
                    selected = state.filter == f,
                    onClick = { onFilter(f) },
                )
            }
        }
        when {
            state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // PROVISIONAL: the artifact has no empty state; built from the design tokens and one of its icons.
                EmptyState(
                    title = stringResource(R.string.notifikasi_empty_title),
                    body = stringResource(R.string.notifikasi_empty_body),
                    illustration = {
                        Box(
                            Modifier.size(96.dp).clip(CircleShape).background(colors.creamYellow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.notifikasi_cat_rencana),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    },
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.groups.forEachIndexed { index, group ->
                    item(key = "header-${group.date}") {
                        Text(
                            text = when (group.relative) {
                                RelativeDay.Today -> stringResource(R.string.notifikasi_group_today)
                                RelativeDay.Yesterday -> stringResource(R.string.notifikasi_group_yesterday)
                                RelativeDay.Other -> formatDateHeader(group.date)
                            },
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = if (index == 0) 0.dp else 8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = colors.muted,
                        )
                    }
                    items(group.items, key = { it.id }) { item -> NotificationRow(item, onClick = { onOpen(item) }) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (item.isUnread) colors.surface else Color.Transparent)
            .border(1.dp, if (item.isUnread) colors.outline else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(item.category.tileColor()),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(item.category.iconRes()), contentDescription = null, modifier = Modifier.size(34.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Text(
                text = item.body,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = colors.muted,
            )
            Text(
                text = formatClock(item.time),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.iconMuted,
            )
        }
        if (item.isUnread) {
            val cd = stringResource(R.string.notifikasi_unread_cd)
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.accentOrange)
                    .semantics { contentDescription = cd },
            )
        }
    }
}

private val PreviewItems = listOf(
    NotificationItem("1", NotificationCategory.Rencana, "Waktunya berangkat ke Prambanan", "Sendratari Roro Jonggrang mulai 19.30 di Prambanan. Jadwal diperiksa 20 Nov 2026.", LocalTime.of(17, 40), true, "prambanan", "roro"),
    NotificationItem("2", NotificationCategory.Paspor, "Cap YK-02 masuk paspormu", "Kamu sudah berkelana ke Museum Sonobudoyo!", LocalTime.of(10, 5), false, "sonobudoyo", null),
)

private val PreviewGroups = listOf(
    NotificationGroup(LocalDate.of(2026, 11, 20), RelativeDay.Today, PreviewItems),
    NotificationGroup(LocalDate.of(2026, 11, 19), RelativeDay.Yesterday, PreviewItems.take(1)),
    NotificationGroup(LocalDate.of(2026, 11, 14), RelativeDay.Other, PreviewItems.drop(1)),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotifikasiListPreview() = KelanaTheme {
    NotifikasiContent(NotifikasiUiState(groups = PreviewGroups, isLoading = false), {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotifikasiFilteredPreview() = KelanaTheme {
    NotifikasiContent(
        NotifikasiUiState(filter = NotificationFilter.Paspor, groups = PreviewGroups.take(1), isLoading = false),
        {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotifikasiEmptyPreview() = KelanaTheme {
    NotifikasiContent(NotifikasiUiState(isLoading = false), {}, {}, {}, {})
}

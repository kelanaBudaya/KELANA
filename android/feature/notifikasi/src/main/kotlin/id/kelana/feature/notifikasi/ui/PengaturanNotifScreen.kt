package id.kelana.feature.notifikasi.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.notifikasi.R
import id.kelana.feature.notifikasi.domain.NotificationSettings

@Composable
internal fun PengaturanNotifScreen(onBack: () -> Unit, viewModel: PengaturanNotifViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    PengaturanNotifContent(
        state = state,
        onBack = onBack,
        onShowReminders = viewModel::setShowReminders,
        onStampConfirmations = viewModel::setStampConfirmations,
        onQuietHours = viewModel::setQuietHours,
    )
}

// Layout from PengaturanNotif.dc.html, reduced to the switches this build has a producer for.
// Compared as code, not as a render.
@Composable
internal fun PengaturanNotifContent(
    state: PengaturanNotifUiState,
    onBack: () -> Unit,
    onShowReminders: (Boolean) -> Unit,
    onStampConfirmations: (Boolean) -> Unit,
    onQuietHours: (Boolean) -> Unit,
) {
    val colors = KelanaTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                text = stringResource(R.string.pengaturan_notif_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
        }
        Section(R.string.pengaturan_notif_section_topics, topPadding = 10.dp) {
            SwitchRow(
                title = R.string.pengaturan_notif_leave_title,
                body = R.string.pengaturan_notif_leave_body,
                checked = state.settings.showReminders,
                onChange = onShowReminders,
            )
            SwitchRow(
                title = R.string.pengaturan_notif_stamp_title,
                body = R.string.pengaturan_notif_stamp_body,
                checked = state.settings.stampConfirmations,
                onChange = onStampConfirmations,
            )
        }
        Section(R.string.pengaturan_notif_section_quiet, topPadding = 20.dp) {
            SwitchRow(
                title = R.string.pengaturan_notif_quiet_title,
                body = R.string.pengaturan_notif_quiet_body,
                checked = state.settings.quietHours,
                onChange = onQuietHours,
            )
        }
    }
}

@Composable
private fun Section(titleRes: Int, topPadding: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = topPadding)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
            ),
            color = KelanaTheme.colors.muted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

/** One row: border on top, 14 dp padding, title 15/700, body 13/18 muted, switch 46 x 28 on the right. The whole row toggles. */
@Composable
private fun SwitchRow(title: Int, body: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = KelanaTheme.colors
    HorizontalDivider(color = colors.outline)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = colors.ink,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        SwitchVisual(checked, Modifier.padding(top = 2.dp))
    }
}

/** The artifact's switch: 46 x 28 pill, 22 dp thumb with 3 dp inset. Drawn here, state belongs to the row. */
@Composable
private fun SwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val thumbX by animateDpAsState(if (checked) 21.dp else 3.dp, label = "thumb")
    Box(
        modifier
            .width(46.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (checked) colors.primary else colors.divider),
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), 3.dp.roundToPx()) }
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .background(colors.white, CircleShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PengaturanNotifPreview() = KelanaTheme {
    PengaturanNotifContent(PengaturanNotifUiState(NotificationSettings(), true), {}, {}, {}, {})
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PengaturanNotifOffPreview() = KelanaTheme {
    PengaturanNotifContent(
        PengaturanNotifUiState(NotificationSettings(showReminders = false, stampConfirmations = false, quietHours = false), true),
        {}, {}, {}, {},
    )
}

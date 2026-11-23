package id.kelana.feature.notifikasi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.notifikasi.R

/** True on API 33+ when POST_NOTIFICATIONS is not granted yet. Below 33 the permission does not exist. */
internal fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/**
 * 4.9 Izin notifikasi, a sheet. "Ingatkan aku" asks the system for POST_NOTIFICATIONS; "Nanti saja" and the close
 * button just close. [onDone] runs in every case (the reminder is stored either way). When the permission is not
 * needed (below API 33, or already granted) the sheet is skipped: it calls [onDone] at once.
 */
@SuppressLint("InlinedApi") // POST_NOTIFICATIONS is only requested when needsNotificationPermission() says API 33+
@Composable
internal fun IzinNotifScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val needed = remember { needsNotificationPermission(context) }
    LaunchedEffect(needed) { if (!needed) onDone() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    if (!needed) return
    IzinNotifContent(
        onClose = onDone,
        onLater = onDone,
        onAllow = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    )
}

// Layout from IzinNotifIlustratif.dc.html: bottom sheet over a 55% ink scrim. Compared as code, not as a render.
@Composable
internal fun IzinNotifContent(onClose: () -> Unit, onLater: () -> Unit, onAllow: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.inkDeep.copy(alpha = 0.55f))) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.background),
        ) {
            Column {
                // Art panel 250 dp: blob and sparkles, then three illustrations at the artifact's offsets and tilts.
                Box(Modifier.fillMaxWidth().height(250.dp).background(colors.skyText)) {
                    Image(
                        painter = painterResource(R.drawable.notifikasi_izin_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Image(
                        painter = painterResource(R.drawable.notifikasi_izin_art_1),
                        contentDescription = null,
                        modifier = Modifier.offset(x = 40.dp, y = 40.dp).size(98.dp).rotate(-8f),
                    )
                    Image(
                        painter = painterResource(R.drawable.notifikasi_izin_art_2),
                        contentDescription = null,
                        modifier = Modifier.offset(x = 190.dp, y = 52.dp).size(118.dp).rotate(4f),
                    )
                    Image(
                        painter = painterResource(R.drawable.notifikasi_izin_art_3),
                        contentDescription = null,
                        modifier = Modifier.offset(x = 130.dp, y = 150.dp).size(56.dp).rotate(-6f),
                    )
                }
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp)) {
                    Text(
                        text = stringResource(R.string.izin_notif_title),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.izin_notif_body),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Row(Modifier.padding(top = 18.dp)) {
                        SheetButton(
                            text = stringResource(R.string.izin_notif_later),
                            onClick = onLater,
                            filled = false,
                            modifier = Modifier.weight(1f),
                        )
                        Box(Modifier.size(10.dp))
                        SheetButton(
                            text = stringResource(R.string.izin_notif_allow),
                            onClick = onAllow,
                            filled = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.white.copy(alpha = 0.9f))
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.notifikasi_ic_close),
                    contentDescription = stringResource(R.string.izin_notif_close),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 48 dp pill, text 15/800: filled primary, or outlined with a 1.5 dp primary border (as in the artifact). */
@Composable
private fun SheetButton(text: String, onClick: () -> Unit, filled: Boolean, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(colors.primary) else Modifier.border(1.5.dp, colors.primary, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = if (filled) colors.white else colors.primary,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IzinNotifPreview() = KelanaTheme { IzinNotifContent({}, {}, {}) }

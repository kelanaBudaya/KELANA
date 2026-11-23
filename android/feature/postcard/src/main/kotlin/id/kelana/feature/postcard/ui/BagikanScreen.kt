package id.kelana.feature.postcard.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.navigation.PostcardRoute
import id.kelana.feature.postcard.R

private const val INSTAGRAM_PACKAGE = "com.instagram.android"
private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val INSTAGRAM_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"

/** Bagikan postcard (7.2). Shares through the FileProvider; no TikTok. */
@Composable
internal fun BagikanScreen(
    onBack: () -> Unit,
    onReplace: (Any) -> Unit,
    viewModel: BagikanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val saved = stringResource(R.string.postcard_notice_saved)
    val saveFailed = stringResource(R.string.postcard_notice_save_failed)
    val shareFailed = stringResource(R.string.postcard_notice_share_failed)
    LaunchedEffect(state.notice) {
        val text = when (state.notice) {
            BagikanNotice.Saved -> saved
            BagikanNotice.SaveFailed -> saveFailed
            BagikanNotice.ShareFailed -> shareFailed
            null -> return@LaunchedEffect
        }
        snackbar.showSnackbar(text)
        viewModel.onNoticeShown()
    }

    val chooserTitle = stringResource(R.string.postcard_share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val ok = launchShare(context, event, chooserTitle)
            if (!ok) viewModel.onShareFailed()
        }
    }

    BagikanContent(
        state = state,
        actions = BagikanActions(
            onBack = onBack,
            onFormat = viewModel::onFormat,
            onShare = viewModel::onShare,
            onSave = viewModel::onSave,
            onWriteAgain = { onReplace(PostcardRoute()) },
        ),
        snackbarHost = snackbar,
    )
}

private fun isInstalled(context: Context, packageName: String): Boolean = try {
    context.packageManager.getPackageInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

/**
 * Opens the target app with the rendered picture. Instagram Stories and WhatsApp are used only when installed;
 * otherwise (and for "Lainnya") the system share sheet opens. Returns false when nothing could be opened.
 */
private fun launchShare(context: Context, event: ShareEvent, chooserTitle: String): Boolean {
    val uri = Uri.parse(event.uri)
    return try {
        when {
            event.target == ShareTarget.InstagramStory && isInstalled(context, INSTAGRAM_PACKAGE) -> {
                val intent = Intent(INSTAGRAM_STORY_ACTION).apply {
                    setDataAndType(uri, "image/png")
                    setPackage(INSTAGRAM_PACKAGE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra("source_application", context.packageName)
                }
                context.grantUriPermission(INSTAGRAM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
            }
            event.target == ShareTarget.WhatsApp && isInstalled(context, WHATSAPP_PACKAGE) -> {
                val intent = sendIntent(uri).setPackage(WHATSAPP_PACKAGE)
                context.grantUriPermission(WHATSAPP_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
            }
            else -> context.startActivity(Intent.createChooser(sendIntent(uri), chooserTitle))
        }
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun sendIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newRawUri("", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

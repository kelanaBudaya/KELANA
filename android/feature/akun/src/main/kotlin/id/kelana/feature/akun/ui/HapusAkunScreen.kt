package id.kelana.feature.akun.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaRadii
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.ACCOUNT_DELETION_CONFIRM_WORD
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.feature.akun.R

private val SheetShape = RoundedCornerShape(topStart = KelanaRadii.sheet, topEnd = KelanaRadii.sheet)
private val PanelShape = RoundedCornerShape(14.dp)

@Composable
internal fun HapusAkunScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: HapusAkunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    // Storage Access Framework: the user picks where the file goes; no storage permission.
    val createExportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE),
    ) { uri -> viewModel.onExportDestination(uri?.toString()) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HapusAkunEvent.Deleted -> onDeleted()
                is HapusAkunEvent.ChooseExportFile -> try {
                    createExportFile.launch(event.fileName)
                } catch (_: ActivityNotFoundException) {
                    viewModel.onExportPickerUnavailable()
                }
            }
        }
    }
    // While the server is deleting, back does nothing: the outcome must reach this screen.
    BackHandler(enabled = state.isDeleting) {}
    HapusAkunContent(
        state = state,
        onConfirmationChange = viewModel::onConfirmationChange,
        onDelete = viewModel::onDelete,
        onCancel = { if (!state.isDeleting) onBack() },
        onExport = viewModel::onExport,
    )
}

/** Bottom sheet over a 55% ink scrim, as HapusAkun.dc.html draws it over Profil. Compared as code, not as a render. */
@Composable
internal fun HapusAkunContent(
    state: HapusAkunUiState,
    onConfirmationChange: (String) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onExport: () -> Unit = {},
) {
    val colors = KelanaTheme.colors
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim: a tap outside the sheet is "Batal". Pointer only, no semantics; Batal is the accessible way out.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.inkDeep.copy(alpha = 0.55f))
                .pointerInput(onCancel) { detectTapGestures { onCancel() } },
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .clip(SheetShape)
                .background(colors.background)
                // Swallow taps so they do not reach the scrim.
                .pointerInput(Unit) { detectTapGestures {} }
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 10.dp, bottom = 26.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(KelanaShapes.chip)
                    .background(colors.outline),
            )
            Text(
                text = stringResource(R.string.hapus_akun_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.hapus_akun_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp),
            )

            DeletedPanel(summary = state.summary)
            KeptPanel()
            DownloadLink(state = state, onExport = onExport)

            Text(
                text = stringResource(R.string.hapus_akun_confirm_label, ACCOUNT_DELETION_CONFIRM_WORD),
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            ConfirmField(
                value = state.confirmation,
                enabled = !state.isDeleting,
                onValueChange = onConfirmationChange,
                onDone = onDelete,
            )

            state.error?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.dangerStrong,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetButton(
                    text = stringResource(R.string.hapus_akun_cancel),
                    onClick = onCancel,
                    enabled = !state.isDeleting,
                    containerColor = colors.surface,
                    contentColor = colors.ink,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.weight(1f),
                )
                SheetButton(
                    text = stringResource(
                        if (state.isDeleting) R.string.hapus_akun_deleting else R.string.hapus_akun_submit,
                    ),
                    onClick = onDelete,
                    enabled = state.canDelete,
                    containerColor = if (state.canDelete) colors.dangerStrong else colors.dangerDisabled,
                    contentColor = colors.white,
                    border = null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DeletedPanel(summary: AccountDeletionSummary) {
    val stamps = summary.stampCount?.let { pluralStringResource(R.plurals.hapus_akun_stamps_count, it, it) }
        ?: stringResource(R.string.hapus_akun_stamps)
    val postcards = summary.postcardCount
    val videos = summary.videoCount
    // Both numbers or none: a half-counted line would read as if the missing kind did not exist.
    val stories = if (postcards != null && videos != null) {
        stringResource(
            R.string.hapus_akun_stories_count,
            pluralStringResource(R.plurals.hapus_akun_postcards_part, postcards, postcards),
            pluralStringResource(R.plurals.hapus_akun_videos_part, videos, videos),
        )
    } else {
        stringResource(R.string.hapus_akun_stories)
    }
    Panel(
        title = stringResource(R.string.hapus_akun_deleted_header),
        titleColor = KelanaTheme.colors.dangerStrong,
        background = KelanaTheme.colors.dangerSurface,
        border = null,
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelLine(R.drawable.akun_hapus_ic_x, stamps, KelanaTheme.colors.ink)
            PanelLine(R.drawable.akun_hapus_ic_x, stories, KelanaTheme.colors.ink)
            PanelLine(R.drawable.akun_hapus_ic_x, stringResource(R.string.hapus_akun_plans), KelanaTheme.colors.ink)
        }
    }
}

@Composable
private fun KeptPanel() {
    val colors = KelanaTheme.colors
    Panel(
        title = stringResource(R.string.hapus_akun_kept_header),
        titleColor = colors.muted,
        background = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        PanelLine(R.drawable.akun_hapus_ic_info, stringResource(R.string.hapus_akun_reports), colors.muted)
    }
}

@Composable
private fun Panel(
    title: String,
    titleColor: Color,
    background: Color,
    border: BorderStroke?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, PanelShape) else Modifier)
            .padding(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = titleColor,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun PanelLine(@DrawableRes icon: Int, text: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = color,
        )
    }
}

/**
 * "Unduh dataku dulu": fetches the user's own data from the server and lets them save it as a JSON file. Below the
 * link, a status line says when the file was saved or why it was not (the link then retries).
 */
@Composable
private fun DownloadLink(state: HapusAkunUiState, onExport: () -> Unit) {
    val colors = KelanaTheme.colors
    val label = stringResource(
        when (state.export) {
            DataExportState.Exporting -> R.string.hapus_akun_download_preparing
            DataExportState.Error -> R.string.hapus_akun_download_retry
            DataExportState.Idle, DataExportState.Saved -> R.string.hapus_akun_download
        },
    )
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                // Dimmed only while the account is being deleted; while exporting the label says what happens.
                .alpha(if (state.isDeleting) 0.5f else 1f)
                .clickable(enabled = state.canExport, role = Role.Button, onClick = onExport),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.akun_hapus_ic_unduh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = colors.primary)
        }
        val status = when (state.export) {
            DataExportState.Saved -> stringResource(R.string.hapus_akun_download_saved) to colors.muted
            DataExportState.Error -> stringResource((state.exportError ?: DataExportError.Failed).messageRes()) to DangerRed
            else -> null
        }
        if (status != null) {
            Text(
                text = status.first,
                style = MaterialTheme.typography.bodySmall,
                color = status.second,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ConfirmField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val colors = KelanaTheme.colors
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.hapus_akun_confirm_label, ACCOUNT_DELETION_CONFIRM_WORD)
    val textStyle = TextStyle(
        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = colors.ink,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onDone()
        }),
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(KelanaShapes.input)
                    .background(colors.surface)
                    .border(1.dp, colors.outline, KelanaShapes.input)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = ACCOUNT_DELETION_CONFIRM_WORD, style = textStyle, color = colors.iconMuted)
                }
                inner()
            }
        },
    )
}

/** 48 dp button with radius 12 (the artifact's sheet buttons are not pills). */
@Composable
private fun SheetButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke?,
    modifier: Modifier = Modifier,
    shape: Shape = KelanaShapes.input,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(containerColor)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = contentColor,
            modifier = Modifier.padding(horizontal = KelanaSpacing.sm),
        )
    }
}

private fun DataExportError.messageRes(): Int = when (this) {
    DataExportError.Network -> R.string.hapus_akun_download_error_network
    DataExportError.Failed -> R.string.hapus_akun_download_error_failed
    DataExportError.NotConfigured -> R.string.hapus_akun_download_error_not_configured
    DataExportError.NotSignedIn -> R.string.hapus_akun_download_error_not_signed_in
    DataExportError.SaveFailed -> R.string.hapus_akun_download_error_save
}

private const val EXPORT_MIME_TYPE = "application/json"

private fun HapusAkunError.messageRes(): Int = when (this) {
    HapusAkunError.Network -> R.string.hapus_akun_error_network
    HapusAkunError.Failed -> R.string.hapus_akun_error_failed
    HapusAkunError.NotConfigured -> R.string.hapus_akun_error_not_configured
    HapusAkunError.NotSignedIn -> R.string.hapus_akun_error_not_signed_in
}

private val PreviewSummary = AccountDeletionSummary(stampCount = 3, postcardCount = 2, videoCount = 1)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunEmptyPreview() {
    KelanaTheme { HapusAkunContent(HapusAkunUiState(summary = PreviewSummary), {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunNoCountsPreview() {
    KelanaTheme { HapusAkunContent(HapusAkunUiState(), {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunConfirmedPreview() {
    KelanaTheme {
        HapusAkunContent(HapusAkunUiState(summary = PreviewSummary, confirmation = "HAPUS"), {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunDeletingPreview() {
    KelanaTheme {
        HapusAkunContent(
            HapusAkunUiState(summary = PreviewSummary, confirmation = "HAPUS", isDeleting = true),
            {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunErrorPreview() {
    KelanaTheme {
        HapusAkunContent(
            HapusAkunUiState(summary = PreviewSummary, confirmation = "HAPUS", error = HapusAkunError.Network),
            {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunExportingPreview() {
    KelanaTheme {
        HapusAkunContent(HapusAkunUiState(summary = PreviewSummary, export = DataExportState.Exporting), {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunExportSavedPreview() {
    KelanaTheme {
        HapusAkunContent(HapusAkunUiState(summary = PreviewSummary, export = DataExportState.Saved), {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HapusAkunExportErrorPreview() {
    KelanaTheme {
        HapusAkunContent(
            HapusAkunUiState(
                summary = PreviewSummary,
                export = DataExportState.Error,
                exportError = DataExportError.Network,
            ),
            {}, {}, {},
        )
    }
}

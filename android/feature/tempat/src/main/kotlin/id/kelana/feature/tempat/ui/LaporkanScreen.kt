package id.kelana.feature.tempat.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.BannerTone
import id.kelana.core.designsystem.component.InfoBanner
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.ReportReason
import id.kelana.core.navigation.LaporkanRoute
import id.kelana.core.navigation.MasukRoute
import id.kelana.feature.tempat.R

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun LaporkanScreen(
    route: LaporkanRoute,
    navigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: LaporkanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Success is shown by state (LaporTerkirimContent), nothing to do here.
                LaporkanEvent.Sent -> Unit
                // A guest is sent to Masuk by route object (the login hook's behaviour).
                LaporkanEvent.NeedsLogin -> navigate(MasukRoute)
            }
        }
    }
    if (state.sent) {
        LaporTerkirimContent(placeName = state.placeName, onBack = onBack)
    } else {
        LaporkanContent(
            state = state,
            onBack = onBack,
            onReason = viewModel::selectReason,
            onDetails = viewModel::setDetails,
            onSubmit = viewModel::submit,
        )
    }
}

/** Report form (LaporIlustratif.dc.html, screen 2.7). */
@Composable
internal fun LaporkanContent(
    state: LaporkanUiState,
    onBack: () -> Unit,
    onReason: (ReportReason) -> Unit,
    onDetails: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize().imePadding()) {
        // Top bar: round back button and "Laporkan" 17/800.
        Row(
            Modifier.fillMaxWidth().padding(start = KelanaSpacing.sm, end = KelanaSpacing.md, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.tempat_ic_chevron_back),
                    contentDescription = stringResource(R.string.tempat_back),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.laporkan_title),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = colors.ink,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.laporkan_question),
                modifier = Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 10.dp),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
                color = colors.ink,
            )
            if (state.placeName.isNotEmpty()) {
                Text(
                    text = state.placeName,
                    modifier = Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            Column(
                Modifier
                    .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.gutter)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LaporkanReasons.forEach { reason ->
                    ReasonCard(reason = reason, selected = state.reason == reason, onSelect = { onReason(reason) })
                }
            }
            // Details
            Column(Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.gutter)) {
                Text(
                    text = stringResource(R.string.laporkan_details_label),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.ink,
                )
                DetailsField(value = state.details, onValueChange = onDetails)
            }
            // PROVISIONAL: visual-only, no photo picker is wired. The dashed colour is the closest token (#B9C4DA in the artifact).
            AddPhotoButton(onClick = {})
            if (state.failed) {
                InfoBanner(
                    text = stringResource(R.string.laporkan_error),
                    tone = BannerTone.Warning,
                    modifier = Modifier.padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.md),
                )
            }
            Spacer(Modifier.height(KelanaSpacing.gutter))
        }
        Column(Modifier.background(colors.surface)) {
            HorizontalDivider(color = colors.outline)
            KelanaButton(
                text = stringResource(R.string.laporkan_submit),
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.md, bottom = 26.dp),
            )
        }
    }
}

/** One reason as a selectable card: icon, title, subtitle, radio. Selected: orange 2.5 dp border on peach. */
@Composable
private fun ReasonCard(reason: ReportReason, selected: Boolean, onSelect: () -> Unit) {
    val colors = KelanaTheme.colors
    val shape = KelanaShapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.peach else colors.surface)
            .border(if (selected) 2.5.dp else 1.5.dp, if (selected) colors.accentOrange else colors.outline, shape)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(KelanaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        Image(painterResource(reason.iconRes()), contentDescription = null, Modifier.size(30.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(reason.labelRes()),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.ink,
            )
            Text(
                text = stringResource(reason.subtitleRes()),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                color = colors.muted,
            )
        }
        // The card is the touch target; the radio itself carries no click.
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.size(22.dp),
            colors = RadioButtonDefaults.colors(selectedColor = colors.accentOrange, unselectedColor = colors.muted),
        )
    }
}

@Composable
private fun DetailsField(value: String, onValueChange: (String) -> Unit) {
    val colors = KelanaTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .padding(top = KelanaSpacing.sm)
            .fillMaxWidth()
            .height(92.dp)
            .clip(KelanaShapes.input)
            .background(colors.surface)
            .border(1.5.dp, colors.outline, KelanaShapes.input)
            .padding(KelanaSpacing.md),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.laporkan_details_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    val colors = KelanaTheme.colors
    val dash = colors.divider
    Row(
        Modifier
            .padding(start = KelanaSpacing.gutterWide, top = 10.dp)
            .height(KelanaSizes.inputHeight)
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clip(KelanaShapes.button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(painterResource(R.drawable.tempat_ic_media), contentDescription = null, Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.laporkan_add_photo),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
        )
    }
}

/** Report sent (LaporTerkirimIlustratif.dc.html, screen 2.8): green illustration, thanks, back to the place. */
@Composable
internal fun LaporTerkirimContent(placeName: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .padding(top = 60.dp, start = KelanaSpacing.gutter, end = KelanaSpacing.gutter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(KelanaShapes.imagePanel)
                    .background(colors.successSurface),
            ) {
                Image(
                    painter = painterResource(R.drawable.tempat_sent_art),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = stringResource(R.string.laporkan_sent_title),
                modifier = Modifier.fillMaxWidth().padding(start = KelanaSpacing.gutterText, end = KelanaSpacing.gutterText, top = 28.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.laporkan_sent_body),
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 10.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        val backLabel = if (placeName.isNotEmpty()) {
            stringResource(R.string.laporkan_sent_back, placeName)
        } else {
            stringResource(R.string.tempat_back)
        }
        KelanaButton(
            text = backLabel,
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = KelanaSpacing.md, bottom = 26.dp),
        )
    }
}

@StringRes
private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.HoursOrPriceChanged -> R.string.laporkan_reason_hours_or_price
    ReportReason.PlaceClosed -> R.string.laporkan_reason_closed
    ReportReason.CeremonyShownWithoutPermission -> R.string.laporkan_reason_ceremony
    ReportReason.InappropriateMedia -> R.string.laporkan_reason_inappropriate
    ReportReason.Other -> R.string.laporkan_reason_inappropriate
}

@StringRes
private fun ReportReason.subtitleRes(): Int = when (this) {
    ReportReason.HoursOrPriceChanged -> R.string.laporkan_reason_hours_or_price_sub
    ReportReason.PlaceClosed -> R.string.laporkan_reason_closed_sub
    ReportReason.CeremonyShownWithoutPermission -> R.string.laporkan_reason_ceremony_sub
    ReportReason.InappropriateMedia -> R.string.laporkan_reason_inappropriate_sub
    ReportReason.Other -> R.string.laporkan_reason_inappropriate_sub
}

@DrawableRes
private fun ReportReason.iconRes(): Int = when (this) {
    ReportReason.HoursOrPriceChanged -> R.drawable.tempat_ic_clock
    ReportReason.PlaceClosed -> R.drawable.tempat_ic_report
    ReportReason.CeremonyShownWithoutPermission -> R.drawable.tempat_ic_ceremony
    ReportReason.InappropriateMedia -> R.drawable.tempat_ic_media
    ReportReason.Other -> R.drawable.tempat_ic_media
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun LaporkanEmptyPreview() {
    KelanaTheme { LaporkanContent(LaporkanUiState(placeName = "Museum Sonobudoyo"), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun LaporkanSelectedPreview() {
    KelanaTheme {
        LaporkanContent(
            LaporkanUiState(reason = ReportReason.HoursOrPriceChanged, details = "Di loket tertulis buka pukul 09.00", placeName = "Museum Sonobudoyo"),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun LaporkanFailedPreview() {
    KelanaTheme {
        LaporkanContent(
            LaporkanUiState(reason = ReportReason.PlaceClosed, failed = true, placeName = "Museum Sonobudoyo"),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LaporTerkirimPreview() {
    KelanaTheme { LaporTerkirimContent(placeName = "Museum Sonobudoyo", onBack = {}) }
}

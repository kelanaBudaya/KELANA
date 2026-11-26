package id.kelana.feature.akun.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.Caption
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSizes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.SessionUser
import id.kelana.core.ui.LanguageSheet
import id.kelana.core.ui.flagRes
import id.kelana.core.ui.labelRes
import id.kelana.core.ui.map.MapCreditDialog
import id.kelana.feature.akun.R

@Composable
internal fun ProfilScreen(
    onBack: () -> Unit,
    onPaspor: () -> Unit,
    onDaerah: () -> Unit,
    onMasuk: () -> Unit,
    onSignedOut: () -> Unit,
    onNotifikasi: () -> Unit = {},
    onHapusAkun: () -> Unit = {},
    viewModel: ProfilViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProfilEvent.SignedOut -> onSignedOut()
            }
        }
    }
    ProfilContent(
        state = state,
        onBack = onBack,
        onPaspor = onPaspor,
        onDaerah = onDaerah,
        onMasuk = onMasuk,
        onSignOut = viewModel::onSignOut,
        onNotifikasi = onNotifikasi,
        onLanguage = viewModel::onLanguageSelected,
        onHapusAkun = onHapusAkun,
    )
}

/**
 * Artifact 8.2 (ProfilIlustratif.dc.html). The stats card (Cap, Postcard, Disimpan) and "Berkelana sejak" are not shown:
 * the artifact marks them `[ISI]` and no real data exists yet. Rows whose screens are not built are inert.
 */
@Composable
internal fun ProfilContent(
    state: ProfilUiState,
    onBack: () -> Unit,
    onPaspor: () -> Unit,
    onDaerah: () -> Unit,
    onMasuk: () -> Unit,
    onSignOut: () -> Unit,
    onNotifikasi: () -> Unit = {},
    onHapusAkun: () -> Unit = {},
    onLanguage: (AppLanguage) -> Unit = {},
) {
    val colors = KelanaTheme.colors
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    if (showLanguageSheet) {
        LanguageSheet(
            selected = state.language,
            onSelect = {
                onLanguage(it)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfilHeader(state = state, onBack = onBack)
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KelanaSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
            state.isGuest -> GuestBody(
                language = state.language,
                onMasuk = onMasuk,
                onLanguage = { showLanguageSheet = true },
            )
            else -> SignedInBody(
                state = state,
                onPaspor = onPaspor,
                onDaerah = onDaerah,
                onSignOut = onSignOut,
                onNotifikasi = onNotifikasi,
                onHapusAkun = onHapusAkun,
                onLanguage = { showLanguageSheet = true },
            )
        }
    }
}

@Composable
private fun ProfilHeader(state: ProfilUiState, onBack: () -> Unit) {
    val colors = KelanaTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(R.drawable.akun_profil_wave),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
        )
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = KelanaSpacing.gutterWide, end = KelanaSpacing.gutterWide, top = 14.dp, bottom = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(KelanaSizes.backButton)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.akun_ic_back),
                    contentDescription = stringResource(R.string.profil_back),
                    modifier = Modifier.size(22.dp),
                )
            }
            if (state.user != null) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(colors.stampYellow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.initials,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                            color = colors.navy,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        state.name?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                                color = colors.white,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        state.email?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.skyText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestBody(language: AppLanguage, onMasuk: () -> Unit, onLanguage: () -> Unit) {
    Column(
        modifier = Modifier.padding(KelanaSpacing.gutterWide).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(KelanaSpacing.lg),
    ) {
        Text(
            text = stringResource(R.string.masuk_title),
            style = MaterialTheme.typography.titleLarge,
            color = KelanaTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        KelanaButton(
            text = stringResource(R.string.profil_guest_masuk),
            onClick = onMasuk,
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            SectionLabel(R.string.profil_section_pengaturan, topPadding = 0.dp)
            LanguageRow(language, onLanguage)
            MapCreditRow()
        }
    }
}

@Composable
private fun SignedInBody(
    state: ProfilUiState,
    onPaspor: () -> Unit,
    onDaerah: () -> Unit,
    onSignOut: () -> Unit,
    onNotifikasi: () -> Unit,
    onHapusAkun: () -> Unit,
    onLanguage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = KelanaSpacing.gutterWide)
            .padding(top = 18.dp),
    ) {
        SectionLabel(R.string.profil_section_perjalanan, topPadding = 0.dp)
        // Paspor is the way into the passport from here now that the stats card is hidden.
        ProfilRow(R.drawable.akun_ic_cap, R.string.profil_row_paspor, subtitle = null, onClick = onPaspor)
        ProfilRow(
            R.drawable.akun_ic_daerah,
            R.string.profil_row_daerah,
            R.string.profil_row_daerah_value,
            onClick = onDaerah,
        )
        // Not built yet: rendered like the artifact but inert.
        ProfilRow(R.drawable.akun_ic_kebutuhan, R.string.profil_row_kebutuhan, R.string.profil_row_kebutuhan_sub)
        ProfilRow(
            R.drawable.akun_ic_notifikasi,
            R.string.profil_row_notifikasi,
            R.string.profil_row_notifikasi_sub,
            onClick = onNotifikasi,
        )

        SectionLabel(R.string.profil_section_pengaturan, topPadding = 16.dp)
        LanguageRow(state.language, onLanguage)
        MapCreditRow()

        SectionLabel(R.string.profil_section_bantuan, topPadding = 16.dp)
        ProfilRow(R.drawable.akun_ic_cara, R.string.profil_row_cara, R.string.profil_row_cara_sub)
        ProfilRow(R.drawable.akun_ic_laporan, R.string.profil_row_laporan, subtitle = null)
        ProfilRow(
            R.drawable.akun_ic_close,
            R.string.profil_row_hapus,
            R.string.profil_row_hapus_sub,
            onClick = onHapusAkun,
            destructive = true,
        )

        KelanaButton(
            text = stringResource(R.string.profil_keluar),
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KelanaSpacing.lg),
            variant = KelanaButtonVariant.Outline,
            enabled = !state.isSigningOut,
        )
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(KelanaSpacing.lg),
        )
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int, topPadding: Dp) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
        color = KelanaTheme.colors.muted,
        modifier = Modifier.padding(top = topPadding, bottom = 6.dp),
    )
}

/** Pengaturan > Bahasa: shows the current choice and opens the language sheet. */
@Composable
private fun LanguageRow(language: AppLanguage, onClick: () -> Unit) {
    ProfilRow(language.flagRes(), R.string.profil_row_bahasa, language.labelRes(), onClick = onClick)
}

/** Pengaturan > Tentang peta: the map credit the licences require, shown in a dialog. */
@Composable
private fun MapCreditRow() {
    var open by remember { mutableStateOf(false) }
    ProfilRow(R.drawable.akun_ic_daerah, R.string.profil_row_peta, R.string.profil_row_peta_sub, onClick = { open = true })
    if (open) MapCreditDialog(onDismiss = { open = false })
}

/** One list row (artifact: 60 dp min, 40 dp warm icon tile, title 15/700, subtitle 12, chevron). [onClick] null = inert. */
@Composable
private fun ProfilRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int? = null,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
) {
    val colors = KelanaTheme.colors
    val enabled = onClick != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier else Modifier.alpha(0.5f).semantics { disabled() }),
    ) {
        HorizontalDivider(color = colors.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = { onClick?.invoke() })
                .padding(horizontal = KelanaSpacing.xs, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(KelanaShapes.input)
                    .background(colors.backgroundWarm),
                contentAlignment = Alignment.Center,
            ) {
                Image(painter = painterResource(icon), contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = if (destructive) colors.danger else colors.ink,
                )
                subtitle?.let {
                    Text(text = stringResource(it), style = Caption, color = colors.muted)
                }
            }
            Image(painter = painterResource(R.drawable.akun_ic_chevron), contentDescription = null)
        }
    }
}

private val PreviewUser = SessionUser(id = "u1", email = "nama@contoh.id", displayName = "Nama Contoh")

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilSignedInPreview() {
    KelanaTheme { ProfilContent(ProfilUiState(isLoading = false, user = PreviewUser), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilEmailOnlyPreview() {
    KelanaTheme {
        ProfilContent(
            ProfilUiState(isLoading = false, user = SessionUser("u2", "tamu@contoh.id", null)),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilSigningOutPreview() {
    KelanaTheme {
        ProfilContent(ProfilUiState(isLoading = false, user = PreviewUser, isSigningOut = true), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilGuestPreview() {
    KelanaTheme { ProfilContent(ProfilUiState(isLoading = false), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilLoadingPreview() {
    KelanaTheme { ProfilContent(ProfilUiState(), {}, {}, {}, {}, {}) }
}

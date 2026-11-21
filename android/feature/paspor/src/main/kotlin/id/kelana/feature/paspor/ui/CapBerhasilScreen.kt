package id.kelana.feature.paspor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.theme.FrauncesFontFamily
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PostcardRoute
import id.kelana.feature.paspor.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "20.11.26" for the seal. */
private fun formatSealDate(instant: Instant): String =
    DateTimeFormatter.ofPattern("dd.MM.yy", IdLocale).withZone(ZoneId.systemDefault()).format(instant)

/**
 * Cap berhasil (Figma 6.3). Reached only after the server confirmed the stamp.
 * "Intip paspormu" replaces this screen with the passport; back returns to where Ambil cap started.
 */
@Composable
internal fun CapBerhasilScreen(
    navigate: (Any) -> Unit,
    onReplace: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CapBerhasilViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    CapBerhasilContent(
        state = state,
        onPostcard = { navigate(PostcardRoute) },
        onPassport = { onReplace(PasporRoute) },
        modifier = modifier,
    )
}

@Composable
internal fun CapBerhasilContent(
    state: CapBerhasilUiState,
    onPostcard: () -> Unit,
    onPassport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val place = state.place
    Box(modifier.fillMaxSize().background(colors.stampBrown)) {
        Image(
            painter = painterResource(R.drawable.paspor_bg_berhasil_wave),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(36.dp))
                Scene(390f, 360f) { s ->
                    SceneBackground(R.drawable.paspor_bg_berhasil)
                    ScenePiece(s, R.drawable.paspor_art_berhasil_1, 40f, 40f, 48f, -16f)
                    ScenePiece(s, R.drawable.paspor_art_berhasil_2, 306f, 60f, 46f, 12f)
                    ScenePiece(s, R.drawable.paspor_art_postcard, 320f, 270f, 40f, 10f)
                    ScenePiece(s, R.drawable.paspor_art_berhasil_3, 26f, 250f, 56f, -8f)
                    if (place != null) {
                        CapSticker(
                            label = place.name.uppercase(IdLocale),
                            code = place.capCode,
                            icon = place.category.stampIcon(),
                            tone = StickerTone.Blue,
                            width = s.dp(210f),
                            height = s.dp(152f),
                            iconSize = 46.dp,
                            modifier = Modifier.offset(s.dp(90f), s.dp(80f)).rotate(-6f),
                        )
                    }
                    CapSeal(
                        top = stringResource(R.string.paspor_seal_visited),
                        bottom = state.stampedAt?.let(::formatSealDate).orEmpty(),
                        center = state.regionCode.orEmpty(),
                        size = s.dp(96f),
                        modifier = Modifier.offset(s.dp(232f), s.dp(188f)),
                        rotation = 12f,
                    )
                }
                Column(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = kicker(state),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                        color = colors.stampYellow,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (place != null) {
                            stringResource(R.string.paspor_berhasil_title, place.name)
                        } else {
                            stringResource(R.string.paspor_berhasil_title_plain)
                        },
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.white,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.paspor_berhasil_body),
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = colors.peach,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp).navigationBarsPadding(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                PasporButton(
                    stringResource(R.string.paspor_berhasil_postcard),
                    onPostcard,
                    Modifier.fillMaxWidth(),
                    style = PasporButtonStyle.WhiteOnBrown,
                    icon = R.drawable.paspor_art_postcard,
                )
                PasporButton(
                    stringResource(R.string.paspor_berhasil_passport),
                    onPassport,
                    Modifier.fillMaxWidth(),
                    style = PasporButtonStyle.OutlinedOnBrown,
                    height = 48.dp,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "CAP KE-3 · TINGGAL 7 LAGI DI JOGJA", with the real numbers; no numbers when they are not known. */
@Composable
private fun kicker(state: CapBerhasilUiState): String = when {
    state.place == null || state.collected <= 0 -> stringResource(R.string.paspor_berhasil_kicker_plain)
    state.remaining == 0 -> stringResource(R.string.paspor_berhasil_kicker_complete, state.collected)
    else -> stringResource(R.string.paspor_berhasil_kicker, state.collected, state.remaining)
}

@Preview(widthDp = 390, heightDp = 844, name = "6.3 Cap berhasil")
@Composable
private fun PreviewCapBerhasil() {
    KelanaTheme {
        CapBerhasilContent(
            state = CapBerhasilUiState(
                isLoading = false,
                place = Place(
                    "museum-sonobudoyo", "museum-sonobudoyo", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02",
                    0.0, 0.0, "", "", "sonobudoyo",
                ),
                collected = 3,
                total = 10,
                stampedAt = Instant.parse("2026-11-20T03:00:00Z"),
                regionCode = "DIY",
            ),
            onPostcard = {},
            onPassport = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "6.3 Cap berhasil, place unknown")
@Composable
private fun PreviewCapBerhasilPlain() {
    KelanaTheme { CapBerhasilContent(CapBerhasilUiState(isLoading = false), onPostcard = {}, onPassport = {}) }
}

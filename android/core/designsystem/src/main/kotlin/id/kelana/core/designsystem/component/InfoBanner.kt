package id.kelana.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme

enum class BannerTone { Info, Success, Warning }

/** Background and text colour of a tone. Colours come from the Figma tokens; the Info pairing is PROVISIONAL. */
internal fun bannerColors(tone: BannerTone, colors: KelanaColors): Pair<Color, Color> = when (tone) {
    BannerTone.Info -> colors.skyText to colors.navy
    BannerTone.Success -> colors.successSurface to colors.successTextDeep
    BannerTone.Warning -> colors.creamYellow to colors.warnTextDeep
}

/** Inline notice (frames use cream "sedang dicek" notes, green "Kamu ada di area"). Padding 12 dp, radius 12 dp PROVISIONAL. */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    title: String? = null,
) {
    val (background, content) = bannerColors(tone, KelanaTheme.colors)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, KelanaShapes.button)
            .padding(KelanaSpacing.md),
    ) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = content)
        }
        Text(
            text = text,
            modifier = Modifier.padding(top = if (title != null) 4.dp else 0.dp),
            style = MaterialTheme.typography.bodySmall,
            color = content,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun InfoBannerPreview() {
    KelanaTheme {
        Column(Modifier.padding(KelanaSpacing.gutter)) {
            BannerTone.entries.forEach {
                InfoBanner(
                    text = stringResource(R.string.ds_preview_card_body),
                    tone = it,
                    title = stringResource(R.string.ds_preview_card_title),
                    modifier = Modifier.padding(bottom = KelanaSpacing.sm),
                )
            }
        }
    }
}

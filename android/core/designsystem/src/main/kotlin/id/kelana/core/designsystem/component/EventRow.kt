package id.kelana.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.R
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import java.time.LocalDate

/**
 * One performance in the agenda or in search results (frame 02.01, "Wayang kulit" row).
 * [place] is the venue, [whenText] the already formatted time such as "Kamis 09.00-11.00".
 * Sizes PROVISIONAL, estimated from frame 02.01: card radius 16 dp, 12 dp padding, 56 dp leading tile.
 * [badge] sits under the time, for example a [VerifiedBadge].
 */
@Composable
fun EventRow(
    title: String,
    place: String,
    whenText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    leading: @Composable BoxScope.() -> Unit = {},
) {
    val colors = KelanaTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = KelanaShapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(KelanaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(KelanaShapes.button)
                    .background(colors.backgroundWarm),
                content = leading,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KelanaSpacing.md),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = place,
                    modifier = Modifier.padding(top = KelanaSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) Box(Modifier.padding(top = KelanaSpacing.sm)) { badge() }
            }
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun EventRowPreview() {
    KelanaTheme {
        EventRow(
            title = stringResource(R.string.ds_preview_event_title),
            place = stringResource(R.string.ds_preview_card_title),
            whenText = stringResource(R.string.ds_preview_event_when),
            onClick = {},
            modifier = Modifier.padding(KelanaSpacing.gutter),
            badge = { VerifiedBadge(checkedAt = LocalDate.of(2026, 11, 20)) },
        )
    }
}

package id.kelana.feature.peta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.component.KelanaButtonVariant
import id.kelana.core.designsystem.theme.Caption
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.formatTimeRange
import java.time.LocalDate

/**
 * Card of the selected place: name, today's hours and price (official facts, or the
 * "sedang dicek" wording), at most one status badge, "Lihat detail" and the optional maps-app link.
 */
@Composable
internal fun PlaceCard(
    place: PetaPlace,
    today: LocalDate,
    onOpenDetail: () -> Unit,
    onOpenInMaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KelanaTheme.colors
    val facts = cardFacts(place.place, today)
    val hoursText = when (val h = facts.hours) {
        is HoursLine.Open ->
            if (h.closes != null) formatTimeRange(h.opens, h.closes)
            else stringResource(R.string.peta_card_hours_from, formatTimeRange(h.opens, null))
        HoursLine.Closed -> stringResource(R.string.peta_card_hours_closed)
        HoursLine.Checking -> stringResource(R.string.peta_card_hours_checking)
    }
    val priceText = when (val p = facts.price) {
        PriceLine.Free -> stringResource(R.string.peta_card_price_free)
        is PriceLine.Paid ->
            if (p.isFrom) stringResource(R.string.peta_card_price_from, formatRupiah(p.fromIdr)) else formatRupiah(p.fromIdr)
        PriceLine.Checking -> stringResource(R.string.peta_card_price_checking)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = KelanaShapes.card,
        color = colors.white,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(place.place.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(
                text = stringResource(R.string.peta_card_line, hoursText, priceText),
                style = Caption,
                color = colors.muted,
            )
            when (badgeFor(place.showToday, place.openToday)) {
                PetaBadge.ShowTonight -> StatusBadge(stringResource(R.string.peta_badge_show))
                PetaBadge.OpenToday -> StatusBadge(stringResource(R.string.peta_badge_open))
                null -> Unit
            }
            KelanaButton(
                text = stringResource(R.string.peta_open_detail),
                onClick = onOpenDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            KelanaButton(
                text = stringResource(R.string.peta_open_maps),
                onClick = onOpenInMaps,
                modifier = Modifier.fillMaxWidth(),
                variant = KelanaButtonVariant.Text,
            )
        }
    }
}

/** Green badge, radius 4, 12/600 (KatPeta.dc.html card badge). */
@Composable
private fun StatusBadge(text: String, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.successText,
        modifier = modifier
            .padding(top = 4.dp)
            .background(colors.successSurface, KelanaShapes.badge)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

package id.kelana.feature.tempat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.theme.KelanaShapes
import id.kelana.core.designsystem.theme.KelanaSpacing
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.TicketPrice
import id.kelana.core.ui.formatRupiah
import id.kelana.feature.tempat.R

/** Title of the "Info resmi" card. Check dates and sources are not displayed. */
@Composable
internal fun InfoHeader() {
    val colors = KelanaTheme.colors
    Row(Modifier.fillMaxWidth().padding(bottom = KelanaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.tempat_ic_info), contentDescription = null, Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.tempat_info_title),
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
    }
}

/**
 * Admission ("Tiket masuk") prices. A price is shown only for a checked fact; price 0 reads "Gratis" and only
 * when the data says 0 explicitly. One price is a single compact row; two or more are a list, label left and
 * price right-aligned.
 */
@Composable
internal fun TicketsRow(tickets: List<OfficialFact<TicketPrice>>) {
    val colors = KelanaTheme.colors
    val label = stringResource(R.string.tempat_ticket_label)
    val shown = tickets.filter { it.checkedAt != null && it.value.priceIdr != null }
    if (shown.isEmpty()) {
        PendingRow(R.drawable.tempat_ic_ticket, colors.creamYellow, label, stringResource(R.string.tempat_ticket_pending))
        return
    }
    val free = stringResource(R.string.tempat_ticket_free)
    fun price(fact: OfficialFact<TicketPrice>): String = fact.value.priceIdr!!.let { if (it == 0L) free else formatRupiah(it) }

    if (tickets.size == 1) {
        val fact = shown.single()
        ValueRow(
            R.drawable.tempat_ic_ticket,
            colors.creamYellow,
            label,
            stringResource(R.string.tempat_ticket_item, fact.value.label, price(fact)),
            pending = false,
        )
        return
    }
    RowDivider()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md),
    ) {
        Box(
            Modifier.size(RowTile).clip(KelanaShapes.input).background(colors.creamYellow),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.tempat_ic_ticket), contentDescription = null, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KelanaSpacing.xs)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            tickets.forEach { fact ->
                val known = fact.checkedAt != null && fact.value.priceIdr != null
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KelanaSpacing.md)) {
                    Text(
                        text = fact.value.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.ink,
                    )
                    Text(
                        text = if (known) price(fact) else stringResource(R.string.tempat_ticket_pending),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (known) colors.ink else colors.warnText,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

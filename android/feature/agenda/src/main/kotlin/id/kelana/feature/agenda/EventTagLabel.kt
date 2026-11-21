package id.kelana.feature.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.kelana.core.model.EventTag

/** Chip label of a filter tag ("Tari", "Wayang", "Ramah anak"). */
@Composable
internal fun EventTag.label(): String = stringResource(
    when (this) {
        EventTag.Tari -> R.string.agenda_tag_tari
        EventTag.Wayang -> R.string.agenda_tag_wayang
        EventTag.RamahAnak -> R.string.agenda_tag_ramah_anak
    },
)

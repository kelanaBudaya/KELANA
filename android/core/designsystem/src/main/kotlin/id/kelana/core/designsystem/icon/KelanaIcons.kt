package id.kelana.core.designsystem.icon

import androidx.annotation.DrawableRes
import id.kelana.core.designsystem.R

/** Bottom bar icon pair: full colour when the tab is active, grey-tinted when inactive. */
data class TabIcon(
    @DrawableRes val active: Int,
    @DrawableRes val inactive: Int,
)

/** Icons converted from docs/assets (Discover, Jadwal, Tujuan, Kamera). */
object KelanaIcons {
    val Jelajah = TabIcon(R.drawable.ic_tab_jelajah_active, R.drawable.ic_tab_jelajah_inactive)
    val Agenda = TabIcon(R.drawable.ic_tab_agenda_active, R.drawable.ic_tab_agenda_inactive)
    val Rencana = TabIcon(R.drawable.ic_tab_rencana_active, R.drawable.ic_tab_rencana_inactive)
    val Cerita = TabIcon(R.drawable.ic_tab_cerita_active, R.drawable.ic_tab_cerita_inactive)
}

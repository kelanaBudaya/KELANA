package id.kelana.app.navigation

import androidx.annotation.StringRes
import id.kelana.app.R
import id.kelana.core.navigation.AgendaRoute
import id.kelana.core.navigation.CeritaRoute
import id.kelana.core.navigation.JelajahRoute
import id.kelana.core.navigation.RencanaRoute
import kotlin.reflect.KClass

/** The four bottom bar tabs, in display order. Paspor is not a tab. */
enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
) {
    Jelajah(JelajahRoute, JelajahRoute::class, R.string.tab_jelajah),
    Agenda(AgendaRoute, AgendaRoute::class, R.string.tab_agenda),
    Rencana(RencanaRoute, RencanaRoute::class, R.string.tab_rencana),
    Cerita(CeritaRoute, CeritaRoute::class, R.string.tab_cerita),
}

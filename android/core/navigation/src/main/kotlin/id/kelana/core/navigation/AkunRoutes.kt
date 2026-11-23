package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/*
 * Routes of the akun feature beyond Masuk and Profil (those are in Routes.kt).
 */

/** "Hapus akun KELANA?" confirmation sheet over Profil (HapusAkun.dc.html). Needs a signed-in user. */
@Serializable data object HapusAkunRoute

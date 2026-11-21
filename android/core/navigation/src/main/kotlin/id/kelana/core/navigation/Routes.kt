package id.kelana.core.navigation

import id.kelana.core.model.PlaceCategory
import kotlinx.serialization.Serializable

/*
 * Type-safe routes. Features navigate by route object only; :app registers every destination.
 */

// Bottom bar tabs, in display order.
@Serializable data object JelajahRoute
@Serializable data object AgendaRoute
@Serializable data object RencanaRoute
@Serializable data object CeritaRoute

// Onboarding (Figma 01.01 to 01.05): Intro(0) -> Intro(1) -> Intro(2) -> Intro(3) -> PilihDaerah -> Jelajah.
/**
 * Onboarding pager. [page] 0 is Mulai (Figma 01.01); pages 1 to 3 are Kenalan 1 to 3 (01.02 to 01.04).
 * Pilih daerah is its own route, [PilihDaerahRoute].
 */
@Serializable data class IntroRoute(val page: Int = 0)
@Serializable data object PilihDaerahRoute

// Jelajah flow.
@Serializable data object CariRoute

/** [category] is a [PlaceCategory.key] (a String keeps the nav argument simple). Use [KategoriRoute.of]. */
@Serializable data class KategoriRoute(val category: String) {
    val placeCategory: PlaceCategory? get() = PlaceCategory.fromKey(category)

    companion object {
        fun of(category: PlaceCategory) = KategoriRoute(category.key)
    }
}
@Serializable data object SemuaTempatRoute

// Tempat flow.
@Serializable data class DetailTempatRoute(val placeId: String)
@Serializable data class FotoResmiRoute(val placeId: String)
@Serializable data class LaporkanRoute(val placeId: String)

// Agenda flow.
@Serializable data class DetailPentasRoute(val eventId: String)

/**
 * Confirm sheet "Ke situs tiket resmi" (Figma 03.04). [url] is the official seller, [label] its name.
 * "Buka situs" opens a Custom Tab. :app registers the sheet from :core:ui.
 */
@Serializable data class OfficialSiteRoute(val url: String, val label: String)

// Destinations without a bottom bar.
@Serializable data object AkunRoute

/** Sign-in screen (Figma 08.01 Masuk). Use the requireLogin hook from :core:ui to send a guest here. */
@Serializable data object MasukRoute

// Other flows, reached from within the app.
@Serializable data object PetaRoute
@Serializable data object PasporRoute
@Serializable data object PostcardRoute
@Serializable data object NotifikasiRoute
@Serializable data object TiketRoute

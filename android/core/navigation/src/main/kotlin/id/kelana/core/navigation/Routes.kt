package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe routes. Features navigate by route object only; :app registers every destination.
 */

// Bottom bar tabs, in display order.
@Serializable data object JelajahRoute
@Serializable data object AgendaRoute
@Serializable data object RencanaRoute
@Serializable data object CeritaRoute

// Destinations without a bottom bar.
@Serializable data object OnboardingRoute
@Serializable data object AkunRoute

// Other flows, reached from within the app.
@Serializable data class TempatRoute(val placeId: String)
@Serializable data object PetaRoute
@Serializable data object PasporRoute
@Serializable data object PostcardRoute
@Serializable data object NotifikasiRoute
@Serializable data object TiketRoute

package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/*
 * Routes of the Rencana tab (the tab itself is [RencanaRoute] in Routes.kt). The six wizard steps live in one nested
 * graph ([SusunGraphRoute]) so they share one ViewModel. Step numbers are the artifact's (Susun 1/6 .. 6/6).
 */

/** Nested graph of the 6-step planner. Navigate to this to start the wizard. */
@Serializable data object SusunGraphRoute

/** 1/6 Kapan kamu berkelana? (SusunTanggal) */
@Serializable data object SusunTanggalRoute

/** 2/6 Kamu berkelana bersama siapa? (SusunRencana) */
@Serializable data object SusunTemanRoute

/** 3/6 Apa yang paling ingin kamu lihat? (SusunMinat) */
@Serializable data object SusunMinatRoute

/** 4/6 Jam berapa kamu mulai dan selesai? (SusunWaktu) */
@Serializable data object SusunWaktuRoute

/** 5/6 Kamu berkeliling naik apa? (SusunKendaraan) */
@Serializable data object SusunKendaraanRoute

/** 6/6 Ada kebutuhan khusus? (SusunKebutuhan) */
@Serializable data object SusunKebutuhanRoute

/** Loading while the plan is built (Menyusun). */
@Serializable data object MenyusunRoute

/** "Kapan kami ingatkan?" sheet for the show [eventId] (Pengingat). */
@Serializable data class PengingatRoute(val eventId: String)

/** A place in the plan of [date] (ISO yyyy-MM-dd) turned out closed (TempatTutup). */
@Serializable data class TempatTutupRoute(val placeId: String, val date: String)


/**
 * Route view of one day of the plan (PetaRute). [date] is ISO yyyy-MM-dd; null means today or the next planned day.
 */
@Serializable data class PetaRuteRoute(val date: String? = null)

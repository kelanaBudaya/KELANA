package id.kelana.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Row shapes of the Supabase tables the app reads (see supabase/contract/types.ts). Names follow the contract;
 * the mappers turn them into :core:model types. `places.location` is never selected, only `lat` and `lng`.
 */

@Serializable
data class RegionDto(
    val code: String,
    val name: String,
    val status: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class PlaceDto(
    val id: String,
    val slug: String,
    val name: String,
    val category: String,
    @SerialName("cap_code") val capCode: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null,
    val description: String? = null,
    @SerialName("cover_path") val coverPath: String? = null,
    @SerialName("passport_order") val passportOrder: Int? = null,
    /** Embedded `regions(code)`. */
    val regions: RegionCodeDto? = null,
)

@Serializable
data class RegionCodeDto(val code: String)

@Serializable
data class OpeningHoursDto(
    @SerialName("place_id") val placeId: String,
    val weekday: Int,
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("is_closed") val isClosed: Boolean = false,
    val note: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class ClosureDto(
    @SerialName("place_id") val placeId: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String,
    val reason: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class TicketPriceDto(
    @SerialName("place_id") val placeId: String,
    val label: String,
    @SerialName("price_idr") val priceIdr: Long? = null,
    @SerialName("seller_url") val sellerUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class PlaceNameDto(val name: String, val regions: RegionCodeDto? = null)

@Serializable
data class EventDto(
    val id: String,
    @SerialName("place_id") val placeId: String,
    val title: String,
    val description: String? = null,
    val category: String = "lainnya",
    val recurrence: String,
    @SerialName("event_date") val eventDate: String? = null,
    val weekday: Int? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("price_from_idr") val priceFromIdr: Long? = null,
    @SerialName("seller_url") val sellerUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
    val performers: List<String> = emptyList(),
    @SerialName("arrival_advice_min") val arrivalAdviceMin: Int? = null,
    @SerialName("arrival_advice_max") val arrivalAdviceMax: Int? = null,
    val tags: List<String> = emptyList(),
    /** Embedded `places(name,regions(code))`. */
    val places: PlaceNameDto? = null,
)

@Serializable
data class ReportInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("place_id") val placeId: String,
    val reason: String,
    val details: String? = null,
)

/** Everything one refresh of the places cache needs. All four lists come from the same fetch or none are used. */
data class PlacesPayload(
    val places: List<PlaceDto>,
    val hours: List<OpeningHoursDto>,
    val closures: List<ClosureDto>,
    val tickets: List<TicketPriceDto>,
)

data class EventsPayload(
    val events: List<EventDto>,
    val closures: List<ClosureDto>,
)

/** Thrown by the remote source when no Supabase url and anon key are in the build. */
class NotConfiguredException : RuntimeException("Supabase is not configured in this build")

/**
 * Network side of the content repositories. Tests replace it with a fake so unit tests never touch the network.
 * Row Level Security decides what comes back (guests only get published rows); callers must not re-filter.
 */
interface KelanaRemoteDataSource {
    suspend fun fetchRegions(): List<RegionDto>

    suspend fun fetchPlaces(): PlacesPayload

    suspend fun fetchEvents(): EventsPayload

    /** Inserts into `reports` as the logged-in user (the session token is attached by the client). */
    suspend fun insertReport(report: ReportInsertDto)
}

package id.kelana.core.data.remote

import id.kelana.core.data.auth.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads public content with the anon key (or the user's token when logged in) and writes reports.
 * Not exercised by unit tests; only the repositories above it are.
 */
@Singleton
class SupabaseRemoteDataSource @Inject constructor(
    private val provider: SupabaseClientProvider,
) : KelanaRemoteDataSource {

    private fun client() = provider.client ?: throw NotConfiguredException()

    override suspend fun fetchRegions(): List<RegionDto> =
        client().from("regions")
            .select(Columns.raw("code,name,status,sort_order")) {
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<RegionDto>()

    override suspend fun fetchPlaces(): PlacesPayload {
        val c = client()
        // `lat` and `lng` are generated columns; `location` (PostGIS) is deliberately not selected.
        val places = c.from("places")
            .select(Columns.raw("id,slug,name,category,cap_code,lat,lng,address,description,cover_path,passport_order,regions(code)")) {
                order("passport_order", Order.ASCENDING)
            }
            .decodeList<PlaceDto>()
        val hours = c.from("opening_hours")
            .select(Columns.raw("place_id,weekday,opens_at,closes_at,is_closed,note,source_url,checked_at"))
            .decodeList<OpeningHoursDto>()
        val closures = c.from("closures")
            .select(Columns.raw("place_id,starts_on,ends_on,reason,source_url,checked_at"))
            .decodeList<ClosureDto>()
        val tickets = c.from("ticket_prices")
            .select(Columns.raw("place_id,label,price_idr,seller_url,on_site_available,source_url,checked_at"))
            .decodeList<TicketPriceDto>()
        return PlacesPayload(places, hours, closures, tickets)
    }

    override suspend fun fetchEvents(): EventsPayload {
        val c = client()
        val events = c.from("events")
            .select(
                Columns.raw(
                    "id,place_id,title,description,category,recurrence,event_date,weekday,start_time,end_time," +
                        "price_from_idr,seller_url,on_site_available,source_url,checked_at,performers,arrival_advice_min," +
                        "arrival_advice_max,tags,places(name,regions(code))",
                ),
            )
            .decodeList<EventDto>()
        val closures = c.from("closures")
            .select(Columns.raw("place_id,starts_on,ends_on,reason,source_url,checked_at"))
            .decodeList<ClosureDto>()
        return EventsPayload(events, closures)
    }

    override suspend fun insertReport(report: ReportInsertDto) {
        client().from("reports").insert(report)
    }
}

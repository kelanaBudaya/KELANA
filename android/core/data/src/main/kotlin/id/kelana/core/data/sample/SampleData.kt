package id.kelana.core.data.sample

import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTag
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus
import id.kelana.core.model.TicketPrice
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * SAMPLE DATA ONLY. Hand-translated from supabase/seed.sql (10 places, hours, tickets, 5 events, 10 regions).
 * Nothing here is verified. Every fact uses checkedAt 2026-11-20 except where seed.sql leaves it null.
 * The pending ticket row of seed.sql (Prambanan "usulan baru") is left out on purpose: pending rows never reach clients.
 * Place and event ids are slugs here; the Supabase-backed repositories will use row uuids.
 */
internal object SampleData {
    val checkedAt: LocalDate = LocalDate.of(2026, 11, 20)

    private fun sourceFor(slug: String) = "https://example.org/sample/$slug"

    private fun hoursFor(
        slug: String,
        opens: LocalTime,
        closes: LocalTime,
        closedDay: DayOfWeek? = null,
        note: String? = null,
    ): List<OfficialFact<OpeningHours>> = DayOfWeek.entries.map { day ->
        val closed = day == closedDay
        OfficialFact(
            value = OpeningHours(
                weekday = day,
                isClosed = closed,
                opens = if (closed) null else opens,
                closes = if (closed) null else closes,
                note = note,
            ),
            sourceUrl = sourceFor(slug),
            checkedAt = checkedAt,
        )
    }

    private fun ticket(slug: String, label: String, price: Long, seller: String): OfficialFact<TicketPrice> = OfficialFact(
        value = TicketPrice(label, price, "https://example.org/sample/tiket/$seller"),
        sourceUrl = sourceFor(slug),
        checkedAt = checkedAt,
    )

    private fun place(
        slug: String,
        name: String,
        category: PlaceCategory,
        capCode: String,
        lat: Double,
        lng: Double,
        address: String,
        description: String,
        hours: List<OfficialFact<OpeningHours>>,
        tickets: List<OfficialFact<TicketPrice>>,
    ) = Place(
        id = slug, slug = slug, name = name, category = category, capCode = capCode,
        lat = lat, lng = lng, address = address, description = description,
        coverKey = slug, regionId = "DIY", hours = hours, tickets = tickets,
    )

    private val standardHours = { slug: String -> hoursFor(slug, LocalTime.of(9, 0), LocalTime.of(15, 0), DayOfWeek.MONDAY) }

    /** Passport order YK-01 .. YK-10. */
    val places: List<Place> = listOf(
        place(
            "keraton-yogyakarta", "Keraton Yogyakarta", PlaceCategory.Lainnya, "YK-01", -7.8053, 110.3642,
            "Jl. Rotowijayan Blok No. 1, Panembahan, Kraton",
            "Istana Kesultanan Yogyakarta dengan bangsal, museum, dan pentas budaya rutin.",
            standardHours("keraton-yogyakarta"), listOf(ticket("keraton-yogyakarta", "Dewasa", 25_000, "keraton")),
        ),
        place(
            "museum-sonobudoyo", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02", -7.8013, 110.3641,
            "Jl. Pangurakan No. 6, Ngupasan, Gondomanan",
            "Museum budaya Jawa: wayang, keris, topeng, dan batik.",
            hoursFor("museum-sonobudoyo", LocalTime.of(8, 0), LocalTime.of(21, 0), DayOfWeek.MONDAY),
            listOf(ticket("museum-sonobudoyo", "Dewasa", 10_000, "sonobudoyo"), ticket("museum-sonobudoyo", "Anak-anak", 5_000, "sonobudoyo")),
        ),
        place(
            "tamansari", "Tamansari", PlaceCategory.Lainnya, "YK-03", -7.8100, 110.3593,
            "Patehan, Kraton",
            "Bekas taman istirahat sultan dengan kolam, lorong, dan Sumur Gumuling.",
            standardHours("tamansari"), listOf(ticket("tamansari", "Dewasa", 15_000, "tamansari")),
        ),
        place(
            "museum-kereta-wahanarata", "Museum Kereta Wahanarata", PlaceCategory.Museum, "YK-04", -7.8036, 110.3567,
            "Jl. Rotowijayan No. 5, Panembahan, Kraton",
            "Koleksi kereta kencana Keraton Yogyakarta.",
            standardHours("museum-kereta-wahanarata"), listOf(ticket("museum-kereta-wahanarata", "Dewasa", 10_000, "kereta")),
        ),
        place(
            "museum-benteng-vredeburg", "Museum Benteng Vredeburg", PlaceCategory.Museum, "YK-05", -7.8002, 110.3664,
            "Jl. Margo Mulyo No. 6, Ngupasan, Gondomanan",
            "Benteng kolonial yang kini menjadi museum sejarah perjuangan.",
            standardHours("museum-benteng-vredeburg"), listOf(ticket("museum-benteng-vredeburg", "Dewasa", 5_000, "vredeburg")),
        ),
        place(
            "puro-pakualaman", "Puro Pakualaman", PlaceCategory.Lainnya, "YK-06", -7.8007, 110.3779,
            "Jl. Sultan Agung, Gunungketur, Pakualaman",
            "Istana Kadipaten Pakualaman dengan museum dan pendapa.",
            standardHours("puro-pakualaman"),
            // Price intentionally unknown: no check date, so the UI must show "Harga sedang dicek".
            listOf(OfficialFact(TicketPrice("Dewasa", null, null), sourceUrl = null, checkedAt = null)),
        ),
        place(
            "museum-batik-yogyakarta", "Museum Batik Yogyakarta", PlaceCategory.Museum, "YK-07", -7.7878, 110.3766,
            "Jl. Dr. Sutomo No. 13A, Bausasran, Danurejan",
            "Museum batik dengan koleksi kain dan alat membatik.",
            standardHours("museum-batik-yogyakarta"), listOf(ticket("museum-batik-yogyakarta", "Dewasa", 20_000, "batik")),
        ),
        place(
            "tugu-yogyakarta", "Tugu Yogyakarta", PlaceCategory.Lainnya, "YK-08", -7.7829, 110.3671,
            "Jl. Jenderal Sudirman, Gowongan, Jetis",
            "Tugu penanda sumbu filosofi Yogyakarta.",
            hoursFor("tugu-yogyakarta", LocalTime.of(0, 0), LocalTime.of(23, 59), note = "Ruang terbuka"),
            emptyList(),
        ),
        place(
            "kotagede", "Kotagede", PlaceCategory.Kerajinan, "YK-09", -7.8279, 110.3987,
            "Kotagede, Kota Yogyakarta",
            "Kawasan kota lama dengan kerajinan perak dan makam raja Mataram.",
            standardHours("kotagede"), emptyList(),
        ),
        place(
            "candi-prambanan", "Candi Prambanan", PlaceCategory.Candi, "YK-10", -7.7520, 110.4914,
            "Jl. Raya Solo - Yogyakarta km 16, Prambanan, Sleman",
            "Kompleks candi Hindu abad ke-9 dengan relief Ramayana.",
            hoursFor("candi-prambanan", LocalTime.of(6, 30), LocalTime.of(17, 30)),
            listOf(ticket("candi-prambanan", "Dewasa", 50_000, "prambanan"), ticket("candi-prambanan", "Anak-anak", 25_000, "prambanan")),
        ),
    )

    private fun event(
        id: String,
        placeSlug: String,
        placeName: String,
        title: String,
        description: String,
        category: PlaceCategory,
        weekday: DayOfWeek,
        start: LocalTime,
        end: LocalTime,
        priceFrom: Long?,
        sellerUrl: String?,
        tags: Set<EventTag> = emptySet(),
        performers: List<String> = emptyList(),
        arrivalAdviceMin: Int? = null,
        arrivalAdviceMax: Int? = null,
    ) = Event(
        id = id,
        placeId = placeSlug,
        placeName = placeName,
        title = title,
        description = description,
        category = category,
        schedule = OfficialFact(
            EventSchedule(Recurrence.Weekly(weekday), start, end),
            sourceUrl = "https://example.org/sample/events/$placeSlug",
            checkedAt = checkedAt,
        ),
        ticket = OfficialFact(
            EventTicket(priceFrom, sellerUrl),
            sourceUrl = "https://example.org/sample/events/$placeSlug",
            checkedAt = checkedAt,
        ),
        performers = performers,
        arrivalAdviceMin = arrivalAdviceMin,
        arrivalAdviceMax = arrivalAdviceMax,
        tags = tags,
    )

    /** weekday follows ISO 8601 as in seed.sql: 1 = Monday .. 7 = Sunday. */
    val events: List<Event> = listOf(
        event(
            "roro-jonggrang-jumat", "candi-prambanan", "Candi Prambanan", "Sendratari Roro Jonggrang",
            "Pentas sendratari kisah Roro Jonggrang.", PlaceCategory.Tari,
            DayOfWeek.FRIDAY, LocalTime.of(19, 30), LocalTime.of(20, 30), 150_000,
            "https://example.org/sample/tiket/roro-jonggrang",
            tags = setOf(EventTag.Tari),
            performers = listOf("Lebih dari 200 penari dan pemusik lokal"),
            arrivalAdviceMin = 30, arrivalAdviceMax = 45,
        ),
        event(
            "ramayana-selasa", "candi-prambanan", "Candi Prambanan", "Sendratari Ramayana",
            "Pentas sendratari Ramayana. Hari tayang contoh.", PlaceCategory.Tari,
            DayOfWeek.TUESDAY, LocalTime.of(19, 30), LocalTime.of(21, 0), 150_000,
            "https://example.org/sample/tiket/ramayana",
            tags = setOf(EventTag.Tari),
        ),
        event(
            "ramayana-kamis", "candi-prambanan", "Candi Prambanan", "Sendratari Ramayana",
            "Pentas sendratari Ramayana. Hari tayang contoh.", PlaceCategory.Tari,
            DayOfWeek.THURSDAY, LocalTime.of(19, 30), LocalTime.of(21, 0), 150_000,
            "https://example.org/sample/tiket/ramayana",
            tags = setOf(EventTag.Tari),
        ),
        event(
            "macapat-sabtu", "keraton-yogyakarta", "Keraton Yogyakarta", "Macapat, Bangsal Srimanganti",
            "Tembang macapat di Bangsal Srimanganti. Hari contoh.", PlaceCategory.Gamelan,
            DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null, null,
        ),
        event(
            "wayang-wong-minggu", "keraton-yogyakarta", "Keraton Yogyakarta", "Fragmen wayang wong, Bangsal Srimanganti",
            "Fragmen wayang wong di Bangsal Srimanganti. Hari contoh.", PlaceCategory.Tari,
            DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null, null,
            tags = setOf(EventTag.Wayang),
        ),
    )

    val regions: List<Region> = listOf(
        Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1),
        Region("JTG", "Jawa Tengah", RegionStatus.Upcoming, 2),
        Region("BALI", "Bali", RegionStatus.Upcoming, 3),
        Region("JTM", "Jawa Timur", RegionStatus.NotOpen, 4),
        Region("JBR", "Jawa Barat", RegionStatus.NotOpen, 5),
        Region("SUMBAR", "Sumatera Barat", RegionStatus.NotOpen, 6),
        Region("SULSEL", "Sulawesi Selatan", RegionStatus.NotOpen, 7),
        Region("NTT", "Nusa Tenggara Timur", RegionStatus.NotOpen, 8),
        Region("KALTIM", "Kalimantan Timur", RegionStatus.NotOpen, 9),
        Region("PAPUA", "Papua", RegionStatus.NotOpen, 10),
    )
}

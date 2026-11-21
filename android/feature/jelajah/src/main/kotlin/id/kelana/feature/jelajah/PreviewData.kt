package id.kelana.feature.jelajah

import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Data for @Preview functions only. Never used by a screen at runtime. */
internal object PreviewData {
    private val checked = LocalDate.of(2026, 11, 20)

    fun place(name: String, category: PlaceCategory, withCheck: Boolean = true) = Place(
        id = name.lowercase().replace(' ', '-'), slug = name, name = name, category = category, capCode = "YK-00",
        lat = 0.0, lng = 0.0, address = "Yogyakarta", description = "Contoh deskripsi $name.", coverKey = name,
        hours = DayOfWeek.entries.map {
            OfficialFact(
                OpeningHours(it, false, LocalTime.of(9, 0), LocalTime.of(15, 0)),
                sourceUrl = "https://example.org",
                checkedAt = if (withCheck) checked else null,
            )
        },
    )

    val places = listOf(
        place("Museum Sonobudoyo", PlaceCategory.Museum),
        place("Candi Prambanan", PlaceCategory.Candi),
        place("Tamansari", PlaceCategory.Lainnya, withCheck = false),
    )

    val event = Event(
        id = "preview", placeId = "candi-prambanan", placeName = "Prambanan", title = "Sendratari Roro Jonggrang",
        description = "", category = PlaceCategory.Tari,
        schedule = OfficialFact(
            EventSchedule(Recurrence.Daily, LocalTime.of(19, 30), LocalTime.of(20, 30)), "https://example.org", checked,
        ),
        ticket = OfficialFact(EventTicket(150_000, null), "https://example.org", checked),
    )

    fun homeState() = JelajahUiState(
        isLoading = false,
        date = LocalDate.of(2026, 11, 20),
        regionName = "DI Yogyakarta",
        tonightEvent = EventOccurrence(event, LocalDate.of(2026, 11, 20)),
        featuredPlaces = places,
    )
}

package id.kelana.feature.agenda

import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTag
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Sample values for @Preview only. Screens never read this. */
internal object PreviewData {
    val today: LocalDate = LocalDate.of(2026, 11, 20)
    private val checked: LocalDate = LocalDate.of(2026, 11, 20)

    val roro = Event(
        id = "roro-jonggrang-jumat",
        placeId = "candi-prambanan",
        placeName = "Candi Prambanan",
        title = "Sendratari Roro Jonggrang",
        description = "Pentas sendratari kisah Roro Jonggrang.",
        category = PlaceCategory.Tari,
        schedule = OfficialFact(
            EventSchedule(Recurrence.Weekly(DayOfWeek.FRIDAY), LocalTime.of(19, 30), LocalTime.of(20, 30)),
            "https://example.org/jadwal",
            checked,
        ),
        ticket = OfficialFact(EventTicket(150_000, "https://example.org/tiket"), "https://example.org/tiket", checked),
        performers = listOf("Lebih dari 200 penari dan pemusik lokal"),
        arrivalAdviceMin = 30,
        arrivalAdviceMax = 45,
        tags = setOf(EventTag.Tari),
    )

    private val macapat = roro.copy(
        id = "macapat-sabtu",
        placeName = "Keraton Yogyakarta",
        title = "Macapat, Bangsal Srimanganti",
        category = PlaceCategory.SeniMusik,
        performers = emptyList(),
        arrivalAdviceMin = null,
        arrivalAdviceMax = null,
        tags = emptySet(),
        schedule = roro.schedule.copy(
            value = EventSchedule(Recurrence.Weekly(DayOfWeek.SATURDAY), LocalTime.of(9, 0), LocalTime.of(11, 0)),
        ),
    )

    val agendaState = AgendaUiState(
        isLoading = false,
        today = today,
        selectedDate = today,
        regionName = "DI Yogyakarta",
        days = (0 until 7).map { AgendaDay(today.plusDays(it.toLong()), hasEvents = it % 2 == 0) },
        filters = EventTag.entries,
        tonight = EventOccurrence(roro, today),
        alsoToday = emptyList(),
        upcoming = listOf(AgendaSection(today.plusDays(1), listOf(EventOccurrence(macapat, today.plusDays(1))))),
    )
}

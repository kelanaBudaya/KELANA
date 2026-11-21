package id.kelana.feature.rencana.ui

import id.kelana.core.model.DayPlan
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanStop
import id.kelana.core.model.Recurrence
import id.kelana.core.model.Travel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TravelMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** PROVISIONAL sample states for @Preview only. Not data of the app. */
internal object PreviewRencana {
    private val today = LocalDate.of(2026, 11, 20)
    private val checked = LocalDate.of(2026, 11, 20)

    private fun hours(open: LocalTime, close: LocalTime) = OfficialFact(
        OpeningHours(DayOfWeek.FRIDAY, false, open, close),
        "https://example.org/preview",
        checked,
    )

    fun stops(): List<PlanStop> = listOf(
        PlanStop(
            placeId = "p1", placeName = "Keraton Yogyakarta", capCode = "YK-01", category = PlaceCategory.Lainnya,
            lat = -7.8053, lng = 110.3642, start = LocalTime.of(8, 30), end = LocalTime.of(10, 0),
            hours = hours(LocalTime.of(9, 0), LocalTime.of(15, 0)),
            price = OfficialFact(25_000L, "https://example.org/preview", checked),
        ),
        PlanStop(
            placeId = "p2", placeName = "Museum Sonobudoyo", capCode = "YK-02", category = PlaceCategory.Museum,
            lat = -7.8013, lng = 110.3641, start = LocalTime.of(10, 30), end = LocalTime.of(12, 0),
            hours = hours(LocalTime.of(8, 0), LocalTime.of(21, 0)),
            price = OfficialFact(null, null, null),
            travel = Travel(TravelMode.Walk, 9),
        ),
        PlanStop(
            placeId = "p3", placeName = "Candi Prambanan", capCode = "YK-10", category = PlaceCategory.Tari,
            lat = -7.752, lng = 110.4914, start = LocalTime.of(19, 30), end = LocalTime.of(20, 30),
            eventId = "e1", eventTitle = "Sendratari Roro Jonggrang",
            schedule = OfficialFact(
                EventSchedule(Recurrence.Weekly(DayOfWeek.FRIDAY), LocalTime.of(19, 30), LocalTime.of(20, 30)),
                "https://example.org/preview", checked,
            ),
            price = OfficialFact(150_000L, "https://example.org/preview", checked),
            travel = Travel(TravelMode.Transit, 45),
            arrivalAdviceMin = 30, arrivalAdviceMax = 45,
        ),
    )

    fun filled() = RencanaUiState(
        isLoading = false,
        today = today,
        now = LocalTime.of(10, 40),
        plan = DayPlan(today, stops()),
        companion = TravelGroup.Family,
    )

    fun empty() = RencanaUiState(isLoading = false, today = today, now = LocalTime.of(10, 40))
}

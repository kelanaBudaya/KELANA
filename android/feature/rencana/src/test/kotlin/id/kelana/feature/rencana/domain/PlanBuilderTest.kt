package id.kelana.feature.rencana.domain

import id.kelana.core.model.Closure
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.Event
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Pace
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.Recurrence
import id.kelana.core.model.SkipReason
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.TicketPrice
import id.kelana.core.model.TravelMode
import id.kelana.core.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

internal val Checked: LocalDate = LocalDate.of(2026, 11, 20)

/** Friday 20 Nov 2026. */
internal val Friday: LocalDate = LocalDate.of(2026, 11, 20)
internal val Monday: LocalDate = LocalDate.of(2026, 11, 23)

private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

internal fun hoursFacts(
    opens: LocalTime = t(9),
    closes: LocalTime = t(15),
    closedDay: DayOfWeek? = null,
    checkedAt: LocalDate? = Checked,
): List<OfficialFact<OpeningHours>> = DayOfWeek.entries.map { day ->
    val closed = day == closedDay
    OfficialFact(
        OpeningHours(day, closed, if (closed) null else opens, if (closed) null else closes),
        "https://example.org/h",
        checkedAt,
    )
}

internal fun place(
    id: String,
    category: PlaceCategory = PlaceCategory.Museum,
    lat: Double = -7.80,
    lng: Double = 110.36,
    hours: List<OfficialFact<OpeningHours>> = hoursFacts(),
    closures: List<OfficialFact<Closure>> = emptyList(),
    price: Long? = 10_000,
) = Place(
    id = id, slug = id, name = id.uppercase(), category = category, capCode = "YK-$id", lat = lat, lng = lng,
    address = "", description = "", coverKey = id, hours = hours,
    tickets = listOf(OfficialFact(TicketPrice("Dewasa", price, null), "https://example.org/t", Checked)),
    closures = closures,
)

internal fun show(
    id: String,
    placeId: String,
    weekday: DayOfWeek = DayOfWeek.FRIDAY,
    start: LocalTime = t(19, 30),
    end: LocalTime? = t(20, 30),
    checkedAt: LocalDate? = Checked,
) = Event(
    id = id, placeId = placeId, placeName = placeId, title = "Show $id", description = "", category = PlaceCategory.Tari,
    schedule = OfficialFact(EventSchedule(Recurrence.Weekly(weekday), start, end), "https://example.org/e", checkedAt),
    ticket = OfficialFact(EventTicket(150_000, null), "https://example.org/e", Checked),
    arrivalAdviceMin = 30, arrivalAdviceMax = 45,
)

internal fun choices(
    interests: Set<Interest> = setOf(Interest.Museum),
    date: LocalDate = Friday,
    companion: TravelGroup = TravelGroup.Solo,
    pace: Pace = Pace.Packed,
    vehicle: Vehicle = Vehicle.TransitAndWalk,
    needs: Set<SpecialNeed> = emptySet(),
    start: LocalTime = t(8),
    end: LocalTime = t(21),
) = PlanChoices(date, date, companion, interests, start, end, pace, vehicle, needs)

class PlanBuilderTest {
    private val builder = PlanBuilder()

    @Test
    fun picksOnlySelectedCategories() {
        val places = listOf(place("m1"), place("c1", PlaceCategory.Candi))
        val plan = builder.build(choices(setOf(Interest.Museum)), Friday, places, emptyList())
        assertEquals(listOf("m1"), plan.stops.map { it.placeId })
    }

    @Test
    fun keratonInterestSelectsLainnya() {
        val places = listOf(place("k1", PlaceCategory.Lainnya), place("m1"))
        val plan = builder.build(choices(setOf(Interest.Keraton)), Friday, places, emptyList())
        assertEquals(listOf("k1"), plan.stops.map { it.placeId })
    }

    @Test
    fun skipsPlaceClosedOnThatWeekday() {
        val places = listOf(place("a", hours = hoursFacts(closedDay = DayOfWeek.MONDAY)), place("b"))
        val plan = builder.build(choices(), Monday, places, emptyList())
        assertEquals(listOf("b"), plan.stops.map { it.placeId })
        assertEquals(SkipReason.ClosedThatDay, plan.skipped.single { it.placeId == "a" }.reason)
    }

    @Test
    fun skipsPlaceInsideAClosurePeriodAndKeepsTheReason() {
        val closure = OfficialFact(Closure(Friday.minusDays(1), Friday.plusDays(1), "Acara keraton"), "https://example.org/c", Checked)
        val places = listOf(place("a", closures = listOf(closure)), place("b"))
        val plan = builder.build(choices(), Friday, places, emptyList())
        assertEquals(listOf("b"), plan.stops.map { it.placeId })
        val skipped = plan.skipped.single()
        assertEquals(SkipReason.TemporarilyClosed, skipped.reason)
        assertEquals("Acara keraton", skipped.detail)
    }

    @Test
    fun closureWithoutCheckDateIsIgnoredAndPlaceWithoutCheckedHoursIsNeverPlanned() {
        val unchecked = OfficialFact(Closure(Friday, Friday, "x"), null, null)
        val places = listOf(
            place("a", closures = listOf(unchecked)),
            place("noHours", hours = hoursFacts(checkedAt = null)),
        )
        val plan = builder.build(choices(), Friday, places, emptyList())
        assertEquals(listOf("a"), plan.stops.map { it.placeId })
        assertEquals(SkipReason.HoursUnknown, plan.skipped.single { it.placeId == "noHours" }.reason)
    }

    @Test
    fun neverPlansAVisitThatWouldEndAfterClosing() {
        // Museum visit is 90 minutes; the place is open only 60 minutes.
        val places = listOf(place("short", hours = hoursFacts(opens = t(9), closes = t(10))))
        val plan = builder.build(choices(), Friday, places, emptyList())
        assertTrue(plan.stops.isEmpty())
        assertEquals(SkipReason.NoRoomInDay, plan.skipped.single().reason)
    }

    @Test
    fun everyStopLiesInsideItsOpeningHoursAndTheWindow() {
        val places = listOf(
            place("a", lat = -7.80, hours = hoursFacts(t(10), t(15))),
            place("b", lat = -7.805, hours = hoursFacts(t(8), t(12))),
            place("c", lat = -7.81, hours = hoursFacts(t(13), t(16))),
        )
        val plan = builder.build(choices(start = t(9), end = t(17)), Friday, places, emptyList())
        assertTrue(plan.stops.isNotEmpty())
        plan.stops.forEach { stop ->
            val hours = stop.hours!!.value
            assertFalse(stop.start.isBefore(hours.opens))
            assertFalse(stop.end.isAfter(hours.closes))
            assertFalse(stop.start.isBefore(t(9)))
            assertFalse(stop.end.isAfter(t(17)))
        }
        plan.stops.zipWithNext().forEach { (x, y) -> assertFalse(y.start.isBefore(x.end)) }
    }

    @Test
    fun respectsTimeWindowEnd() {
        val places = listOf(place("a"), place("b", lat = -7.801))
        val plan = builder.build(choices(start = t(9), end = t(10, 30)), Friday, places, emptyList())
        assertEquals(1, plan.stops.size)
        assertEquals(t(10, 30), plan.stops.single().end)
    }

    @Test
    fun respectsMaxStopsOfThePace() {
        val places = (1..8).map { place("p$it", lat = -7.80 - it * 0.001, hours = hoursFacts(t(8), t(21))) }
        val relaxed = builder.build(choices(pace = Pace.Relaxed, end = t(22)), Friday, places, emptyList())
        assertEquals(3, relaxed.stops.size)
        assertEquals(5, relaxed.skipped.count { it.reason == SkipReason.NoRoomInDay })
    }

    @Test
    fun picksNearestNextStop() {
        val places = listOf(
            place("a", lat = -7.800, lng = 110.360, hours = hoursFacts(t(8), t(21))),
            place("near", lat = -7.801, lng = 110.360, hours = hoursFacts(t(8), t(21))),
            place("far", lat = -7.900, lng = 110.360, hours = hoursFacts(t(8), t(21))),
        )
        val plan = builder.build(choices(pace = Pace.Packed, end = t(22)), Friday, places, emptyList())
        assertEquals(listOf("a", "near", "far"), plan.stops.map { it.placeId })
        assertNull(plan.stops.first().travel)
        assertNotNull(plan.stops[1].travel)
    }

    @Test
    fun showIsLastAndPlacesLeaveTimeToReachIt() {
        val places = listOf(
            place("m1", hours = hoursFacts(t(8), t(21))),
            place("pram", PlaceCategory.Candi, lat = -7.752, lng = 110.491, hours = hoursFacts(t(6, 30), t(17, 30))),
        )
        val events = listOf(show("roro", "pram"))
        val plan = builder.build(
            choices(setOf(Interest.Museum, Interest.Pentas), companion = TravelGroup.Family, pace = Pace.Moderate),
            Friday, places, events,
        )
        val last = plan.stops.last()
        assertEquals("roro", last.eventId)
        assertEquals(t(19, 30), last.start)
        assertTrue(plan.stops.dropLast(1).all { it.end < last.start })
        assertTrue(plan.stops.size <= Pace.Moderate.maxStops)
        // Travelling from the museum to Prambanan is estimated and not zero.
        assertTrue(last.travel!!.minutes > 0)
    }

    @Test
    fun showWithoutCheckDateOrOutsideWindowIsNotPlanned() {
        val places = listOf(place("pram", PlaceCategory.Candi))
        val unchecked = show("a", "pram", checkedAt = null)
        val plan1 = builder.build(choices(setOf(Interest.Pentas)), Friday, places, listOf(unchecked))
        assertTrue(plan1.stops.isEmpty())
        assertTrue(plan1.noShowFound)

        val plan2 = builder.build(choices(setOf(Interest.Pentas), end = t(20)), Friday, places, listOf(show("b", "pram")))
        assertTrue(plan2.stops.isEmpty())
        assertTrue(plan2.noShowFound)
    }

    @Test
    fun showOnAnotherWeekdayIsNotPlanned() {
        val places = listOf(place("pram", PlaceCategory.Candi))
        val plan = builder.build(choices(setOf(Interest.Pentas)), Friday, places, listOf(show("tue", "pram", DayOfWeek.TUESDAY)))
        assertTrue(plan.stops.isEmpty())
    }

    @Test
    fun walkOnlyDropsPlacesFarFromThePreviousStop() {
        val places = listOf(
            place("a", lat = -7.800, hours = hoursFacts(t(8), t(21))),
            place("far", lat = -7.752, lng = 110.491, hours = hoursFacts(t(8), t(21))),
        )
        val plan = builder.build(choices(vehicle = Vehicle.WalkOnly, end = t(22)), Friday, places, emptyList())
        assertEquals(listOf("a"), plan.stops.map { it.placeId })
    }

    @Test
    fun travelModeFollowsVehicle() {
        val places = listOf(
            place("a", lat = -7.80, hours = hoursFacts(t(8), t(21))),
            place("b", lat = -7.83, hours = hoursFacts(t(8), t(21))),
        )
        val transit = builder.build(choices(vehicle = Vehicle.TransitAndWalk, end = t(22)), Friday, places, emptyList())
        val car = builder.build(choices(vehicle = Vehicle.PrivateVehicle, end = t(22)), Friday, places, emptyList())
        assertEquals(TravelMode.Transit, transit.stops[1].travel!!.mode)
        assertEquals(TravelMode.Car, car.stops[1].travel!!.mode)
    }

    @Test
    fun familyAndRestNeedAddBreakTime() {
        val places = listOf(
            place("a", lat = -7.800, hours = hoursFacts(t(8), t(21))),
            place("b", lat = -7.8001, hours = hoursFacts(t(8), t(21))),
        )
        val solo = builder.build(choices(companion = TravelGroup.Solo, pace = Pace.Packed), Friday, places, emptyList())
        val family = builder.build(choices(companion = TravelGroup.Family, pace = Pace.Packed), Friday, places, emptyList())
        val rests = builder.build(choices(needs = setOf(SpecialNeed.MoreRests), pace = Pace.Packed), Friday, places, emptyList())
        assertEquals(15, family.stops[1].start.toSecondOfDay() / 60 - solo.stops[1].start.toSecondOfDay() / 60)
        assertEquals(family.stops[1].start, rests.stops[1].start)
    }

    @Test
    fun accessNeedsFlagEveryStop() {
        val places = listOf(place("a"))
        val flagged = builder.build(choices(needs = setOf(SpecialNeed.Wheelchair)), Friday, places, emptyList())
        val plain = builder.build(choices(needs = setOf(SpecialNeed.MoreRests)), Friday, places, emptyList())
        assertTrue(flagged.stops.all { it.accessUnverified })
        assertTrue(plain.stops.none { it.accessUnverified })
    }

    @Test
    fun priceFactKeepsItsCheckDate() {
        val stop = builder.build(choices(), Friday, listOf(place("a", price = null)), emptyList()).stops.single()
        assertEquals(Checked, stop.price!!.checkedAt)
        assertNull(stop.price!!.value)
        assertEquals(Checked, stop.hours!!.checkedAt)
    }

    @Test
    fun sameInputGivesSamePlan() {
        val places = (1..6).map { place("p$it", lat = -7.80 - it * 0.002) }
        val a = builder.build(choices(), Friday, places, emptyList())
        val b = builder.build(choices(), Friday, places.shuffled(java.util.Random(7)), emptyList())
        assertEquals(a, b)
    }

    @Test
    fun excludedPlacesAreNeverPlannedAndNotReported() {
        val places = listOf(place("a"), place("b"))
        val plan = builder.build(choices(), Friday, places, emptyList(), excludedPlaceIds = setOf("a"))
        assertEquals(listOf("b"), plan.stops.map { it.placeId })
        assertTrue(plan.skipped.none { it.placeId == "a" })
    }

    @Test
    fun buildTripMakesOneDayPerDateAndCapsAtSeven() {
        val long = choices().copy(endDate = Friday.plusDays(20))
        val trip = builder.buildTrip(long, listOf(place("a")), emptyList(), builtOn = Friday)
        assertEquals(PlanBuilder.MAX_DAYS, trip.days.size)
        assertEquals(Friday, trip.days.first().date)
    }

    @Test
    fun closedStopsFindsAPlaceThatClosedAfterThePlanWasBuilt() {
        val open = place("a")
        val plan = builder.build(choices(), Friday, listOf(open, place("b", lat = -7.81)), emptyList())
        val closure = OfficialFact(Closure(Friday, Friday, "Acara"), "https://example.org/c", Checked)
        val updated = listOf(open.copy(closures = listOf(closure)), place("b", lat = -7.81))
        val closed = builder.closedStops(plan, updated)
        assertEquals(listOf("a"), closed.map { it.stop.placeId })
        assertTrue(closed.single().availability is Availability.TemporarilyClosed)
        assertTrue(builder.closedStops(plan, listOf(open, place("b", lat = -7.81))).isEmpty())
    }

    @Test
    fun closedStopsFlagsHoursThatMovedUnderAPlannedVisit() {
        val plan = builder.build(choices(), Friday, listOf(place("a")), emptyList())
        val moved = place("a", hours = hoursFacts(t(12), t(15)))
        assertEquals(1, builder.closedStops(plan, listOf(moved)).size)
    }

    @Test
    fun replanSkipsReplacementCandidatesThatAreClosedToo() {
        val places = listOf(
            place("keraton", PlaceCategory.Lainnya, lat = -7.8053),
            place("tamansari", PlaceCategory.Lainnya, lat = -7.8100),
            place("puro", PlaceCategory.Lainnya, lat = -7.8007, hours = hoursFacts(closedDay = DayOfWeek.FRIDAY)),
        )
        val ch = choices(setOf(Interest.Keraton), pace = Pace.Relaxed)
        val original = builder.build(ch, Friday, places, emptyList())
        assertEquals(setOf("keraton", "tamansari"), original.stops.map { it.placeId }.toSet())

        val result = builder.replan(original, ch, "keraton", places, emptyList(), now = null)
        assertEquals(listOf("tamansari"), result.plan.stops.map { it.placeId })
        assertNull(result.replacement)
    }

    @Test
    fun replanOffersAReplacementNotYetInThePlan() {
        val places = listOf(
            place("a", lat = -7.800),
            place("b", lat = -7.801),
            place("c", lat = -7.802),
        )
        val ch = choices(pace = Pace.Relaxed, end = t(22))
        val original = builder.build(ch, Friday, places, emptyList())
        assertEquals(3, original.stops.size)
        val four = places + place("d", lat = -7.803)
        val result = builder.replan(original, ch.copy(pace = Pace.Moderate), "b", four, emptyList(), now = null)
        assertEquals("d", result.replacement!!.placeId)
        assertTrue(result.plan.stops.none { it.placeId == "b" })
    }

    @Test
    fun replanKeepsFinishedStopsAndStartsAfterNow() {
        val places = listOf(
            place("a", lat = -7.800, hours = hoursFacts(t(8), t(21))),
            place("b", lat = -7.801, hours = hoursFacts(t(8), t(21))),
            place("c", lat = -7.802, hours = hoursFacts(t(8), t(21))),
            place("d", lat = -7.803, hours = hoursFacts(t(8), t(21))),
        )
        val ch = choices(pace = Pace.Relaxed, end = t(22))
        val original = builder.build(ch, Friday, places.take(3), emptyList())
        val doneStop = original.stops.first()
        val now = doneStop.end.plusMinutes(5)
        val result = builder.replan(original, ch, original.stops[1].placeId, places, emptyList(), now)
        assertEquals(doneStop, result.plan.stops.first())
        result.plan.stops.drop(1).forEach { assertFalse(it.start.isBefore(now)) }
        assertTrue(result.plan.stops.none { it.placeId == original.stops[1].placeId })
        assertNotNull(result.plan.stops[1].travel)
    }

    @Test
    fun removeStopRecomputesTravelIntoTheFollowingStop() {
        val places = listOf(
            place("a", lat = -7.800, hours = hoursFacts(t(8), t(21))),
            place("b", lat = -7.801, hours = hoursFacts(t(8), t(21))),
            place("c", lat = -7.830, hours = hoursFacts(t(8), t(21))),
        )
        val plan = builder.build(choices(end = t(22)), Friday, places, emptyList())
        val removed = builder.removeStop(plan, "b", Vehicle.TransitAndWalk)
        assertEquals(listOf("a", "c"), removed.stops.map { it.placeId })
        assertEquals(TravelMode.Transit, removed.stops[1].travel!!.mode)
        val removedFirst = builder.removeStop(plan, "a", Vehicle.TransitAndWalk)
        assertNull(removedFirst.stops.first().travel)
    }
}

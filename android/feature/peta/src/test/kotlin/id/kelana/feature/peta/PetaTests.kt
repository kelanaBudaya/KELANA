package id.kelana.feature.peta

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.data.repository.FakeEventRepository
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.TicketPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private val friday: LocalDate = LocalDate.of(2026, 11, 20)
private val monday: LocalDate = LocalDate.of(2026, 11, 23)

private fun hoursOn(day: DayOfWeek, checked: LocalDate?, closed: Boolean = false) = OfficialFact(
    OpeningHours(
        weekday = day,
        isClosed = closed,
        opens = if (closed) null else LocalTime.of(9, 0),
        closes = if (closed) null else LocalTime.of(15, 0),
    ),
    sourceUrl = null,
    checkedAt = checked,
)

private fun place(
    id: String = "p",
    hours: List<OfficialFact<OpeningHours>> = emptyList(),
    tickets: List<OfficialFact<TicketPrice>> = emptyList(),
    capCode: String = "YK-01",
    category: PlaceCategory = PlaceCategory.Museum,
) = Place(
    id = id, slug = id, name = "Museum $id", category = category, capCode = capCode, lat = -7.8, lng = 110.4,
    address = "", description = "", coverKey = id, hours = hours, tickets = tickets,
)

private fun ticket(price: Long?, checked: LocalDate?) =
    OfficialFact(TicketPrice("Dewasa", price, null), sourceUrl = null, checkedAt = checked)

class PetaProjectionTest {
    @Test fun boundsOfEmptyListIsNull() {
        assertNull(boundsOf(emptyList()))
    }

    @Test fun boundsCoverAllPlaces() {
        val b = boundsOf(listOf(-7.75 to 110.49, -7.83 to 110.36, -7.80 to 110.40))!!
        assertEquals(-7.75, b.north, 1e-9)
        assertEquals(-7.83, b.south, 1e-9)
        assertEquals(110.36, b.west, 1e-9)
        assertEquals(110.49, b.east, 1e-9)
        assertEquals(-7.79, b.centerLat, 1e-9)
        assertEquals(110.425, b.centerLng, 1e-9)
    }

    @Test fun singlePlaceIsAPointAndNeedsAFixedZoom() {
        assertTrue(boundsOf(listOf(-7.8 to 110.4))!!.isPoint())
        assertFalse(boundsOf(listOf(-7.75 to 110.49, -7.83 to 110.36))!!.isPoint())
    }

    @Test fun tileConfigIsOneOsmRasterTemplate() {
        assertTrue(TILE_URL_TEMPLATE.contains("{z}") && TILE_URL_TEMPLATE.contains("{x}") && TILE_URL_TEMPLATE.contains("{y}"))
        assertTrue(osmStyleJson().contains(TILE_URL_TEMPLATE))
        assertTrue(TILE_USER_AGENT.isNotBlank())
    }

    @Test fun pinLabelsAreShortened() {
        assertEquals("Vredeburg", pinLabel("Museum Benteng Vredeburg"))
        assertEquals("Prambanan", pinLabel("Candi Prambanan"))
        assertEquals("Pakualaman", pinLabel("Puro Pakualaman"))
        assertEquals("Keraton", pinLabel("Keraton Yogyakarta"))
        assertEquals("Tamansari", pinLabel("Tamansari"))
    }
}

class PetaFiltersTest {
    @Test fun uncheckedHoursAreNotOpen() {
        val p = place(hours = listOf(hoursOn(DayOfWeek.FRIDAY, checked = null)))
        assertFalse(p.isOpenOn(friday))
    }

    @Test fun checkedOpenDayIsOpen() {
        val p = place(hours = listOf(hoursOn(DayOfWeek.FRIDAY, checked = friday)))
        assertTrue(p.isOpenOn(friday))
    }

    @Test fun closedDayAndMissingDayAreNotOpen() {
        val closed = place(hours = listOf(hoursOn(DayOfWeek.MONDAY, checked = friday, closed = true)))
        assertFalse(closed.isOpenOn(monday))
        assertFalse(place(hours = emptyList()).isOpenOn(friday))
    }

    @Test fun filterMatching() {
        val open = PetaPlace(place(capCode = "YK-01"), openToday = true, showToday = false)
        val shut = PetaPlace(place(capCode = "  "), openToday = false, showToday = false)
        assertTrue(open.matches(PetaFilter.All) && shut.matches(PetaFilter.All))
        assertTrue(open.matches(PetaFilter.OpenToday))
        assertFalse(shut.matches(PetaFilter.OpenToday))
        assertTrue(open.matches(PetaFilter.HasCap))
        assertFalse(shut.matches(PetaFilter.HasCap))
    }

    @Test fun badgeChoice() {
        assertEquals(PetaBadge.ShowTonight, badgeFor(showToday = true, openToday = true))
        assertEquals(PetaBadge.ShowTonight, badgeFor(showToday = true, openToday = false))
        assertEquals(PetaBadge.OpenToday, badgeFor(showToday = false, openToday = true))
        assertNull(badgeFor(showToday = false, openToday = false))
    }

    @Test fun cardFactsUseOnlyCheckedValues() {
        val checked = place(
            hours = listOf(hoursOn(DayOfWeek.FRIDAY, friday)),
            tickets = listOf(ticket(30_000, friday), ticket(10_000, friday), ticket(99_000, null)),
        )
        val facts = cardFacts(checked, friday)
        assertEquals(HoursLine.Open(LocalTime.of(9, 0), LocalTime.of(15, 0)), facts.hours)
        assertEquals(PriceLine.Paid(10_000, isFrom = true), facts.price)
        assertEquals(friday, facts.checkedAt)

        val unchecked = place(hours = listOf(hoursOn(DayOfWeek.FRIDAY, null)), tickets = listOf(ticket(30_000, null)))
        val f = cardFacts(unchecked, friday)
        assertEquals(HoursLine.Checking, f.hours)
        assertEquals(PriceLine.Checking, f.price)
        assertNull(f.checkedAt)
    }

    @Test fun cardFactsFreeAndClosed() {
        val p = place(hours = listOf(hoursOn(DayOfWeek.MONDAY, friday, closed = true)), tickets = listOf(ticket(0, friday)))
        val f = cardFacts(p, monday)
        assertEquals(HoursLine.Closed, f.hours)
        assertEquals(PriceLine.Free, f.price)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PetaViewModelTest {
    private val places = FakePlaceRepository()
    private val events = FakeEventRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    private fun vm(date: LocalDate = friday, p: PlaceRepository = places, e: EventRepository = events) =
        PetaViewModel(p, e) { date }

    @Test fun everyPlaceFromTheRepositoryBecomesAPin() = runTest {
        vm().state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(10, s.pins.size)
            assertFalse(s.hasError)
            assertFalse(s.isEmpty)
        }
    }

    @Test fun openTodayFilterKeepsOnlyOpenPlaces() = runTest {
        val v = vm(monday) // sample: most museums are closed on Monday
        v.state.test {
            awaitUntil { !it.isLoading }
            v.onFilterChange(PetaFilter.OpenToday)
            val s = awaitUntil { it.filter == PetaFilter.OpenToday }
            assertTrue(s.pins.isNotEmpty())
            assertTrue(s.pins.all { it.openToday })
            assertTrue(s.pins.size < 10)
        }
    }

    @Test fun hasCapFilterUsesCapCode() = runTest {
        val v = vm()
        v.state.test {
            awaitUntil { !it.isLoading }
            v.onFilterChange(PetaFilter.HasCap)
            val s = awaitUntil { it.filter == PetaFilter.HasCap }
            assertEquals(10, s.pins.size)
            assertTrue(s.pins.all { it.place.capCode.isNotBlank() })
        }
    }

    @Test fun tappingAPinSelectsAndTappingAgainClears() = runTest {
        val v = vm()
        v.state.test {
            awaitUntil { !it.isLoading }
            v.onPinTap("candi-prambanan")
            assertEquals("candi-prambanan", awaitUntil { it.selectedId != null }.selected?.place?.id)
            v.onPinTap("tamansari")
            assertEquals("tamansari", awaitUntil { it.selectedId == "tamansari" }.selected?.place?.id)
            v.onPinTap("tamansari")
            assertNull(awaitUntil { it.selectedId == null }.selected)
        }
    }

    @Test fun selectionHiddenByFilterReadsAsNoSelection() = runTest {
        val v = vm(monday)
        v.state.test {
            awaitUntil { !it.isLoading }
            v.onPinTap("museum-sonobudoyo") // closed on Monday in the sample
            awaitUntil { it.selectedId != null }
            v.onFilterChange(PetaFilter.OpenToday)
            val s = awaitUntil { it.filter == PetaFilter.OpenToday }
            assertNull(s.selected)
        }
    }

    @Test fun showBadgeComesFromTodaysEventsAtThePlace() = runTest {
        vm(friday).state.test {
            val s = awaitUntil { !it.isLoading }
            val prambanan = s.pins.first { it.place.id == "candi-prambanan" }
            assertTrue(prambanan.showToday)
            assertEquals(PetaBadge.ShowTonight, badgeFor(prambanan.showToday, prambanan.openToday))
            val other = s.pins.first { it.place.id == "tamansari" }
            assertFalse(other.showToday)
            assertEquals(PetaBadge.OpenToday, badgeFor(other.showToday, other.openToday))
        }
    }

    @Test fun laterEmissionFromRefreshReachesTheMap() = runTest {
        // Cache-then-network: the cache emits first, the refreshed list arrives later and must replace it.
        val all = places.places().first()
        val live = MutableStateFlow(all.take(3))
        val refreshing = object : PlaceRepository by places {
            override fun places(): Flow<List<Place>> = live
        }
        vm(p = refreshing).state.test {
            assertEquals(3, awaitUntil { !it.isLoading }.pins.size)
            live.value = all
            assertEquals(10, awaitUntil { it.pins.size == 10 }.pins.size)
        }
    }

    @Test fun noPlacesIsEmptyState() = runTest {
        val none = object : PlaceRepository by places {
            override fun places(): Flow<List<Place>> = flowOf(emptyList())
        }
        vm(p = none).state.test {
            assertTrue(awaitUntil { !it.isLoading }.isEmpty)
        }
    }

    @Test fun failingPlacesIsErrorAndRetryRecovers() = runTest {
        var fail = true
        val flaky = object : PlaceRepository by places {
            override fun places(): Flow<List<Place>> = flow {
                if (fail) error("boom")
                emitAll(places.places())
            }
        }
        val v = vm(p = flaky)
        v.state.test {
            assertTrue(awaitUntil { !it.isLoading }.hasError)
            fail = false
            v.onRetry()
            val s = awaitUntil { !it.isLoading && !it.hasError }
            assertEquals(10, s.pins.size)
            // The state is live now: the events lookup may emit once more after the places.
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun failingEventsOnlyDropsTheShowBadge() = runTest {
        val brokenEvents = object : EventRepository by events {
            override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = flow { error("boom") }
        }
        vm(e = brokenEvents).state.test {
            val s = awaitUntil { !it.isLoading }
            assertFalse(s.hasError)
            assertEquals(10, s.pins.size)
            assertTrue(s.pins.none { it.showToday })
        }
    }
}

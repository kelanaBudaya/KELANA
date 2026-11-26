package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlacePhoto
import id.kelana.core.model.PlacePhotoRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.Recurrence
import id.kelana.core.model.TicketPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DetailTempatViewModelTest {
    private val date = LocalDate.of(2026, 11, 20)

    private fun place(checkedAt: LocalDate?) = Place(
        id = "p1", slug = "p1", name = "Museum Uji", category = PlaceCategory.Museum, capCode = "YK-02",
        lat = 0.0, lng = 0.0, address = "Jl. Uji", description = "Deskripsi", coverKey = "p1",
        hours = listOf(
            OfficialFact(OpeningHours(DayOfWeek.MONDAY, true, null, null), "https://x.id/a", checkedAt),
            OfficialFact(OpeningHours(DayOfWeek.TUESDAY, false, LocalTime.of(8, 0), LocalTime.of(15, 0)), "https://x.id/a", checkedAt),
        ),
        tickets = listOf(OfficialFact(TicketPrice("Dewasa", 10_000, null), null, checkedAt)),
    )

    private class FakePlaces(private val place: Place?, private val failures: Int = 0) : PlaceRepository {
        var calls = 0
        override fun place(id: String): Flow<Place?> = flow {
            calls++
            if (calls <= failures) error("boom")
            emit(place)
        }
        override fun places(): Flow<List<Place>> = flowOf(listOfNotNull(place))
        override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
    }

    private class FakeEvents(
        private val all: List<Event> = emptyList(),
        private val fail: Boolean = false,
    ) : EventRepository {
        override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = flowOf(emptyList())
        override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> = flowOf(emptyList())
        override fun event(id: String): Flow<Event?> = flowOf(all.firstOrNull { it.id == id })
        override fun eventsAt(placeId: String): Flow<List<Event>> =
            if (fail) flow { error("boom") } else flowOf(all.filter { it.placeId == placeId })
    }

    private fun event(
        id: String,
        recurrence: Recurrence,
        checkedAt: LocalDate? = date,
        start: LocalTime = LocalTime.of(20, 0),
        placeId: String = "p1",
    ) = Event(
        id = id, placeId = placeId, placeName = "Museum Uji", title = "Pentas $id", description = "",
        category = PlaceCategory.Tari,
        schedule = OfficialFact(EventSchedule(recurrence, start, null), null, checkedAt),
        ticket = OfficialFact(EventTicket(60_000, null), null, checkedAt),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Fixed "today": Friday 20 Nov 2026.
    private val clock: Clock = Clock.fixed(
        date.atTime(10, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant(), ZoneId.of("Asia/Jakarta"),
    )

    private class FakePhotos(private val count: Int = 0, private val fail: Boolean = false) : PlacePhotoRepository {
        override fun photos(placeId: String): Flow<List<PlacePhoto>> =
            if (fail) flow { error("boom") } else flowOf((1..count).map { testPhoto(it) })
    }

    private fun vm(repo: PlaceRepository, events: EventRepository = FakeEvents(), photos: PlacePhotoRepository = FakePhotos()) =
        DetailTempatViewModel(SavedStateHandle(mapOf("placeId" to "p1")), repo, events, photos, clock)

    private suspend fun app.cash.turbine.ReceiveTurbine<DetailTempatUiState>.awaitLoaded(): DetailTempatUiState {
        var item = awaitItem()
        // The first Loaded item may carry no events yet (they load without blocking the place).
        while (item.status != PlaceLoadStatus.Loaded) item = awaitItem()
        return item
    }

    @Test
    fun found_place_is_loaded() = runTest {
        vm(FakePlaces(place(date))).uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.Loaded, item.status)
            assertEquals("Museum Uji", item.place?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missing_place_is_not_found() = runTest {
        vm(FakePlaces(null)).uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.NotFound, item.status)
            assertNull(item.place)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failure_is_error_and_retry_recovers() = runTest {
        val vm = vm(FakePlaces(place(date), failures = 1))
        vm.uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.Error, item.status)
            vm.retry()
            item = awaitItem()
            while (item.status != PlaceLoadStatus.Loaded) item = awaitItem()
            assertNotNull(item.place)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun facts_keep_their_check_date_or_lack_of_it() = runTest {
        // The state passes facts through untouched: the UI decides what to render from display().
        listOf(date, null).forEach { checkedAt ->
            vm(FakePlaces(place(checkedAt))).uiState.test {
                var item = awaitItem()
                while (item.status != PlaceLoadStatus.Loaded) item = awaitItem()
                val facts = item.place!!.hours + item.place!!.tickets
                assertEquals(true, facts.all { it.checkedAt == checkedAt })
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun hostedEvents_areShownSoonestFirst() = runTest {
        val events = FakeEvents(
            listOf(
                event("weekly-sat", Recurrence.Weekly(DayOfWeek.SATURDAY)),
                event("daily", Recurrence.Daily),
                event("once", Recurrence.Once(date.plusDays(10))),
                event("elsewhere", Recurrence.Daily, placeId = "other"),
            ),
        )
        vm(FakePlaces(place(date)), events).uiState.test {
            var item = awaitLoaded()
            while (item.events.isEmpty()) item = awaitItem()
            assertEquals(listOf("daily", "weekly-sat", "once"), item.events.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noEvents_leavesListEmpty() = runTest {
        vm(FakePlaces(place(date)), FakeEvents()).uiState.test {
            val item = awaitLoaded()
            assertTrue(item.events.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun eventsFailure_hidesSectionButKeepsPlace() = runTest {
        vm(FakePlaces(place(date)), FakeEvents(listOf(event("a", Recurrence.Daily)), fail = true)).uiState.test {
            val item = awaitLoaded()
            assertEquals("Museum Uji", item.place?.name)
            assertTrue(item.events.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pastOnceEvent_andUncheckedEvent_areFiltered() = runTest {
        val events = FakeEvents(
            listOf(
                event("past", Recurrence.Once(date.minusDays(1))),
                event("today", Recurrence.Once(date)),
                event("unchecked", Recurrence.Daily, checkedAt = null),
            ),
        )
        vm(FakePlaces(place(date)), events).uiState.test {
            var item = awaitLoaded()
            while (item.events.isEmpty()) item = awaitItem()
            assertEquals(listOf("today"), item.events.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun photoCount_comes_from_repository() = runTest {
        vm(FakePlaces(place(date)), photos = FakePhotos(count = 3)).uiState.test {
            var item = awaitLoaded()
            while (item.photoCount == 0) item = awaitItem()
            assertEquals(3, item.photoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noPhotos_meansZeroCount() = runTest {
        vm(FakePlaces(place(date)), photos = FakePhotos(count = 0)).uiState.test {
            assertEquals(0, awaitLoaded().photoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun photosFailure_hidesChipButKeepsPlace() = runTest {
        vm(FakePlaces(place(date)), photos = FakePhotos(fail = true)).uiState.test {
            val item = awaitLoaded()
            assertEquals("Museum Uji", item.place?.name)
            assertEquals(0, item.photoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

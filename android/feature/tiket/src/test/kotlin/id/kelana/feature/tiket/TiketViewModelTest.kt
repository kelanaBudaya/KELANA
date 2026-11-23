package id.kelana.feature.tiket

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val Checked: LocalDate = LocalDate.of(2026, 11, 20)

private fun place(
    id: String,
    category: PlaceCategory = PlaceCategory.Candi,
    tickets: List<OfficialFact<TicketPrice>>,
) = Place(
    id = id, slug = id, name = "Tempat $id", category = category, capCode = "X", lat = 0.0, lng = 0.0,
    address = "", description = "", coverKey = id, tickets = tickets,
)

private fun tier(price: Long?, checkedAt: LocalDate?, url: String? = "https://tiket.example.org/$price") =
    OfficialFact(TicketPrice("Dewasa", price, url), url, checkedAt)

private fun show(id: String, price: Long?, checkedAt: LocalDate?) = Event(
    id = id, placeId = "p", placeName = "Tempat p", title = "Pentas $id", description = "",
    category = PlaceCategory.Tari,
    schedule = OfficialFact(
        EventSchedule(Recurrence.Weekly(DayOfWeek.FRIDAY), LocalTime.of(19, 30), null),
        "https://jadwal.example.org",
        Checked,
    ),
    ticket = OfficialFact(EventTicket(price, "https://tiket.example.org/e"), "https://tiket.example.org/e", checkedAt),
)

private class FakePlaces(private val source: Flow<List<Place>>) : PlaceRepository {
    override fun places() = source
    override fun place(id: String): Flow<Place?> = flowOf(null)
    override fun placesByCategory(category: PlaceCategory) = flowOf(emptyList<Place>())
    override fun search(query: String) = flowOf(emptyList<Place>())
}

private class FakeEvents(private val all: List<Event>) : EventRepository {
    override fun eventsOn(date: LocalDate) = flowOf(emptyList<EventOccurrence>())
    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        // Each weekly event shows up twice in a two-week window: the view model must list it once.
        flowOf(all.flatMap { e -> listOf(EventOccurrence(e, from), EventOccurrence(e, from.plusDays(7))) })
    override fun event(id: String): Flow<Event?> = flowOf(null)
}

private suspend fun ReceiveTurbine<TiketUiState>.loaded(): TiketUiState {
    var item = awaitItem()
    while (item.isLoading) item = awaitItem()
    return item
}

@OptIn(ExperimentalCoroutinesApi::class)
class TiketViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-20T03:00:00Z"), ZoneId.of("Asia/Jakarta"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(places: Flow<List<Place>>, events: List<Event> = emptyList()) =
        TiketViewModel(FakePlaces(places), FakeEvents(events), clock)

    @Test
    fun `a price with a check date is shown from the lowest checked tier`() = runTest {
        val vm = viewModel(flowOf(listOf(place("a", tickets = listOf(tier(150_000, Checked), tier(50_000, Checked))))))
        vm.uiState.test {
            val item = loaded().items.single()
            assertEquals(50_000L, item.priceFromIdr)
            assertEquals(Checked, item.checkedAt)
            assertEquals("tiket.example.org", item.sellerHost)
        }
    }

    @Test
    fun `a price without a check date is never carried and stays sedang dicek`() = runTest {
        val vm = viewModel(
            flowOf(listOf(place("a", tickets = listOf(tier(150_000, null)))))
        )
        vm.uiState.test {
            val item = loaded().items.single()
            assertNull(item.priceFromIdr)
            assertNull(item.checkedAt)
            // The row can still lead to the official seller.
            assertEquals("https://tiket.example.org/150000", item.sellerUrl)
        }
    }

    @Test
    fun `an unchecked tier does not undercut a checked one`() = runTest {
        val vm = viewModel(flowOf(listOf(place("a", tickets = listOf(tier(10_000, null), tier(90_000, Checked))))))
        vm.uiState.test { assertEquals(90_000L, loaded().items.single().priceFromIdr) }
    }

    @Test
    fun `a non https seller is dropped`() = runTest {
        val vm = viewModel(flowOf(listOf(place("a", tickets = listOf(tier(1_000, Checked, "http://x.example.org"))))))
        vm.uiState.test { assertNull(loaded().items.single().sellerUrl) }
    }

    @Test
    fun `places without tickets are skipped and the list can be empty`() = runTest {
        val vm = viewModel(flowOf(listOf(place("a", tickets = emptyList()))))
        vm.uiState.test {
            val state = loaded()
            assertTrue(state.items.isEmpty())
            assertTrue(state.isEmpty)
            assertFalse(state.isError)
        }
    }

    @Test
    fun `a repository failure gives the error state and retry reloads`() = runTest {
        var calls = 0
        val source = flow<List<Place>> {
            if (calls++ == 0) throw IllegalStateException("no signal")
            emit(listOf(place("a", tickets = listOf(tier(25_000, Checked)))))
        }
        val vm = viewModel(source)
        vm.uiState.test {
            val failed = loaded()
            assertTrue(failed.isError)
            assertFalse(failed.isEmpty)
            vm.retry()
            var next = awaitItem()
            while (next.isLoading || next.isError) next = awaitItem()
            assertEquals(1, next.items.size)
        }
    }

    @Test
    fun `shows are listed once each, craft places are classes, and filters narrow the list`() = runTest {
        val vm = viewModel(
            flowOf(
                listOf(
                    place("a", tickets = listOf(tier(1_000, Checked))),
                    place("b", PlaceCategory.Kerajinan, listOf(tier(90_000, Checked))),
                ),
            ),
            events = listOf(show("e1", 150_000, Checked), show("e2", 150_000, null)),
        )
        vm.uiState.test {
            val all = loaded()
            assertEquals(4, all.items.size)
            val e2 = all.items.single { it.id == "pentas-e2" }
            assertNull(e2.priceFromIdr)
            assertNull(e2.checkedAt)
            vm.selectFilter(TiketKind.Kelas)
            var state = awaitItem()
            while (state.filter != TiketKind.Kelas) state = awaitItem()
            assertEquals(listOf("tempat-b"), state.items.map { it.id })
            vm.selectFilter(TiketKind.Pentas)
            state = awaitItem()
            while (state.filter != TiketKind.Pentas) state = awaitItem()
            assertEquals(listOf("pentas-e1", "pentas-e2"), state.items.map { it.id })
            vm.selectFilter(null)
            state = awaitItem()
            while (state.filter != null) state = awaitItem()
            assertEquals(4, state.items.size)
        }
    }
}

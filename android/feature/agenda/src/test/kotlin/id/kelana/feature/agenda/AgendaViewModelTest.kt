package id.kelana.feature.agenda

import app.cash.turbine.test
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTag
import id.kelana.core.model.EventTicket
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.model.Region
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.RegionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

internal val TestChecked: LocalDate = LocalDate.of(2026, 11, 20)

internal fun testEvent(
    id: String,
    weekday: DayOfWeek,
    start: LocalTime,
    category: PlaceCategory = PlaceCategory.Tari,
    checkedAt: LocalDate? = TestChecked,
    ticketUrl: String? = "https://tiket.example.org/x",
    ticketChecked: LocalDate? = TestChecked,
    price: Long? = 150_000,
    tags: Set<EventTag> = emptySet(),
    performers: List<String> = emptyList(),
    arrivalAdviceMin: Int? = null,
    arrivalAdviceMax: Int? = null,
) = Event(
    id = id,
    placeId = "p",
    placeName = "Tempat $id",
    title = "Judul $id",
    description = "Kisah $id",
    category = category,
    schedule = OfficialFact(
        EventSchedule(Recurrence.Weekly(weekday), start, start.plusHours(1)),
        "https://jadwal.example.org/a",
        checkedAt,
    ),
    ticket = OfficialFact(EventTicket(price, ticketUrl), ticketUrl, ticketChecked),
    performers = performers,
    arrivalAdviceMin = arrivalAdviceMin,
    arrivalAdviceMax = arrivalAdviceMax,
    tags = tags,
)

/** Weekly events resolved against a date range, like the fake in :core:data. */
internal class TestEventRepository(private val all: List<Event>) : EventRepository {
    override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> =
        flowOf(all.filter { it.occursOn(date) }.map { EventOccurrence(it, date) })

    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        flowOf(
            all.flatMap { e -> e.occurrencesBetween(from, to).map { EventOccurrence(e, it) } }
                .sortedWith(compareBy({ it.date }, { it.event.schedule.value.start })),
        )

    override fun event(id: String): Flow<Event?> = flowOf(all.firstOrNull { it.id == id })
}

private class TestRegionRepository : RegionRepository {
    override fun regions(): Flow<List<Region>> =
        flowOf(listOf(Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1)))

    override val selectedRegionId = MutableStateFlow<String?>("DIY")

    override suspend fun select(regionId: String) {
        selectedRegionId.value = regionId
    }
}

/** Skips the initial loading state. */
private suspend fun app.cash.turbine.ReceiveTurbine<AgendaUiState>.loaded(): AgendaUiState {
    var item = awaitItem()
    while (item.isLoading) item = awaitItem()
    return item
}

/** Waits for the next state that satisfies [predicate]. */
private suspend fun app.cash.turbine.ReceiveTurbine<AgendaUiState>.until(predicate: (AgendaUiState) -> Boolean): AgendaUiState {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {
    // Friday 20 November 2026.
    private val clock = Clock.fixed(Instant.parse("2026-11-20T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val friday = LocalDate.of(2026, 11, 20)
    private val saturday = friday.plusDays(1)

    private val roro = testEvent("roro", DayOfWeek.FRIDAY, LocalTime.of(19, 30), tags = setOf(EventTag.Tari))
    private val fridayMorning =
        testEvent("pagi", DayOfWeek.FRIDAY, LocalTime.of(9, 0), PlaceCategory.SeniMusik, tags = setOf(EventTag.Wayang))
    private val macapat = testEvent("macapat", DayOfWeek.SATURDAY, LocalTime.of(9, 0), PlaceCategory.SeniMusik)
    private val unchecked = testEvent("belum", DayOfWeek.SUNDAY, LocalTime.of(10, 0), checkedAt = null)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg events: Event) =
        AgendaViewModel(TestEventRepository(events.toList()), TestRegionRepository(), clock)

    @Test
    fun `default date is today and the tonight card is the first evening event`() = runTest {
        val vm = viewModel(roro, fridayMorning, macapat)
        vm.uiState.test {
            val state = loaded()
            assertEquals(friday, state.selectedDate)
            assertEquals("roro", state.tonight?.event?.id)
            assertEquals(listOf("pagi"), state.alsoToday.map { it.event.id })
            assertEquals("DI Yogyakarta", state.regionName)
            assertEquals(14, state.days.size)
        }
    }

    @Test
    fun `date strip marks dates that have events and unchecked events are left out`() = runTest {
        val vm = viewModel(roro, macapat, unchecked)
        vm.uiState.test {
            val state = loaded()
            val withDot = state.days.filter { it.hasEvents }.map { it.date }
            assertTrue(friday in withDot)
            assertTrue(saturday in withDot)
            // Sunday 22 November has only the event without a check date.
            assertFalse(friday.plusDays(2) in withDot)
            assertTrue(state.upcoming.flatMap { it.items }.none { it.event.id == "belum" })
        }
    }

    @Test
    fun `selecting a date shows that day and later days as sections`() = runTest {
        val vm = viewModel(roro, macapat)
        vm.uiState.test {
            loaded()
            vm.selectDate(saturday)
            val state = until { it.selectedDate == saturday }
            assertEquals(saturday, state.selectedDate)
            assertNull(state.tonight)
            assertEquals(listOf("macapat"), state.alsoToday.map { it.event.id })
            // Friday 27 and Saturday 28 November follow, in date order.
            assertEquals(listOf(friday.plusDays(7), friday.plusDays(8)), state.upcoming.map { it.date })
        }
    }

    @Test
    fun `a date outside the strip is ignored`() = runTest {
        val vm = viewModel(roro)
        vm.uiState.test {
            assertEquals(friday, loaded().selectedDate)
            vm.selectDate(friday.minusDays(1))
            vm.selectDate(friday.plusDays(14))
            expectNoEvents()
        }
    }

    @Test
    fun `filter keeps one tag and Semua clears it`() = runTest {
        val vm = viewModel(roro, fridayMorning)
        vm.uiState.test {
            // Chips come from EventTag, not from place categories, and are always all three.
            assertEquals(listOf(EventTag.Tari, EventTag.Wayang, EventTag.RamahAnak), loaded().filters)
            vm.selectFilter(EventTag.Wayang)
            val filtered = until { it.selectedFilter == EventTag.Wayang }
            assertNull(filtered.tonight)
            assertEquals(listOf("pagi"), filtered.alsoToday.map { it.event.id })
            // Chips stay, so the user can go back.
            assertEquals(3, filtered.filters.size)
            vm.selectFilter(null)
            val all = until { it.selectedFilter == null }
            assertEquals("roro", all.tonight?.event?.id)
            assertEquals(listOf("pagi"), all.alsoToday.map { it.event.id })
        }
    }

    @Test
    fun `a tag without events gives an empty day and untagged events are only under Semua`() = runTest {
        val untagged = testEvent("polos", DayOfWeek.FRIDAY, LocalTime.of(10, 0))
        val vm = viewModel(roro, untagged)
        vm.uiState.test {
            loaded()
            vm.selectFilter(EventTag.RamahAnak)
            assertTrue(until { it.selectedFilter == EventTag.RamahAnak }.isSelectedDayEmpty)
            vm.selectFilter(EventTag.Tari)
            val tari = until { it.selectedFilter == EventTag.Tari }
            assertEquals("roro", tari.tonight?.event?.id)
            assertTrue(tari.alsoToday.isEmpty())
            vm.selectFilter(null)
            assertEquals(listOf("polos"), until { it.selectedFilter == null }.alsoToday.map { it.event.id })
        }
    }

    @Test
    fun `a date with no events is empty`() = runTest {
        val vm = viewModel(roro)
        vm.uiState.test {
            loaded()
            vm.selectDate(saturday)
            val state = until { it.selectedDate == saturday }
            assertTrue(state.isSelectedDayEmpty)
            assertFalse(state.days.first { it.date == saturday }.hasEvents)
        }
    }

    @Test
    fun `no events at all is fully empty`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val state = loaded()
            assertTrue(state.isEmpty)
            assertEquals(EventTag.entries, state.filters)
        }
    }
}

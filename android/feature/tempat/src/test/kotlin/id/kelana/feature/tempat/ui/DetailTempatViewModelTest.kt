package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
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
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: PlaceRepository) =
        DetailTempatViewModel(SavedStateHandle(mapOf("placeId" to "p1")), repo)

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
}

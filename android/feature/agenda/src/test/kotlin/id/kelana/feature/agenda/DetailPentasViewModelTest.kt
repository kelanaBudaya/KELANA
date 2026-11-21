package id.kelana.feature.agenda

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DetailPentasViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val roro = testEvent("roro", DayOfWeek.FRIDAY, LocalTime.of(19, 30))

    private fun viewModel(id: String, vararg events: id.kelana.core.model.Event) =
        DetailPentasViewModel(TestEventRepository(events.toList()), id)

    private suspend fun app.cash.turbine.ReceiveTurbine<DetailPentasUiState>.settled(): DetailPentasUiState {
        val first = awaitItem()
        return if (first == DetailPentasUiState.Loading) awaitItem() else first
    }

    @Test
    fun `found event carries source note and a ticket url`() = runTest {
        viewModel("roro", roro).uiState.test {
            val state = settled() as DetailPentasUiState.Found
            assertEquals("roro", state.event.id)
            assertEquals(SourceNote("jadwal.example.org", TestChecked), state.sourceNote)
            assertEquals("https://tiket.example.org/x", state.ticketUrl)
        }
    }

    @Test
    fun `unknown id is not found`() = runTest {
        viewModel("nope", roro).uiState.test {
            assertEquals(DetailPentasUiState.NotFound, settled())
        }
    }

    @Test
    fun `schedule without checkedAt has no source note`() = runTest {
        val event = testEvent("x", DayOfWeek.FRIDAY, LocalTime.of(19, 30), checkedAt = null)
        viewModel("x", event).uiState.test {
            val state = settled() as DetailPentasUiState.Found
            assertNull(state.event.schedule.checkedAt)
            assertNull(state.sourceNote)
        }
    }

    @Test
    fun `ticket without checkedAt or with a non https url offers no ticket button`() = runTest {
        val unchecked = testEvent("a", DayOfWeek.FRIDAY, LocalTime.of(19, 30), ticketChecked = null)
        val http = testEvent("b", DayOfWeek.FRIDAY, LocalTime.of(19, 30), ticketUrl = "http://tiket.example.org/x")
        val none = testEvent("c", DayOfWeek.FRIDAY, LocalTime.of(19, 30), ticketUrl = null, price = null)
        val script = testEvent("d", DayOfWeek.FRIDAY, LocalTime.of(19, 30), ticketUrl = "javascript:alert(1)")
        listOf(unchecked, http, none, script).forEach { event ->
            viewModel(event.id, event).uiState.test {
                assertNull(event.id, (settled() as DetailPentasUiState.Found).ticketUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `performers and arrival advice reach the detail state`() = runTest {
        val event = testEvent(
            "r", DayOfWeek.FRIDAY, LocalTime.of(19, 30),
            performers = listOf("Lebih dari 200 penari dan pemusik lokal"),
            arrivalAdviceMin = 30, arrivalAdviceMax = 45,
        )
        viewModel("r", event).uiState.test {
            val found = settled() as DetailPentasUiState.Found
            assertEquals(listOf("Lebih dari 200 penari dan pemusik lokal"), found.event.performers)
            assertEquals(
                ArrivalAdvice.Range(30, 45),
                arrivalAdvice(found.event.arrivalAdviceMin, found.event.arrivalAdviceMax),
            )
        }
    }

    @Test
    fun `arrival advice covers range, equal bounds, one bound and none`() {
        assertEquals(ArrivalAdvice.Range(30, 45), arrivalAdvice(30, 45))
        assertEquals(ArrivalAdvice.About(30), arrivalAdvice(30, 30))
        assertEquals(ArrivalAdvice.About(20), arrivalAdvice(20, null))
        assertEquals(ArrivalAdvice.About(20), arrivalAdvice(null, 20))
        assertNull(arrivalAdvice(null, null))
        assertNull(arrivalAdvice(0, 0))
    }

    @Test
    fun `httpsHost only accepts https`() {
        assertEquals("a.example.org", httpsHost("https://a.example.org/p?q=1"))
        assertNull(httpsHost("http://a.example.org"))
        assertNull(httpsHost("intent://a.example.org"))
        assertNull(httpsHost(null))
        assertTrue(httpsHost("not a url") == null)
    }
}

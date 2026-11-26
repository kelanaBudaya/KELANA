package id.kelana.core.ui.photo

import app.cash.turbine.test
import id.kelana.core.model.PlaceCoverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceCoversViewModelTest {
    private class Fake(val emissions: List<Map<String, String>>) : PlaceCoverRepository {
        override fun covers(): Flow<Map<String, String>> = flowOf(*emissions.toTypedArray())
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun starts_empty_then_exposes_covers() = runTest {
        val vm = PlaceCoversViewModel(Fake(listOf(emptyMap(), mapOf("p1" to "https://x/p1_t.jpg"))))
        vm.covers.test {
            assertEquals(mapOf("p1" to "https://x/p1_t.jpg"), expectMostRecentItem())
        }
    }

    @Test
    fun no_photos_means_no_covers() = runTest {
        val vm = PlaceCoversViewModel(Fake(listOf(emptyMap())))
        vm.covers.test { assertEquals(emptyMap<String, String>(), expectMostRecentItem()) }
    }
}

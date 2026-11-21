package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
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
class FotoResmiViewModelTest {
    private class FakePlaces(private val place: Place?) : PlaceRepository {
        override fun place(id: String): Flow<Place?> = flowOf(place)
        override fun places(): Flow<List<Place>> = flowOf(listOfNotNull(place))
        override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loads_placeholder_photos_for_a_place() = runTest {
        val place = Place("p1", "p1", "Museum Uji", PlaceCategory.Museum, "YK-02", 0.0, 0.0, "a", "d", "cover-p1")
        FotoResmiViewModel(SavedStateHandle(mapOf("placeId" to "p1")), FakePlaces(place)).uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.Loaded, item.status)
            assertEquals("cover-p1", item.coverKey)
            assertEquals(4, item.photoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknown_place_is_not_found() = runTest {
        FotoResmiViewModel(SavedStateHandle(mapOf("placeId" to "x")), FakePlaces(null)).uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.NotFound, item.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

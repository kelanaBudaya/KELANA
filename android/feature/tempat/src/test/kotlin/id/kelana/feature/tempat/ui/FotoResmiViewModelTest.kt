package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlacePhoto
import id.kelana.core.model.PlacePhotoRepository
import id.kelana.core.model.PlaceRepository
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

    private class FakePhotos(private val list: List<PlacePhoto> = emptyList(), private val fail: Boolean = false) : PlacePhotoRepository {
        override fun photos(placeId: String): Flow<List<PlacePhoto>> =
            if (fail) flow { error("boom") } else flowOf(list)
    }

    private val place = Place("p1", "p1", "Museum Uji", PlaceCategory.Museum, "YK-02", 0.0, 0.0, "a", "d", "cover-p1")

    private fun vm(places: PlaceRepository, photos: PlacePhotoRepository) =
        FotoResmiViewModel(SavedStateHandle(mapOf("placeId" to "p1")), places, photos)

    private suspend fun app.cash.turbine.ReceiveTurbine<FotoResmiUiState>.awaitPhotosDone(): FotoResmiUiState {
        var item = awaitItem()
        while (item.status == PlaceLoadStatus.Loading || item.photoStatus == PhotoStatus.Loading) item = awaitItem()
        return item
    }

    @Test
    fun loads_photos_for_a_place() = runTest {
        vm(FakePlaces(place), FakePhotos((1..3).map { testPhoto(it) })).uiState.test {
            val item = awaitPhotosDone()
            assertEquals(PlaceLoadStatus.Loaded, item.status)
            assertEquals(PhotoStatus.Loaded, item.photoStatus)
            assertEquals(3, item.photoCount)
            assertEquals("Museum Uji", item.placeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun no_photos_is_empty() = runTest {
        vm(FakePlaces(place), FakePhotos()).uiState.test {
            val item = awaitPhotosDone()
            assertEquals(PlaceLoadStatus.Loaded, item.status)
            assertEquals(PhotoStatus.Empty, item.photoStatus)
            assertEquals(0, item.photoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun photo_failure_is_error_state_with_place_kept() = runTest {
        vm(FakePlaces(place), FakePhotos(fail = true)).uiState.test {
            val item = awaitPhotosDone()
            assertEquals(PlaceLoadStatus.Loaded, item.status)
            assertEquals(PhotoStatus.Error, item.photoStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknown_place_is_not_found() = runTest {
        FotoResmiViewModel(SavedStateHandle(mapOf("placeId" to "x")), FakePlaces(null), FakePhotos()).uiState.test {
            var item = awaitItem()
            while (item.status == PlaceLoadStatus.Loading) item = awaitItem()
            assertEquals(PlaceLoadStatus.NotFound, item.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

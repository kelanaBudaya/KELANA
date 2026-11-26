package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.PlaceVideo
import id.kelana.core.model.PlaceVideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceVideosViewModelTest {
    private val video = PlaceVideo("v1", "p1", "https://www.tiktok.com/@a/video/123", "123", "Pentas")

    private class FakeRepo(private val rows: List<PlaceVideo>, private val failures: Int = 0) : PlaceVideoRepository {
        var calls = 0
        override fun videos(placeId: String): Flow<List<PlaceVideo>> = flow {
            calls++
            if (calls <= failures) error("boom")
            emit(rows)
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: PlaceVideoRepository) =
        PlaceVideosViewModel(SavedStateHandle(mapOf("placeId" to "p1")), repo)

    private suspend fun app.cash.turbine.ReceiveTurbine<PlaceVideosState>.settled(): PlaceVideosState {
        var item = awaitItem()
        while (item == PlaceVideosState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun videos_are_listed() = runTest {
        vm(FakeRepo(listOf(video))).state.test {
            assertEquals(PlaceVideosState.Videos(listOf(video)), settled())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun no_videos_is_empty() = runTest {
        vm(FakeRepo(emptyList())).state.test {
            assertEquals(PlaceVideosState.Empty, settled())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failure_is_error_and_retry_recovers() = runTest {
        val vm = vm(FakeRepo(listOf(video), failures = 1))
        vm.state.test {
            assertEquals(PlaceVideosState.Error, settled())
            vm.retry()
            assertEquals(PlaceVideosState.Videos(listOf(video)), settled())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

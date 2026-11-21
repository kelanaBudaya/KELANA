package id.kelana.feature.cerita.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryDetailViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(session: FakeSession, stories: CountingStories, id: String = "a") =
        StoryDetailViewModel(SavedStateHandle(mapOf(StoryDetailViewModel.ARG_STORY_ID to id)), session, stories)

    private suspend fun app.cash.turbine.ReceiveTurbine<StoryDetailUiState>.settled(): StoryDetailUiState {
        var item = awaitItem()
        while (item == StoryDetailUiState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun guest_neverRequestsTheStory() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        vm(FakeSession(SessionState.Guest), stories).uiState.test {
            assertEquals(StoryDetailUiState.Guest, settled())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, stories.subscriptions)
    }

    @Test
    fun postcard_isExposedAsPostcard() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        vm(FakeSession(SignedIn), stories).uiState.test {
            val state = settled()
            assertTrue(state is StoryDetailUiState.Postcard)
            assertEquals("a", (state as StoryDetailUiState.Postcard).story.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun video_isExposedAsVideo() = runTest {
        val stories = CountingStories(listOf(video("a", "2026-11-20T10:00:00Z")))
        vm(FakeSession(SignedIn), stories).uiState.test {
            assertTrue(settled() is StoryDetailUiState.Video)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownId_isNotFound() = runTest {
        vm(FakeSession(SignedIn), CountingStories(), id = "missing").uiState.test {
            assertEquals(StoryDetailUiState.NotFound, settled())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failure_isError_thenRetryLoadsTheStory() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        stories.failNext = true
        val vm = vm(FakeSession(SignedIn), stories)
        vm.uiState.test {
            assertEquals(StoryDetailUiState.Error, settled())
            vm.retry()
            assertTrue(settled() is StoryDetailUiState.Postcard)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

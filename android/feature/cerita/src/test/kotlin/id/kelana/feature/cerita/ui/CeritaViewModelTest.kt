package id.kelana.feature.cerita.ui

import app.cash.turbine.test
import id.kelana.core.model.AuthResult
import id.kelana.core.model.PostcardStory
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import id.kelana.core.model.Story
import id.kelana.core.model.StoryRepository
import id.kelana.core.model.VideoStory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

internal class FakeSession(initial: SessionState) : SessionRepository {
    val state = MutableStateFlow(initial)
    override val sessionState: Flow<SessionState> = state
    override suspend fun signInWithGoogle(idToken: String, nonce: String) = AuthResult.Success
    override suspend fun signOut() = Unit
}

internal class CountingStories(initial: List<Story> = emptyList()) : StoryRepository {
    val all = MutableStateFlow(initial)
    var failNext = false
    var subscriptions = 0

    override fun stories(): Flow<List<Story>> = flow {
        subscriptions++
        if (failNext) {
            failNext = false
            error("network")
        }
        all.collect { emit(it) }
    }

    override fun story(id: String): Flow<Story?> = flow {
        subscriptions++
        if (failNext) {
            failNext = false
            error("network")
        }
        all.collect { list -> emit(list.firstOrNull { it.id == id }) }
    }
}

internal val SignedIn = SessionState.SignedIn(SessionUser("u1", null, "Uji"))

internal fun postcard(id: String, at: String) = PostcardStory(
    id = id, placeId = "p", placeName = "Tempat Uji", authorName = "Penulis Uji", createdAt = Instant.parse(at),
)

internal fun video(id: String, at: String) = VideoStory(
    id = id, placeId = "p", placeName = "Tempat Uji", authorName = "Penulis Uji", createdAt = Instant.parse(at),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CeritaViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun guest_getsGuestState_andStoriesAreNeverRequested() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        val vm = CeritaViewModel(FakeSession(SessionState.Guest), stories)
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Guest, item.status)
            assertTrue(item.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, stories.subscriptions)
    }

    @Test
    fun sessionLoading_staysLoading() = runTest {
        val stories = CountingStories()
        val vm = CeritaViewModel(FakeSession(SessionState.Loading), stories)
        vm.uiState.test {
            assertEquals(FeedStatus.Loading, awaitItem().status)
            expectNoEvents()
        }
        assertEquals(0, stories.subscriptions)
    }

    @Test
    fun signedIn_withNoStories_isEmpty() = runTest {
        val vm = CeritaViewModel(FakeSession(SignedIn), CountingStories())
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Empty, item.status)
            assertTrue(item.feedIsEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signedIn_showsNewestFirst_withCounts() = runTest {
        val stories = CountingStories(
            listOf(
                postcard("old", "2026-11-18T10:00:00Z"),
                video("newest", "2026-11-20T10:00:00Z"),
                postcard("mid", "2026-11-19T10:00:00Z"),
            ),
        )
        val vm = CeritaViewModel(FakeSession(SignedIn), stories)
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Content, item.status)
            assertEquals(listOf("newest", "mid", "old"), item.items.map { it.id })
            assertEquals(2, item.postcardCount)
            assertEquals(1, item.videoCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filter_keepsOnlyThatKind_andEmptyFilterIsNotAnEmptyFeed() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        val vm = CeritaViewModel(FakeSession(SignedIn), stories)
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Content, item.status)
            vm.select(StoryFilter.Video)
            item = awaitItem()
            assertEquals(FeedStatus.Empty, item.status)
            assertEquals(StoryFilter.Video, item.filter)
            assertFalse(item.feedIsEmpty)
            vm.select(StoryFilter.Postcard)
            assertEquals(listOf("a"), awaitItem().items.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun liveInsert_appearsAtTheTop() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-19T10:00:00Z")))
        val vm = CeritaViewModel(FakeSession(SignedIn), stories)
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(listOf("a"), item.items.map { it.id })
            stories.all.value = stories.all.value + video("b", "2026-11-20T10:00:00Z")
            assertEquals(listOf("b", "a"), awaitItem().items.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failure_showsError_thenRetryRecovers() = runTest {
        val stories = CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z")))
        stories.failNext = true
        val vm = CeritaViewModel(FakeSession(SignedIn), stories)
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Error, item.status)
            vm.retry()
            var after = awaitItem()
            while (after.status == FeedStatus.Loading) after = awaitItem()
            assertEquals(FeedStatus.Content, after.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signingOut_turnsTheFeedIntoGuest() = runTest {
        val session = FakeSession(SignedIn)
        val vm = CeritaViewModel(session, CountingStories(listOf(postcard("a", "2026-11-20T10:00:00Z"))))
        vm.uiState.test {
            var item = awaitItem()
            if (item.status == FeedStatus.Loading) item = awaitItem()
            assertEquals(FeedStatus.Content, item.status)
            session.state.value = SessionState.Guest
            item = awaitItem()
            assertEquals(FeedStatus.Guest, item.status)
            assertTrue(item.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

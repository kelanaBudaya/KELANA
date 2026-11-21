package id.kelana.core.ui.session

import app.cash.turbine.test
import id.kelana.core.model.AuthResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTest {
    private class FakeSessionRepository : SessionRepository {
        val state = MutableStateFlow<SessionState>(SessionState.Loading)
        override val sessionState: Flow<SessionState> = state
        override suspend fun signInWithGoogle(idToken: String, nonce: String) = AuthResult.Success
        override suspend fun signOut() = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun decideLoginAction_coversEveryState() {
        val user = SessionUser("1", "a@b.c", "A")
        assertEquals(LoginAction.Run, decideLoginAction(SessionState.SignedIn(user)))
        assertEquals(LoginAction.GoToLogin, decideLoginAction(SessionState.Guest))
        assertEquals(LoginAction.Wait, decideLoginAction(SessionState.Loading))
    }

    @Test
    fun sessionViewModel_startsLoadingThenFollowsTheRepository() = runTest {
        val repo = FakeSessionRepository()
        val viewModel = SessionViewModel(repo)
        viewModel.sessionState.test {
            assertEquals(SessionState.Loading, awaitItem())
            repo.state.value = SessionState.Guest
            assertEquals(SessionState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

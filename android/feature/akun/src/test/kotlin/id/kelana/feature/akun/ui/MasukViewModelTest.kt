package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
import id.kelana.core.model.AuthConfig
import id.kelana.core.model.AuthResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.feature.akun.domain.GoogleTokenResult
import id.kelana.feature.akun.domain.Nonce
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MasukViewModelTest {
    private class FakeSessionRepository(var result: AuthResult = AuthResult.Success) : SessionRepository {
        var lastIdToken: String? = null
        var lastNonce: String? = null
        var calls = 0
        override val sessionState: Flow<SessionState> = flowOf(SessionState.Guest)
        override suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult {
            calls++
            lastIdToken = idToken
            lastNonce = nonce
            return result
        }
        override suspend fun signOut() = Unit
    }

    private class FakeAppLanguageRepository(initial: AppLanguage = AppLanguage.ID) : AppLanguageRepository {
        val current = MutableStateFlow(initial)
        override val language: Flow<AppLanguage> = current
        override fun set(language: AppLanguage) {
            current.value = language
        }
    }

    private val languages = FakeAppLanguageRepository()
    private val configured = AuthConfig(isSupabaseConfigured = true, googleWebClientId = "web-client-id")
    private val repo = FakeSessionRepository()

    private fun viewModel(config: AuthConfig = configured) =
        MasukViewModel(repo, config, languages) { "fixed-nonce" }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun success_requestsTokenWithHashedNonce_thenSendsRawNonceToSupabase() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onGoogleClick()
            assertEquals(MasukUiState(isLoading = true), vm.state.value)
            assertEquals(
                MasukEvent.RequestGoogleToken(Nonce.sha256Hex("fixed-nonce"), "web-client-id"),
                awaitItem(),
            )

            vm.onGoogleResult(GoogleTokenResult.Token("google-id-token"))

            assertEquals(MasukEvent.SignedIn, awaitItem())
            assertEquals("google-id-token", repo.lastIdToken)
            assertEquals("fixed-nonce", repo.lastNonce)
            assertEquals(MasukUiState(), vm.state.value)
        }
    }

    @Test
    fun cancel_showsCancelledAndStopsLoading() = runTest {
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.Cancelled)
        assertEquals(MasukUiState(isLoading = false, error = MasukError.Cancelled), vm.state.value)
        assertEquals(0, repo.calls)
    }

    @Test
    fun noGoogleAccount_showsNoAccountError() = runTest {
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.NoAccount)
        assertEquals(MasukError.NoGoogleAccount, vm.state.value.error)
    }

    @Test
    fun credentialFailure_showsGenericError() = runTest {
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.Failure)
        assertEquals(MasukError.Generic, vm.state.value.error)
    }

    @Test
    fun networkError_fromSupabase_showsNetworkError() = runTest {
        repo.result = AuthResult.NetworkError
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.Token("t"))
        assertEquals(MasukUiState(error = MasukError.Network), vm.state.value)
    }

    @Test
    fun failedExchange_showsGenericError() = runTest {
        repo.result = AuthResult.Failed
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.Token("t"))
        assertEquals(MasukError.Generic, vm.state.value.error)
    }

    @Test
    fun notConfigured_neverOpensGoogleAndShowsError() = runTest {
        val vm = viewModel(AuthConfig(isSupabaseConfigured = false, googleWebClientId = "web-client-id"))
        vm.events.test {
            vm.onGoogleClick()
            assertEquals(MasukUiState(error = MasukError.NotConfigured), vm.state.value)
            expectNoEvents()
        }
        assertEquals(0, repo.calls)
    }

    @Test
    fun repositoryReportsNotConfigured_showsNotConfiguredError() = runTest {
        repo.result = AuthResult.NotConfigured
        val vm = viewModel()
        vm.onGoogleClick()
        vm.onGoogleResult(GoogleTokenResult.Token("t"))
        assertEquals(MasukError.NotConfigured, vm.state.value.error)
    }

    @Test
    fun guestClick_emitsContinueAsGuest() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onGuestClick()
            assertEquals(MasukEvent.ContinueAsGuest, awaitItem())
        }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun language_followsRepositoryAndSurvivesStateResets() = runTest {
        val vm = viewModel()
        assertEquals(AppLanguage.ID, vm.language.value)
        vm.onLanguageSelected(AppLanguage.EN)
        assertEquals(AppLanguage.EN, vm.language.value)
        assertEquals(AppLanguage.EN, languages.current.value)
        vm.onGoogleClick() // replaces the whole login state
        assertEquals(AppLanguage.EN, vm.language.value)
    }
}

package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilViewModelTest {
    private class FakeSessionRepository(initial: SessionState) : SessionRepository {
        val session = MutableStateFlow(initial)
        var signOutCalls = 0
        override val sessionState = session
        override suspend fun signInWithGoogle(idToken: String, nonce: String) = AuthResult.Success
        override suspend fun signOut() {
            signOutCalls++
            session.value = SessionState.Guest
        }
    }

    private class FakeAppLanguageRepository(initial: AppLanguage = AppLanguage.ID) : AppLanguageRepository {
        val current = MutableStateFlow(initial)
        override val language: Flow<AppLanguage> = current
        override fun set(language: AppLanguage) {
            current.value = language
        }
    }

    private val user = SessionUser(id = "u1", email = "nama@contoh.id", displayName = "Muhammad Haikal")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun signedIn_showsRealNameEmailAndInitials() {
        val vm = ProfilViewModel(FakeSessionRepository(SessionState.SignedIn(user)), FakeAppLanguageRepository())
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isGuest)
        assertEquals("Muhammad Haikal", state.name)
        assertEquals("nama@contoh.id", state.email)
        assertEquals("MH", state.initials)
    }

    @Test
    fun signedInWithoutDisplayName_fallsBackToEmailAndHidesDuplicateLine() {
        val vm = ProfilViewModel(FakeSessionRepository(SessionState.SignedIn(user.copy(displayName = null))), FakeAppLanguageRepository())
        assertEquals("nama@contoh.id", vm.state.value.name)
        assertNull(vm.state.value.email)
    }

    @Test
    fun loading_isLoadingAndNotGuest() {
        val vm = ProfilViewModel(FakeSessionRepository(SessionState.Loading), FakeAppLanguageRepository())
        assertTrue(vm.state.value.isLoading)
        assertFalse(vm.state.value.isGuest)
    }

    @Test
    fun guest_hasNoUserAndSignOutDoesNothing() = runTest {
        val repo = FakeSessionRepository(SessionState.Guest)
        val vm = ProfilViewModel(repo, FakeAppLanguageRepository())
        assertTrue(vm.state.value.isGuest)
        assertNull(vm.state.value.user)
        vm.onSignOut()
        assertEquals(0, repo.signOutCalls)
    }

    @Test
    fun signOut_callsRepositoryOnce_emitsSignedOut_andKeepsProfileUntilNavigation() = runTest {
        val repo = FakeSessionRepository(SessionState.SignedIn(user))
        val vm = ProfilViewModel(repo, FakeAppLanguageRepository())
        vm.events.test {
            vm.onSignOut()
            vm.onSignOut() // a second tap while signing out is ignored
            assertEquals(ProfilEvent.SignedOut, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, repo.signOutCalls)
        assertEquals(SessionState.Guest, repo.session.value)
        assertTrue(vm.state.value.isSigningOut)
        assertEquals(user, vm.state.value.user)
    }

    @Test
    fun language_showsRememberedChoiceAndSetAppliesIt() = runTest {
        val languages = FakeAppLanguageRepository(AppLanguage.EN)
        val vm = ProfilViewModel(FakeSessionRepository(SessionState.Guest), languages)
        assertEquals(AppLanguage.EN, vm.state.value.language)
        vm.state.test {
            assertEquals(AppLanguage.EN, awaitItem().language)
            vm.onLanguageSelected(AppLanguage.ID)
            assertEquals(AppLanguage.ID, awaitItem().language)
        }
        assertEquals(AppLanguage.ID, languages.current.value)
    }
}

package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.AccountDeletionRepository
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HapusAkunViewModelTest {

    /**
     * Mirrors the real repository's contract: on Deleted it has cleared the local data and the session (the session
     * flips to Guest); on any other result nothing local changes.
     */
    private class FakeAccountDeletionRepository(
        var summary: AccountDeletionSummary = AccountDeletionSummary(3, 2, 1),
        var result: AccountDeletionResult = AccountDeletionResult.Deleted,
    ) : AccountDeletionRepository {
        val session = MutableStateFlow<SessionState>(SessionState.SignedIn(SessionUser("u1", "a@contoh.id", "A")))
        var localData = true
        var deleteCalls = 0
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun summary(): AccountDeletionSummary = summary

        override suspend fun deleteAccount(): AccountDeletionResult {
            deleteCalls++
            gate?.await()
            if (result == AccountDeletionResult.Deleted) {
                localData = false
                session.value = SessionState.Guest
            }
            return result
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsRealCounts() {
        val vm = HapusAkunViewModel(FakeAccountDeletionRepository())
        assertEquals(AccountDeletionSummary(3, 2, 1), vm.state.value.summary)
    }

    @Test
    fun unreadableCounts_stayNull() {
        val vm = HapusAkunViewModel(FakeAccountDeletionRepository(summary = AccountDeletionSummary()))
        assertNull(vm.state.value.summary.stampCount)
        assertNull(vm.state.value.summary.postcardCount)
        assertNull(vm.state.value.summary.videoCount)
    }

    @Test
    fun button_disabledUntilExactlyHapus() {
        val repo = FakeAccountDeletionRepository()
        val vm = HapusAkunViewModel(repo)
        assertFalse(vm.state.value.canDelete)
        for (text in listOf("", "H", "HAPU", "hapus", "Hapus", "HAPUS!", "HA PUS")) {
            vm.onConfirmationChange(text)
            assertFalse("'$text' must not enable", vm.state.value.canDelete)
            vm.onDelete()
        }
        assertEquals(0, repo.deleteCalls)
        vm.onConfirmationChange("  HAPUS ")
        assertTrue("surrounding spaces are trimmed", vm.state.value.canDelete)
        vm.onConfirmationChange("HAPUS")
        assertTrue(vm.state.value.canDelete)
    }

    @Test
    fun success_clearsSessionAndNavigates() = runTest {
        val repo = FakeAccountDeletionRepository()
        val vm = HapusAkunViewModel(repo)
        vm.events.test {
            vm.onConfirmationChange("HAPUS")
            vm.onDelete()
            assertEquals(HapusAkunEvent.Deleted, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, repo.deleteCalls)
        assertFalse(repo.localData)
        assertEquals(SessionState.Guest, repo.session.value)
        assertNull(vm.state.value.error)
    }

    @Test
    fun secondTapWhileDeleting_isIgnored_andInputIsLocked() = runTest {
        val repo = FakeAccountDeletionRepository().apply { gate = CompletableDeferred() }
        val vm = HapusAkunViewModel(repo)
        vm.onConfirmationChange("HAPUS")
        vm.onDelete()
        assertTrue(vm.state.value.isDeleting)
        assertFalse(vm.state.value.canDelete)
        vm.onDelete()
        vm.onConfirmationChange("")
        assertEquals("HAPUS", vm.state.value.confirmation)
        repo.gate!!.complete(Unit)
        assertEquals(1, repo.deleteCalls)
    }

    @Test
    fun failure_keepsEverything_andShowsError() = runTest {
        val cases = mapOf(
            AccountDeletionResult.NetworkError to HapusAkunError.Network,
            AccountDeletionResult.Failed to HapusAkunError.Failed,
            AccountDeletionResult.NotConfigured to HapusAkunError.NotConfigured,
            AccountDeletionResult.NotSignedIn to HapusAkunError.NotSignedIn,
        )
        for ((result, error) in cases) {
            val repo = FakeAccountDeletionRepository(result = result)
            val vm = HapusAkunViewModel(repo)
            vm.events.test {
                vm.onConfirmationChange("HAPUS")
                vm.onDelete()
                expectNoEvents()
            }
            val state = vm.state.value
            assertEquals(error, state.error)
            assertFalse(state.isDeleting)
            assertEquals("HAPUS", state.confirmation)
            assertTrue("retry stays one tap away", state.canDelete)
            assertTrue(repo.localData)
            assertTrue(repo.session.value is SessionState.SignedIn)
        }
    }

    @Test
    fun typingAfterAnError_clearsIt() {
        val vm = HapusAkunViewModel(FakeAccountDeletionRepository(result = AccountDeletionResult.Failed))
        vm.onConfirmationChange("HAPUS")
        vm.onDelete()
        assertEquals(HapusAkunError.Failed, vm.state.value.error)
        vm.onConfirmationChange("HAPU")
        assertNull(vm.state.value.error)
    }
}

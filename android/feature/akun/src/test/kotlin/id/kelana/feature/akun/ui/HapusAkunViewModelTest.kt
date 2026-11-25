package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.AccountDeletionRepository
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.core.model.AccountExportRepository
import id.kelana.core.model.AccountExportResult
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

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

    private class FakeExportRepository(
        var result: AccountExportResult = AccountExportResult.Ready(DOCUMENT),
        var saveOk: Boolean = true,
    ) : AccountExportRepository {
        var exportCalls = 0
        val saved = mutableListOf<Pair<String, String>>()
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun export(): AccountExportResult {
            exportCalls++
            gate?.await()
            return result
        }

        override suspend fun save(destination: String, document: String): Boolean {
            saved += destination to document
            return saveOk
        }
    }

    // 2026-11-23 23:30 UTC is already 24 November in Jakarta (UTC+7): the file name uses the local date.
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-23T23:30:00Z"), ZoneOffset.UTC)

    private fun viewModel(
        repo: FakeAccountDeletionRepository,
        export: FakeExportRepository = FakeExportRepository(),
    ) = HapusAkunViewModel(repo, export, clock) { ZoneId.of("Asia/Jakarta") }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsRealCounts() {
        val vm = viewModel(FakeAccountDeletionRepository())
        assertEquals(AccountDeletionSummary(3, 2, 1), vm.state.value.summary)
    }

    @Test
    fun unreadableCounts_stayNull() {
        val vm = viewModel(FakeAccountDeletionRepository(summary = AccountDeletionSummary()))
        assertNull(vm.state.value.summary.stampCount)
        assertNull(vm.state.value.summary.postcardCount)
        assertNull(vm.state.value.summary.videoCount)
    }

    @Test
    fun button_disabledUntilExactlyHapus() {
        val repo = FakeAccountDeletionRepository()
        val vm = viewModel(repo)
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
        val vm = viewModel(repo)
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
        val vm = viewModel(repo)
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
            val vm = viewModel(repo)
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
        val vm = viewModel(FakeAccountDeletionRepository(result = AccountDeletionResult.Failed))
        vm.onConfirmationChange("HAPUS")
        vm.onDelete()
        assertEquals(HapusAkunError.Failed, vm.state.value.error)
        vm.onConfirmationChange("HAPU")
        assertNull(vm.state.value.error)
    }

    @Test
    fun export_success_asksForTheFile_thenSaves() = runTest {
        val export = FakeExportRepository()
        val vm = viewModel(FakeAccountDeletionRepository(), export)
        vm.onConfirmationChange("HAPUS")
        vm.events.test {
            vm.onExport()
            assertEquals(HapusAkunEvent.ChooseExportFile("kelana-dataku-2026-11-24.json"), awaitItem())
        }
        assertEquals(DataExportState.Exporting, vm.state.value.export)
        assertFalse("no delete while exporting", vm.state.value.canDelete)
        vm.onExportDestination("content://docs/kelana-dataku.json")
        assertEquals(listOf("content://docs/kelana-dataku.json" to DOCUMENT), export.saved)
        assertEquals(DataExportState.Saved, vm.state.value.export)
        assertNull(vm.state.value.exportError)
        assertTrue(vm.state.value.canDelete)
    }

    @Test
    fun export_whileRunning_secondTapIgnored() = runTest {
        val export = FakeExportRepository().apply { gate = CompletableDeferred() }
        val vm = viewModel(FakeAccountDeletionRepository(), export)
        vm.onExport()
        vm.onExport()
        assertFalse(vm.state.value.canExport)
        export.gate!!.complete(Unit)
        assertEquals(1, export.exportCalls)
    }

    @Test
    fun export_errors_showError_andRetryWorks() = runTest {
        val cases = mapOf(
            AccountExportResult.NetworkError to DataExportError.Network,
            AccountExportResult.Failed to DataExportError.Failed,
            AccountExportResult.NotConfigured to DataExportError.NotConfigured,
            AccountExportResult.NotSignedIn to DataExportError.NotSignedIn,
        )
        for ((result, error) in cases) {
            val export = FakeExportRepository(result = result)
            val vm = viewModel(FakeAccountDeletionRepository(), export)
            vm.events.test {
                vm.onExport()
                expectNoEvents()
            }
            assertEquals(DataExportState.Error, vm.state.value.export)
            assertEquals(error, vm.state.value.exportError)
            assertTrue("retry stays one tap away", vm.state.value.canExport)

            export.result = AccountExportResult.Ready(DOCUMENT)
            vm.events.test {
                vm.onExport()
                assertTrue(awaitItem() is HapusAkunEvent.ChooseExportFile)
            }
            assertNull(vm.state.value.exportError)
            assertEquals(2, export.exportCalls)
        }
    }

    @Test
    fun export_saveFails_showsSaveError() = runTest {
        val export = FakeExportRepository(saveOk = false)
        val vm = viewModel(FakeAccountDeletionRepository(), export)
        vm.onExport()
        vm.onExportDestination("content://docs/x.json")
        assertEquals(DataExportState.Error, vm.state.value.export)
        assertEquals(DataExportError.SaveFailed, vm.state.value.exportError)
    }

    @Test
    fun export_pickerClosed_backToIdle_andNothingWritten() = runTest {
        val export = FakeExportRepository()
        val vm = viewModel(FakeAccountDeletionRepository(), export)
        vm.onExport()
        vm.onExportDestination(null)
        assertEquals(DataExportState.Idle, vm.state.value.export)
        assertTrue(export.saved.isEmpty())
        // A late destination without a document (for example after a restart) asks for a retry.
        vm.onExportDestination("content://docs/x.json")
        assertEquals(DataExportError.Failed, vm.state.value.exportError)
        assertTrue(export.saved.isEmpty())
    }

    @Test
    fun export_pickerUnavailable_showsSaveError() = runTest {
        val vm = viewModel(FakeAccountDeletionRepository())
        vm.onExport()
        vm.onExportPickerUnavailable()
        assertEquals(DataExportError.SaveFailed, vm.state.value.exportError)
    }

    @Test
    fun export_notPossibleWhileDeleting() = runTest {
        val repo = FakeAccountDeletionRepository().apply { gate = CompletableDeferred() }
        val export = FakeExportRepository()
        val vm = viewModel(repo, export)
        vm.onConfirmationChange("HAPUS")
        vm.onDelete()
        vm.onExport()
        assertEquals(0, export.exportCalls)
        repo.gate!!.complete(Unit)
    }

    private companion object {
        const val DOCUMENT = """{"format":"kelana-account-export","version":1,"account":{"id":"u1"}}"""
    }
}

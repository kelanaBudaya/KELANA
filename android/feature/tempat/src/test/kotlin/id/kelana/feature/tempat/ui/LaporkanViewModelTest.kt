package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.model.ReportDraft
import id.kelana.core.model.ReportReason
import id.kelana.core.model.ReportRepository
import id.kelana.core.model.ReportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LaporkanViewModelTest {
    private class FakeReports(var result: () -> ReportResult = { ReportResult.Success }) : ReportRepository {
        val submitted = mutableListOf<ReportDraft>()
        override suspend fun submit(report: ReportDraft): ReportResult {
            submitted += report
            return result()
        }
    }

    private val repo = FakeReports()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = LaporkanViewModel(SavedStateHandle(mapOf("placeId" to "p1")), repo)

    @Test
    fun reason_is_required() = runTest {
        val vm = vm()
        assertFalse(vm.uiState.value.canSubmit)
        vm.submit()
        assertTrue(repo.submitted.isEmpty())
        vm.selectReason(ReportReason.PlaceClosed)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun guest_needs_login() = runTest {
        repo.result = { ReportResult.NeedsLogin }
        val vm = vm()
        vm.events.test {
            vm.selectReason(ReportReason.PlaceClosed)
            vm.submit()
            assertEquals(LaporkanEvent.NeedsLogin, awaitItem())
        }
        assertFalse(vm.uiState.value.failed)
    }

    @Test
    fun success_sends_draft_and_emits_sent() = runTest {
        val vm = vm()
        vm.events.test {
            vm.selectReason(ReportReason.HoursOrPriceChanged)
            vm.setDetails("  Jam berubah  ")
            vm.submit()
            assertEquals(LaporkanEvent.Sent, awaitItem())
        }
        assertEquals(ReportDraft("p1", ReportReason.HoursOrPriceChanged, "Jam berubah"), repo.submitted.single())
    }

    @Test
    fun blank_details_are_sent_as_null() = runTest {
        val vm = vm()
        vm.selectReason(ReportReason.InappropriateMedia)
        vm.setDetails("   ")
        vm.submit()
        assertNull(repo.submitted.single().details)
    }

    @Test
    fun failure_shows_error_and_allows_retry() = runTest {
        repo.result = { ReportResult.Failed }
        val vm = vm()
        vm.selectReason(ReportReason.PlaceClosed)
        vm.submit()
        assertTrue(vm.uiState.value.failed)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun exception_is_a_failure() = runTest {
        repo.result = { error("network") }
        val vm = vm()
        vm.selectReason(ReportReason.PlaceClosed)
        vm.submit()
        assertTrue(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.submitting)
    }

    @Test
    fun details_are_capped() = runTest {
        val vm = vm()
        vm.setDetails("a".repeat(MAX_DETAILS_LENGTH + 50))
        assertEquals(MAX_DETAILS_LENGTH, vm.uiState.value.details.length)
    }
}

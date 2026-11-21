package id.kelana.feature.paspor.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampResult
import id.kelana.feature.paspor.ScriptedLocation
import id.kelana.feature.paspor.ScriptedStampRepository
import id.kelana.feature.paspor.data.LocationFix
import id.kelana.feature.paspor.domain.StampFailure
import id.kelana.feature.paspor.fix
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AmbilCapViewModelTest {
    private val now = Instant.parse("2026-11-21T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val placeId = "museum-sonobudoyo"

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(stamps: ScriptedStampRepository, location: ScriptedLocation = ScriptedLocation(fix())) =
        AmbilCapViewModel(SavedStateHandle(mapOf("placeId" to placeId)), FakePlaceRepository(), stamps, location, clock)

    private fun kotlinx.coroutines.test.TestScope.watch(vm: AmbilCapViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
    }

    private fun AmbilCapViewModel.step(): CapStep = uiState.value.step

    private fun AmbilCapViewModel.ready() {
        onPermissionChecked(granted = true)
    }

    // ----- permission (6.1, 6.5)

    @Test fun without_permission_the_explain_screen_comes_first() = runTest {
        val vm = vm(ScriptedStampRepository())
        watch(vm)
        assertEquals(CapStep.Checking, vm.step())
        vm.onPermissionChecked(granted = false)
        assertEquals(CapStep.Explain, vm.step())
    }

    @Test fun granted_permission_goes_straight_to_ready() = runTest {
        val vm = vm(ScriptedStampRepository())
        watch(vm)
        vm.onPermissionChecked(granted = true)
        assertEquals(CapStep.Ready, vm.step())
    }

    @Test fun refused_prompt_opens_denied_and_remembers_dont_ask_again() = runTest {
        val vm = vm(ScriptedStampRepository())
        watch(vm)
        vm.onPermissionChecked(granted = false)
        vm.onPermissionResult(granted = false, canAskAgain = true)
        assertEquals(CapStep.Denied(canAskAgain = true), vm.step())
        vm.onPermissionResult(granted = false, canAskAgain = false)
        assertEquals(CapStep.Denied(canAskAgain = false), vm.step())
        // Re-checking while still not granted keeps the denied screen.
        vm.onPermissionChecked(granted = false)
        assertEquals(CapStep.Denied(canAskAgain = false), vm.step())
    }

    @Test fun granting_in_settings_returns_to_ready() = runTest {
        val vm = vm(ScriptedStampRepository())
        watch(vm)
        vm.onPermissionResult(granted = false, canAskAgain = false)
        vm.onPermissionChecked(granted = true)
        assertEquals(CapStep.Ready, vm.step())
    }

    @Test fun revoked_permission_goes_back_to_explain() = runTest {
        val vm = vm(ScriptedStampRepository())
        watch(vm)
        vm.ready()
        vm.onPermissionChecked(granted = false)
        assertEquals(CapStep.Explain, vm.step())
    }

    @Test fun the_location_is_not_read_before_the_tap() = runTest {
        val location = ScriptedLocation(fix())
        val vm = vm(ScriptedStampRepository(), location)
        watch(vm)
        vm.onPermissionChecked(granted = true)
        vm.onPermissionChecked(granted = true)
        assertEquals(0, location.reads)
    }

    @Test fun tapping_without_permission_does_nothing() = runTest {
        val location = ScriptedLocation(fix())
        val vm = vm(ScriptedStampRepository(), location)
        watch(vm)
        vm.onPermissionChecked(granted = false)
        vm.onCapNow()
        assertEquals(0, location.reads)
    }

    // ----- success

    @Test fun confirmed_stamp_opens_cap_berhasil_and_sends_the_single_fix() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Stamped)
        val location = ScriptedLocation(fix(accuracyM = 12.3))
        val vm = vm(stamps, location)
        watch(vm)
        vm.ready()
        vm.events.test {
            vm.onCapNow()
            assertEquals(AmbilCapEvent.Stamped(placeId), awaitItem())
        }
        assertEquals(1, location.reads)
        val request = stamps.requests.single()
        assertEquals(placeId, request.placeId)
        assertEquals(12.3, request.accuracyM, 0.0)
        assertEquals(false, request.isMock)
        assertEquals(now, request.capturedAt)
    }

    @Test fun already_stamped_also_opens_cap_berhasil() = runTest {
        val vm = vm(ScriptedStampRepository(nextResult = StampResult.AlreadyStamped))
        watch(vm)
        vm.ready()
        vm.events.test {
            vm.onCapNow()
            assertEquals(AmbilCapEvent.Stamped(placeId), awaitItem())
        }
    }

    @Test fun each_tap_reads_the_location_once_and_a_second_tap_while_sending_is_ignored() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Stamped)
        val gate = CompletableDeferred<Unit>()
        stamps.gate = gate
        val location = ScriptedLocation(fix())
        val vm = vm(stamps, location)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertTrue(vm.step() is CapStep.Working)
        vm.onCapNow()
        vm.onCapNow()
        assertEquals(1, location.reads)
        assertEquals(1, stamps.requests.size)
        gate.complete(Unit)
    }

    @Test fun live_accuracy_comes_only_from_the_single_fix() = runTest {
        val stamps = ScriptedStampRepository()
        val gate = CompletableDeferred<Unit>()
        stamps.gate = gate
        val vm = vm(stamps, ScriptedLocation(fix(accuracyM = 17.2)))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.Working(accuracyM = 18), vm.step())
        gate.complete(Unit)
    }

    // ----- no stamp before the server confirms

    @Test fun nothing_is_claimed_while_the_server_has_not_confirmed() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.events.test {
            vm.onCapNow()
            expectNoEvents()
        }
        assertTrue(stamps.stampsState.value.isEmpty())
        assertTrue(vm.step() is CapStep.WaitingSignal)
    }

    @Test fun a_rejection_never_opens_cap_berhasil_and_adds_no_stamp() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Rejected(StampRejectReason.TooFar, 420))
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.events.test {
            vm.onCapNow()
            expectNoEvents()
        }
        assertTrue(stamps.stampsState.value.isEmpty())
    }

    // ----- rejections

    @Test fun too_far_opens_di_luar_area_with_the_server_distance() = runTest {
        val vm = vm(ScriptedStampRepository(nextResult = StampResult.Rejected(StampRejectReason.TooFar, 420)))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.OutOfArea(distanceM = 420), vm.step())
    }

    @Test fun too_far_without_a_distance_shows_no_number() = runTest {
        val vm = vm(ScriptedStampRepository(nextResult = StampResult.Rejected(StampRejectReason.TooFar, null)))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.OutOfArea(distanceM = null), vm.step())
    }

    @Test fun every_other_server_reason_has_its_own_failure() = runTest {
        val expected = mapOf(
            StampRejectReason.Expired to StampFailure.Expired,
            StampRejectReason.FutureTimestamp to StampFailure.ClockOff,
            StampRejectReason.LowAccuracy to StampFailure.LowAccuracy,
            StampRejectReason.MockLocation to StampFailure.MockLocation,
            StampRejectReason.IntegrityFailed to StampFailure.IntegrityFailed,
            StampRejectReason.PlaceUnknown to StampFailure.PlaceUnknown,
            StampRejectReason.RadiusNotSet to StampFailure.RadiusNotSet,
            StampRejectReason.BadRequest to StampFailure.BadRequest,
        )
        // Every reason except too_far is covered, and no two reasons share a screen.
        assertEquals(StampRejectReason.entries.toSet() - StampRejectReason.TooFar, expected.keys)
        assertEquals(expected.size, expected.values.toSet().size)
        expected.forEach { (reason, failure) ->
            val vm = vm(ScriptedStampRepository(nextResult = StampResult.Rejected(reason)))
            watch(vm)
            vm.ready()
            vm.onCapNow()
            assertEquals("reason $reason", CapStep.Failed(failure), vm.step())
        }
    }

    @Test fun client_side_results_map_to_their_own_states() = runTest {
        mapOf(
            StampResult.NeedsLogin to CapStep.Failed(StampFailure.NeedsLogin),
            StampResult.NotConfigured to CapStep.Failed(StampFailure.NotConfigured),
            StampResult.Expired to CapStep.Failed(StampFailure.Expired),
        ).forEach { (result, step) ->
            val vm = vm(ScriptedStampRepository(nextResult = result))
            watch(vm)
            vm.ready()
            vm.onCapNow()
            assertEquals("result $result", step, vm.step())
        }
    }

    @Test fun a_guest_gets_needs_login() = runTest {
        val vm = vm(ScriptedStampRepository(nextResult = StampResult.NeedsLogin))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.Failed(StampFailure.NeedsLogin), vm.step())
    }

    @Test fun network_error_means_the_request_is_queued_and_waiting() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.NetworkError)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.WaitingSignal(capturedAt = now), vm.step())
    }

    @Test fun a_mock_location_is_rejected_on_the_client_without_a_request() = runTest {
        val stamps = ScriptedStampRepository()
        val vm = vm(stamps, ScriptedLocation(fix(isMock = true)))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.Failed(StampFailure.MockLocation), vm.step())
        assertTrue(stamps.requests.isEmpty())
    }

    @Test fun no_fix_shows_its_own_failure() = runTest {
        val stamps = ScriptedStampRepository()
        val vm = vm(stamps, ScriptedLocation(LocationFix.NoFix))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.Failed(StampFailure.NoFix), vm.step())
        assertTrue(stamps.requests.isEmpty())
    }

    @Test fun losing_the_permission_during_the_read_goes_back_to_explain() = runTest {
        val vm = vm(ScriptedStampRepository(), ScriptedLocation(LocationFix.NoPermission))
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertEquals(CapStep.Explain, vm.step())
    }

    @Test fun retry_returns_to_ready_and_allows_another_tap() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Rejected(StampRejectReason.LowAccuracy))
        val location = ScriptedLocation(fix())
        val vm = vm(stamps, location)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        vm.onRetry()
        assertEquals(CapStep.Ready, vm.step())
        vm.onCapNow()
        assertEquals(2, location.reads) // once per tap
    }

    // ----- pending ("menunggu sinyal", PROVISIONAL)

    @Test fun reopening_with_a_queued_request_for_this_place_shows_waiting() = runTest {
        val stamps = ScriptedStampRepository()
        val capturedAt = now.minusSeconds(300)
        stamps.pendingState.value = id.kelana.core.model.PendingStamp(placeId.uppercase(), capturedAt)
        val vm = vm(stamps)
        watch(vm)
        assertEquals(CapStep.WaitingSignal(capturedAt), vm.step())
    }

    @Test fun a_queued_request_for_another_place_is_ignored() = runTest {
        val stamps = ScriptedStampRepository()
        stamps.pendingState.value = id.kelana.core.model.PendingStamp("tamansari", now)
        val vm = vm(stamps)
        watch(vm)
        assertEquals(CapStep.Checking, vm.step())
    }

    @Test fun waiting_state_knows_when_the_30_minute_limit_ends() {
        val step = CapStep.WaitingSignal(capturedAt = now)
        assertEquals(now.plusSeconds(30 * 60), step.expiresAt)
    }

    @Test fun cancel_deletes_the_queued_request_and_goes_back_to_ready() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        assertTrue(vm.step() is CapStep.WaitingSignal)
        vm.events.test {
            vm.onCancelPending()
            expectNoEvents()
        }
        assertEquals(1, stamps.cancelCalls)
        assertEquals(null, stamps.pendingState.value)
        assertEquals(CapStep.Ready, vm.step())
        assertTrue(stamps.stampsState.value.isEmpty())
    }

    @Test fun when_the_server_later_confirms_the_open_screen_shows_cap_berhasil() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        vm.events.test {
            expectNoEvents()
            stamps.confirmLater(placeId, now)
            assertEquals(AmbilCapEvent.Stamped(placeId), awaitItem())
        }
    }

    @Test fun a_queued_request_answered_without_a_stamp_is_not_shown_as_success() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        vm.events.test {
            stamps.pendingState.value = null // the worker got a rejection and deleted the row
            advanceTimeBy(6_000)
            expectNoEvents()
        }
        assertEquals(CapStep.Failed(StampFailure.Unconfirmed), vm.step())
    }

    @Test fun an_expired_queued_request_is_dropped_and_the_user_is_asked_to_try_again() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        val vm = vm(stamps)
        watch(vm)
        vm.ready()
        vm.onCapNow()
        vm.onPendingExpired()
        assertEquals(CapStep.Failed(StampFailure.Expired), vm.step())
        assertEquals(1, stamps.cancelCalls)
        assertEquals(null, stamps.pendingState.value)
    }

    @Test fun existing_stamps_do_not_trigger_cap_berhasil_by_themselves() = runTest {
        val stamps = ScriptedStampRepository(initialStamps = listOf(Stamp(placeId, now)))
        val vm = vm(stamps)
        watch(vm)
        vm.events.test {
            vm.ready()
            expectNoEvents()
        }
    }

    @Test fun unknown_place_is_reported() = runTest {
        val vm = AmbilCapViewModel(
            SavedStateHandle(mapOf("placeId" to "nowhere")), FakePlaceRepository(), ScriptedStampRepository(),
            ScriptedLocation(fix()), clock,
        )
        watch(vm)
        assertEquals(PlaceStatus.NotFound, vm.uiState.value.placeStatus)
    }
}

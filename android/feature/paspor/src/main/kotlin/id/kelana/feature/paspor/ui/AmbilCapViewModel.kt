package id.kelana.feature.paspor.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PendingStamp
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.STAMP_QUEUE_EXPIRY_MINUTES
import id.kelana.core.model.StampRepository
import id.kelana.core.model.StampRequest
import id.kelana.feature.paspor.data.LocationFix
import id.kelana.feature.paspor.data.LocationFixSource
import id.kelana.feature.paspor.domain.StampFailure
import id.kelana.feature.paspor.domain.StampOutcome
import id.kelana.feature.paspor.domain.toOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.ceil

/** Where the Ambil cap flow is. Screens 6.1 to 6.5 plus the PROVISIONAL "menunggu sinyal" state. */
sealed interface CapStep {
    /** Permission not looked at yet. */
    data object Checking : CapStep

    /** 6.1: explain first, then the system prompt. */
    data object Explain : CapStep

    /** 6.5: permission refused. [canAskAgain] false means "don't ask again": only the app settings can help. */
    data class Denied(val canAskAgain: Boolean) : CapStep

    /** 6.2 */
    data object Ready : CapStep

    /** Reading the single fix, then waiting for the server. [accuracyM] comes only from that fix. */
    data class Working(val accuracyM: Int? = null) : CapStep

    /** PROVISIONAL: the request is queued and sent when there is signal. */
    data class WaitingSignal(val capturedAt: Instant) : CapStep {
        val expiresAt: Instant get() = capturedAt.plus(Duration.ofMinutes(STAMP_QUEUE_EXPIRY_MINUTES))
    }

    /** 6.4. [distanceM] is the server's number, or null when the server gave none. */
    data class OutOfArea(val distanceM: Int?) : CapStep

    data class Failed(val failure: StampFailure) : CapStep
}

enum class PlaceStatus { Loading, Loaded, NotFound }

data class AmbilCapUiState(
    val placeStatus: PlaceStatus = PlaceStatus.Loading,
    val place: Place? = null,
    val step: CapStep = CapStep.Checking,
)

sealed interface AmbilCapEvent {
    /** The server confirmed the stamp: open "Cap berhasil". */
    data class Stamped(val placeId: String) : AmbilCapEvent
}

/**
 * Ambil cap for one place. The location is read once per tap of "Cap sekarang" ([onCapNow]); the fix is passed
 * straight to [StampRepository.requestStamp] and is not kept here. The server decides: this class only shows
 * what the repository answered. A stamp is never claimed before [StampOutcome.Confirmed].
 */
@HiltViewModel
class AmbilCapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    places: PlaceRepository,
    private val stamps: StampRepository,
    private val location: LocationFixSource,
    private val clock: Clock,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()
    private val step = MutableStateFlow<CapStep>(CapStep.Checking)
    private var permissionGranted = false

    /** True from the tap until the repository answered; the pending observer stays out of the way meanwhile. */
    private var sending = false

    private val _events = Channel<AmbilCapEvent>(Channel.BUFFERED)
    val events: Flow<AmbilCapEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<AmbilCapUiState> = combine(
        places.place(placeId).catch { emit(null) },
        step,
    ) { place, current ->
        AmbilCapUiState(
            placeStatus = if (place == null) PlaceStatus.NotFound else PlaceStatus.Loaded,
            place = place,
            step = current,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AmbilCapUiState())

    init {
        viewModelScope.launch { stamps.pending().collect { onPending(it) } }
    }

    /** The screen checked the system permission (on enter and on every resume). */
    fun onPermissionChecked(granted: Boolean) {
        permissionGranted = granted
        when (val current = step.value) {
            CapStep.Checking, CapStep.Explain, is CapStep.Denied ->
                step.value = when {
                    granted -> CapStep.Ready
                    current is CapStep.Denied -> current
                    else -> CapStep.Explain
                }
            CapStep.Ready -> if (!granted) step.value = CapStep.Explain
            else -> Unit
        }
    }

    /** Result of the system prompt. [canAskAgain] is `shouldShowRequestPermissionRationale` after the answer. */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        permissionGranted = granted
        if (step.value is CapStep.Working || step.value is CapStep.WaitingSignal) return
        step.value = if (granted) CapStep.Ready else CapStep.Denied(canAskAgain)
    }

    /** "Cap sekarang": the only place that reads the location, once per call. */
    fun onCapNow() {
        if (sending || step.value !is CapStep.Ready) return
        sending = true
        step.value = CapStep.Working()
        viewModelScope.launch {
            try {
                when (val fix = location.currentFix()) {
                    LocationFix.NoPermission -> {
                        permissionGranted = false
                        step.value = CapStep.Explain
                    }
                    LocationFix.NoFix -> step.value = CapStep.Failed(StampFailure.NoFix)
                    is LocationFix.Fix -> submit(fix)
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                sending = false
            }
        }
    }

    private suspend fun submit(fix: LocationFix.Fix) {
        // Defense in depth: the server rejects mock locations too.
        if (fix.isMock) {
            step.value = CapStep.Failed(StampFailure.MockLocation)
            return
        }
        step.value = CapStep.Working(accuracyM = ceil(fix.accuracyM).toInt())
        val capturedAt = clock.instant()
        val request = StampRequest(placeId, fix.lat, fix.lng, fix.accuracyM, fix.isMock, capturedAt)
        when (val outcome = stamps.requestStamp(request).toOutcome()) {
            StampOutcome.Confirmed -> _events.send(AmbilCapEvent.Stamped(placeId))
            is StampOutcome.TooFar -> step.value = CapStep.OutOfArea(outcome.distanceM)
            is StampOutcome.Failed -> step.value = CapStep.Failed(outcome.failure)
            StampOutcome.Waiting -> step.value = CapStep.WaitingSignal(capturedAt)
        }
    }

    /** "Coba lagi" from an error or the out-of-area screen. */
    fun onRetry() {
        val current = step.value
        if (current is CapStep.Failed || current is CapStep.OutOfArea) {
            step.value = if (permissionGranted) CapStep.Ready else CapStep.Explain
        }
    }

    /** "Batalkan" on the waiting screen: the queued request and its fix are deleted. */
    fun onCancelPending() {
        step.value = if (permissionGranted) CapStep.Ready else CapStep.Explain
        viewModelScope.launch { stamps.cancelPending() }
    }

    /** The queue limit passed while the screen was open: drop the request and ask the user to try again. */
    fun onPendingExpired() {
        if (step.value !is CapStep.WaitingSignal) return
        step.value = CapStep.Failed(StampFailure.Expired)
        viewModelScope.launch { stamps.cancelPending() }
    }

    private suspend fun onPending(pending: PendingStamp?) {
        if (sending) return
        val mine = pending?.takeIf { it.placeId.equals(placeId, ignoreCase = true) }
        val current = step.value
        if (mine != null) {
            if (current !is CapStep.WaitingSignal && current !is CapStep.Working) {
                step.value = CapStep.WaitingSignal(mine.capturedAt)
            }
            return
        }
        if (current is CapStep.WaitingSignal) resolveAnsweredRequest()
    }

    /** The queued request is gone and the user did not cancel it: the server answered. Show a stamp only if it is there. */
    private suspend fun resolveAnsweredRequest() {
        val confirmed = withTimeoutOrNull(CONFIRM_WAIT_MS) {
            stamps.stamps().first { list -> list.any { it.placeId.equals(placeId, ignoreCase = true) } }
        } != null
        if (confirmed) {
            _events.send(AmbilCapEvent.Stamped(placeId))
        } else {
            step.value = CapStep.Failed(StampFailure.Unconfirmed)
        }
    }

    private companion object {
        /** The queue row is deleted just before the stamp cache is refreshed; give the cache a moment. */
        const val CONFIRM_WAIT_MS = 5_000L
    }
}

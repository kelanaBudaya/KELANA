package id.kelana.feature.paspor.domain

import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampResult

/** Why Ambil cap did not give a stamp (other than "too far", which has its own screen). One copy per entry. */
enum class StampFailure {
    LowAccuracy,
    Expired,
    ClockOff,
    IntegrityFailed,
    RadiusNotSet,
    MockLocation,
    PlaceUnknown,
    BadRequest,
    NotConfigured,
    NoFix,
    NeedsLogin,

    /** A queued request was answered but no stamp showed up. */
    Unconfirmed,
}

/** What the screen does after the repository answered. The client never decides that a stamp was earned. */
sealed interface StampOutcome {
    /** The server confirmed the stamp (new or already held). Only this opens "Cap berhasil". */
    data object Confirmed : StampOutcome

    data class TooFar(val distanceM: Int?) : StampOutcome

    data class Failed(val failure: StampFailure) : StampOutcome

    /** Queued: "menunggu sinyal" (PROVISIONAL screen). */
    data object Waiting : StampOutcome
}

fun StampResult.toOutcome(): StampOutcome = when (this) {
    StampResult.Stamped, StampResult.AlreadyStamped -> StampOutcome.Confirmed
    is StampResult.Rejected -> when (reason) {
        StampRejectReason.TooFar -> StampOutcome.TooFar(distanceM)
        StampRejectReason.Expired -> StampOutcome.Failed(StampFailure.Expired)
        StampRejectReason.FutureTimestamp -> StampOutcome.Failed(StampFailure.ClockOff)
        StampRejectReason.LowAccuracy -> StampOutcome.Failed(StampFailure.LowAccuracy)
        StampRejectReason.MockLocation -> StampOutcome.Failed(StampFailure.MockLocation)
        StampRejectReason.IntegrityFailed -> StampOutcome.Failed(StampFailure.IntegrityFailed)
        StampRejectReason.PlaceUnknown -> StampOutcome.Failed(StampFailure.PlaceUnknown)
        StampRejectReason.RadiusNotSet -> StampOutcome.Failed(StampFailure.RadiusNotSet)
        StampRejectReason.BadRequest -> StampOutcome.Failed(StampFailure.BadRequest)
    }
    // Both mean the request is still in the local queue and will be sent when there is signal.
    StampResult.Queued, StampResult.NetworkError -> StampOutcome.Waiting
    StampResult.NeedsLogin -> StampOutcome.Failed(StampFailure.NeedsLogin)
    StampResult.NotConfigured -> StampOutcome.Failed(StampFailure.NotConfigured)
    StampResult.Expired -> StampOutcome.Failed(StampFailure.Expired)
}

package id.kelana.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single queued stamp request. This row is the ONLY place a location fix is ever persisted. It is
 * deleted on any server answer and when older than 30 minutes. The Play Integrity token is never stored here.
 * [id] is always [SINGLE_ID]: there is at most one pending request.
 */
@Entity(tableName = "pending_stamp")
data class PendingStampEntity(
    @PrimaryKey val id: Int = SINGLE_ID,
    /** Account that took the fix; a different account never sends it. */
    val userId: String,
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
    val isMock: Boolean,
    /** Epoch milliseconds (UTC) of the fix. */
    val capturedAtMillis: Long,
) {
    // Keep coordinates out of logs and crash reports.
    override fun toString(): String = "PendingStampEntity(placeId=$placeId, capturedAtMillis=$capturedAtMillis)"

    companion object {
        const val SINGLE_ID = 1
    }
}

/** Offline copy of the user's own stamps (server rows, own rows only through RLS). Holds no location. */
@Entity(tableName = "own_stamp", primaryKeys = ["userId", "placeId"])
data class OwnStampEntity(
    val userId: String,
    val placeId: String,
    val stampedAtMillis: Long,
)

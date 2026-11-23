package id.kelana.core.model

/**
 * What "Hapus akun" will delete, counted from real data of the signed-in user. A count is null when it could not be
 * read (offline, not configured, guest): the screen then drops the number instead of inventing one.
 */
data class AccountDeletionSummary(
    val stampCount: Int? = null,
    val postcardCount: Int? = null,
    val videoCount: Int? = null,
)

enum class AccountDeletionResult {
    /** The server deleted the account; the local session and per-user caches are already cleared. */
    Deleted,

    /** No signed-in user. Nothing was sent. */
    NotSignedIn,

    /** Supabase is not configured in this build. Nothing was sent. */
    NotConfigured,

    /** No answer from the server (offline, timeout). Nothing local was cleared. */
    NetworkError,

    /** The server refused or failed (401, 5xx, unreadable answer). Nothing local was cleared. */
    Failed,
}

/** The word the user types to confirm, and that the server checks again (`delete-account`). Case-sensitive. */
const val ACCOUNT_DELETION_CONFIRM_WORD: String = "HAPUS"

/**
 * Account deletion. The server (Edge Function `delete-account`) does the deleting, for the JWT's user only;
 * the client never deletes rows itself.
 */
interface AccountDeletionRepository {
    suspend fun summary(): AccountDeletionSummary

    /**
     * Asks the server to delete the signed-in account. Only on [AccountDeletionResult.Deleted] does it then clear
     * the local session and the per-user local data (pending stamp, passport cache, plan); on any other result
     * nothing local is touched.
     */
    suspend fun deleteAccount(): AccountDeletionResult
}

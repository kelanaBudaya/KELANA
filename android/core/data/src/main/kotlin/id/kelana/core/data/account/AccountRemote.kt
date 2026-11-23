package id.kelana.core.data.account

import kotlinx.serialization.Serializable

/** Server side of "Hapus akun". Real implementation: [SupabaseAccountRemote]. Tests use a fake. */
interface AccountRemote {
    /** False when the Supabase url or anon key is missing in this build. */
    val isConfigured: Boolean

    /** Number of own rows in [table] (filtered on `user_id`, RLS applies), or null when it could not be read. */
    suspend fun countOwn(table: OwnTable, userId: String): Int?

    /** Calls the `delete-account` Edge Function with the signed-in user's JWT. Never throws (except cancellation). */
    suspend fun deleteAccount(): DeleteAccountOutcome

    /** Drops the local Supabase session without calling the server (the user no longer exists there). */
    suspend fun clearLocalSession()
}

enum class OwnTable(val tableName: String) {
    Stamps("stamps"),
    Postcards("postcards"),
    Videos("videos"),
}

enum class DeleteAccountOutcome {
    /** HTTP 200 `{ "status": "deleted" }`. */
    Deleted,

    /** No answer (offline, timeout). */
    Unreachable,

    /** An answer that is not a deletion (400, 401, 5xx, unexpected body). */
    Refused,
}

/** Body of `delete-account`: the word the user typed. The server checks it again. */
@Serializable
data class DeleteAccountBody(val confirm: String)

@Serializable
data class DeleteAccountResponseDto(val status: String? = null)

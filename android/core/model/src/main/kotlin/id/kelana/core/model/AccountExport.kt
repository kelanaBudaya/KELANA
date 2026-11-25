package id.kelana.core.model

/** Outcome of asking the server for the signed-in user's own data ("Unduh dataku dulu"). */
sealed interface AccountExportResult {
    /** The JSON document of the user's data, checked to be an export of the signed-in user. */
    data class Ready(val document: String) : AccountExportResult

    /** No signed-in user. Nothing was sent. */
    data object NotSignedIn : AccountExportResult

    /** Supabase is not configured in this build. Nothing was sent. */
    data object NotConfigured : AccountExportResult

    /** No answer from the server (offline, timeout). */
    data object NetworkError : AccountExportResult

    /** The server refused or failed, or the answer was not an export of this user. */
    data object Failed : AccountExportResult
}

/**
 * Data export. The server (Edge Function `export-account`) builds the document for the JWT's user only; the device's
 * location is never part of it (none is stored).
 */
interface AccountExportRepository {
    suspend fun export(): AccountExportResult

    /**
     * Writes [document] to [destination], a document URI the user picked through the Storage Access Framework
     * (no storage permission). True when written.
     */
    suspend fun save(destination: String, document: String): Boolean
}

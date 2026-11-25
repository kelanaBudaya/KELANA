package id.kelana.core.data.account

/** Server side of "Unduh dataku dulu". Real implementation: [SupabaseAccountExportRemote]. Tests use a fake. */
interface AccountExportRemote {
    /** False when the Supabase url or anon key is missing in this build. */
    val isConfigured: Boolean

    /** Calls the `export-account` Edge Function with the signed-in user's JWT. Never throws (except cancellation). */
    suspend fun exportAccount(): ExportAccountOutcome
}

sealed interface ExportAccountOutcome {
    /** HTTP 200 with the document as text. */
    data class Exported(val body: String) : ExportAccountOutcome

    /** No answer (offline, timeout). */
    data object Unreachable : ExportAccountOutcome

    /** An answer that is not an export (401, 5xx). */
    data object Refused : ExportAccountOutcome
}

/** Writes a document the user picked through the Storage Access Framework. */
interface ExportDocumentWriter {
    /** Writes [content] as UTF-8 to [destination] (a content URI string). Throws when it cannot. */
    suspend fun write(destination: String, content: String)
}

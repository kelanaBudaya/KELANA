package id.kelana.core.data.auth

/**
 * The Supabase client wants the project root (https://xxx.supabase.co). The dashboard also shows the REST
 * endpoint (https://xxx.supabase.co/rest/v1/), so accept that form too and strip the suffix.
 */
internal fun normalizeSupabaseUrl(raw: String): String =
    raw.trim().trimEnd('/').removeSuffix("/rest/v1").trimEnd('/')

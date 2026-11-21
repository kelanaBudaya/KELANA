package id.kelana.core.model

/**
 * Build-time auth configuration visible to features. Secrets (the Supabase anon key) are deliberately not
 * part of this class so they cannot leak through logging. Empty or false means "not configured yet".
 */
data class AuthConfig(
    val isSupabaseConfigured: Boolean,
    val googleWebClientId: String,
) {
    val isGoogleConfigured: Boolean
        get() = googleWebClientId.isNotBlank()
}

package id.kelana.core.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kelana.core.data.BuildConfig
import id.kelana.core.model.AuthConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the Supabase client once, and only when url and anon key are present in the build config. */
@Singleton
class SupabaseClientProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val authConfig: AuthConfig = AuthConfig(
        isSupabaseConfigured = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank(),
        googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    )

    val client: SupabaseClient? by lazy {
        if (!authConfig.isSupabaseConfigured) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = normalizeSupabaseUrl(BuildConfig.SUPABASE_URL),
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    sessionManager = PrefsSessionManager(context)
                }
                install(Postgrest)
                install(Functions)
            }
        }
    }
}

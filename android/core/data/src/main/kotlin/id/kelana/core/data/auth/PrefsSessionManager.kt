package id.kelana.core.data.auth

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/** Persists the Supabase session in private SharedPreferences (allowBackup is off, so it is not backed up). */
internal class PrefsSessionManager(context: Context) : SessionManager {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(UserSession.serializer(), session)).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private companion object {
        const val PREFS_NAME = "kelana_session"
        const val KEY_SESSION = "session"
    }
}

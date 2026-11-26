package id.kelana.core.data.plan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import id.kelana.core.model.PlanCache
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.TripPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the last [TripPlan] and the reminder choices across process restarts: one versioned JSON document each, in
 * the shared preferences DataStore (writes are atomic). A document that is unreadable, has another version or holds
 * an unknown value reads as "no plan" (reminders: the bad entry is dropped); it never throws.
 * [clear] wipes the plan and the reminder choices (used by account deletion).
 */
@Singleton
class DataStorePlanCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PlanCache {

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val trip: Flow<TripPlan?> = prefs.map { decodeTrip(it[TRIP_KEY]) }.distinctUntilChanged()

    override val reminders: Flow<Map<String, ReminderChoice>> =
        prefs.map { decodeReminders(it[REMINDERS_KEY]) }.distinctUntilChanged()

    override suspend fun save(trip: TripPlan) {
        val raw = json.encodeToString(PlanDocumentDto(PLAN_CACHE_VERSION, trip.toDto()))
        dataStore.edit { it[TRIP_KEY] = raw }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(TRIP_KEY)
            it.remove(REMINDERS_KEY)
        }
    }

    override suspend fun saveReminder(eventId: String, choice: ReminderChoice) {
        dataStore.edit { prefs ->
            val updated = decodeReminders(prefs[REMINDERS_KEY]) + (eventId to choice)
            prefs[REMINDERS_KEY] = json.encodeToString(updated.toDto())
        }
    }

    private fun decodeTrip(raw: String?): TripPlan? {
        if (raw == null) return null
        return runCatching {
            val doc = json.decodeFromString<PlanDocumentDto>(raw)
            if (doc.version != PLAN_CACHE_VERSION) null else doc.trip.toModel()
        }.getOrNull()
    }

    private fun decodeReminders(raw: String?): Map<String, ReminderChoice> {
        if (raw == null) return emptyMap()
        return runCatching {
            val doc = json.decodeFromString<RemindersDocumentDto>(raw)
            if (doc.version != PLAN_CACHE_VERSION) emptyMap() else doc.toModel()
        }.getOrDefault(emptyMap())
    }

    internal companion object {
        val TRIP_KEY = stringPreferencesKey("plan_cache_trip_json")
        val REMINDERS_KEY = stringPreferencesKey("plan_cache_reminders_json")
        private val json = Json { ignoreUnknownKeys = true }
    }
}

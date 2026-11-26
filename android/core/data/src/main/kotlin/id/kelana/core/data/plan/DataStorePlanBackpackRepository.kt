package id.kelana.core.data.plan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.kelana.core.model.PlanBackpackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Stores the ids as one newline-joined string, which keeps the order a string set would lose. */
@Singleton
class DataStorePlanBackpackRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PlanBackpackRepository {

    override fun placeIds(): Flow<List<String>> = dataStore.data.map { decode(it[KEY]) }.distinctUntilChanged()

    override suspend fun addPlace(placeId: String) {
        if (placeId.isBlank() || '\n' in placeId) return
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            if (placeId !in current) prefs[KEY] = encode(current + placeId)
        }
    }

    override suspend fun removePlace(placeId: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            if (placeId in current) prefs[KEY] = encode(current - placeId)
        }
    }

    private fun decode(raw: String?): List<String> = raw.orEmpty().split('\n').filter { it.isNotBlank() }

    private fun encode(ids: List<String>): String = ids.joinToString("\n")

    private companion object {
        val KEY = stringPreferencesKey("plan_backpack_place_ids")
    }
}

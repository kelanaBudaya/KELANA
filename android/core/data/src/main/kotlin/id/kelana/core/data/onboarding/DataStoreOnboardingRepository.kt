package id.kelana.core.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import id.kelana.core.model.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreOnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val completed: Flow<Boolean> = dataStore.data.map { it[KEY_COMPLETED] ?: false }

    override suspend fun markCompleted() {
        dataStore.edit { it[KEY_COMPLETED] = true }
    }

    private companion object {
        val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

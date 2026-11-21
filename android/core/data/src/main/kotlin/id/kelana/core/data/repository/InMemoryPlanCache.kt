package id.kelana.core.data.repository

import id.kelana.core.model.PlanCache
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.TripPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROVISIONAL: holds the last plan in memory only, so it survives screen changes and going offline but not a process
 * restart. Replace with a Room or DataStore backed implementation behind the same interface.
 */
@Singleton
class InMemoryPlanCache @Inject constructor() : PlanCache {
    private val tripState = MutableStateFlow<TripPlan?>(null)
    private val reminderState = MutableStateFlow<Map<String, ReminderChoice>>(emptyMap())

    override val trip: Flow<TripPlan?> = tripState
    override val reminders: Flow<Map<String, ReminderChoice>> = reminderState

    override suspend fun save(trip: TripPlan) {
        tripState.value = trip
    }

    override suspend fun clear() {
        tripState.value = null
    }

    override suspend fun saveReminder(eventId: String, choice: ReminderChoice) {
        reminderState.update { it + (eventId to choice) }
    }
}

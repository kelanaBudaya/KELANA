package id.kelana.feature.rencana.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import id.kelana.core.model.PlanCache
import id.kelana.core.model.ReminderScheduler
import id.kelana.core.model.UserDataCleaner
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Clears the planner's data after "Hapus akun": cancels the scheduled reminder of every show the user chose one for
 * (Pengingat), then clears today's plan ([PlanCache]). Each cancel runs even if an earlier one fails.
 * The reminder choices map itself lives in [PlanCache], which has no call to empty it (in-memory, gone on restart).
 */
class RencanaUserDataCleaner @Inject constructor(
    private val cache: PlanCache,
    private val scheduler: ReminderScheduler,
) : UserDataCleaner {

    override suspend fun clear() {
        var failure: Throwable? = null
        val eventIds = runCatching { cache.reminders.first().keys }.onFailure { failure = it }.getOrDefault(emptySet())
        for (eventId in eventIds) {
            runCatching { scheduler.cancelShowReminder(eventId) }.onFailure { failure = failure ?: it }
        }
        runCatching { cache.clear() }.onFailure { failure = failure ?: it }
        failure?.let { throw it }
    }
}

/** Registers [RencanaUserDataCleaner] (set declared in `:core:data`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RencanaCleanerModule {
    @Binds @IntoSet abstract fun bindUserDataCleaner(impl: RencanaUserDataCleaner): UserDataCleaner
}

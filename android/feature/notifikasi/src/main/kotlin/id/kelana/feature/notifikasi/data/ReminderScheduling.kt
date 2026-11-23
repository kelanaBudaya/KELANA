package id.kelana.feature.notifikasi.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.kelana.core.model.EventRepository
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.NotificationRepository
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderScheduler
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.notifikasi.domain.ReminderTimes
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit

/** One pending reminder per event. A seam over WorkManager so the scheduling rules can be tested. */
interface ReminderWork {
    /** Replaces any earlier reminder of [eventId] by one that fires after [delay]. */
    fun enqueue(eventId: String, timing: ReminderTiming, delay: Duration)

    fun cancel(eventId: String)
}

class WorkManagerReminderWork(private val context: Context) : ReminderWork {

    override fun enqueue(eventId: String, timing: ReminderTiming, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ReminderWorker.KEY_EVENT_ID, eventId)
                    .putString(ReminderWorker.KEY_TIMING, timing.name)
                    .build(),
            )
            .build()
        // One unique work per event: choosing again REPLACEs the earlier reminder.
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName(eventId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(eventId))
    }

    internal companion object {
        fun uniqueName(eventId: String) = "kelana.reminder.$eventId"
    }
}

/**
 * Schedules the reminder chosen on the Pengingat sheet. The reminder fires at the show start minus the lead time of
 * the chosen timing ([ReminderTimes]). Never reads location or live travel time. A reminder whose moment has
 * already passed is not scheduled (and clears an earlier one). A show whose time has no check date is not
 * scheduled either: its time is not a published fact yet.
 */
class LocalReminderScheduler(
    private val events: EventRepository,
    private val work: ReminderWork,
    private val clock: Clock,
) : ReminderScheduler {

    override suspend fun scheduleShowReminder(eventId: String, choice: ReminderChoice) {
        val event = events.event(eventId).first()
        if (event == null || event.schedule.checkedAt == null) {
            work.cancel(eventId)
            return
        }
        val delay = ReminderTimes.delayUntilReminder(event.schedule.value, choice.timing, clock.instant())
        if (delay == null) work.cancel(eventId) else work.enqueue(eventId, choice.timing, delay)
    }

    override suspend fun cancelShowReminder(eventId: String) = work.cancel(eventId)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun notifications(): NotificationRepository
}

/**
 * Posts [NotificationEvent.ShowReminderDue] when the reminder moment arrives. Gets its dependency from a Hilt entry
 * point (like the stamp worker) so `:app` needs no WorkManager Configuration.Provider.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val timing = inputData.getString(KEY_TIMING)
            ?.let { name -> ReminderTiming.entries.firstOrNull { it.name == name } }
            ?: return Result.failure()
        EntryPointAccessors.fromApplication(applicationContext, ReminderWorkerEntryPoint::class.java)
            .notifications()
            .post(NotificationEvent.ShowReminderDue(eventId, timing))
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "eventId"
        const val KEY_TIMING = "timing"
    }
}

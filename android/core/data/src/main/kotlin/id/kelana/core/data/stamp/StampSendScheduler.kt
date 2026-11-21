package id.kelana.core.data.stamp

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.kelana.core.model.StampResult
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Asks the system to send the pending stamp once there is a network. */
interface StampSendScheduler {
    fun schedule()
    fun cancel()
}

@Singleton
class WorkManagerStampSendScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : StampSendScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<StampSendWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // One pending request at most, so one job at most. A newer request is picked up by the job in flight.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "kelana.stamp.send"
        const val BACKOFF_SECONDS = 30L
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StampWorkerEntryPoint {
    fun stampSender(): StampSender
}

/**
 * Sends the pending stamp when the network is CONNECTED, with exponential backoff. The Integrity token is
 * requested inside [StampSender] at send time. A final answer (stamped, rejected, expired, ...) ends the job
 * and the row is already deleted; only "no answer yet" retries, and the sender drops the row after 30 minutes.
 *
 * Gets its dependencies from a Hilt entry point instead of `@HiltWorker`, so the default WorkManager
 * initializer keeps working and `:app` needs no Configuration.Provider.
 */
class StampSendWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender = EntryPointAccessors
            .fromApplication(applicationContext, StampWorkerEntryPoint::class.java)
            .stampSender()
        return when (sender.sendPending()) {
            StampResult.NetworkError -> Result.retry()
            else -> Result.success() // null (nothing pending) or a final answer
        }
    }
}

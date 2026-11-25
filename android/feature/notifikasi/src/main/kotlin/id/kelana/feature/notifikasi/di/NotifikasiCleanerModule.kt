package id.kelana.feature.notifikasi.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import id.kelana.core.model.UserDataCleaner
import id.kelana.feature.notifikasi.data.NotificationStorage
import id.kelana.feature.notifikasi.data.NotifikasiUserDataCleaner
import id.kelana.feature.notifikasi.data.ReminderWorker

/** Registers the notification centre's clean-up for "Hapus akun" (set declared in `:core:data`). */
@Module
@InstallIn(SingletonComponent::class)
object NotifikasiCleanerModule {

    @Provides
    @IntoSet
    fun provideUserDataCleaner(
        @ApplicationContext context: Context,
        storage: NotificationStorage,
    ): UserDataCleaner = NotifikasiUserDataCleaner(
        storage = storage,
        // WorkManager tags every request with its worker's class name, so this cancels every show reminder,
        // whatever event it was for.
        cancelScheduledReminders = { WorkManager.getInstance(context).cancelAllWorkByTag(ReminderWorker::class.java.name) },
        dismissShown = { NotificationManagerCompat.from(context).cancelAll() },
    )
}

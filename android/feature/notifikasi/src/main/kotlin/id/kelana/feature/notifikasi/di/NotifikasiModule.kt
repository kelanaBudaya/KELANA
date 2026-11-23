package id.kelana.feature.notifikasi.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.kelana.core.model.EventRepository
import id.kelana.core.model.NotificationRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.ReminderScheduler
import id.kelana.feature.notifikasi.data.AndroidSystemNotifier
import id.kelana.feature.notifikasi.data.DataStoreNotificationSettings
import id.kelana.feature.notifikasi.data.DataStoreNotificationStorage
import id.kelana.feature.notifikasi.data.LocalNotificationRepository
import id.kelana.feature.notifikasi.data.LocalReminderScheduler
import id.kelana.feature.notifikasi.data.NotificationStorage
import id.kelana.feature.notifikasi.data.ProcessForeground
import id.kelana.feature.notifikasi.data.WorkManagerReminderWork
import id.kelana.feature.notifikasi.domain.NotificationContentResolver
import id.kelana.feature.notifikasi.domain.NotificationSettingsRepository
import id.kelana.feature.notifikasi.domain.TextSource
import java.time.Clock
import javax.inject.Singleton

/** Binds the local notification center and the WorkManager reminder scheduler (precedent: PasporModule). */
@Module
@InstallIn(SingletonComponent::class)
object NotifikasiModule {

    @Provides
    @Singleton
    fun provideStorage(dataStore: DataStore<Preferences>): NotificationStorage = DataStoreNotificationStorage(dataStore)

    @Provides
    @Singleton
    fun provideSettings(dataStore: DataStore<Preferences>): NotificationSettingsRepository =
        DataStoreNotificationSettings(dataStore)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        @ApplicationContext context: Context,
        storage: NotificationStorage,
        settings: NotificationSettingsRepository,
        places: PlaceRepository,
        events: EventRepository,
        clock: Clock,
    ): NotificationRepository = LocalNotificationRepository(
        storage = storage,
        settings = settings,
        resolver = NotificationContentResolver(places, events, TextSource { id, args -> context.getString(id, *args) }),
        notifier = AndroidSystemNotifier(context),
        foreground = ProcessForeground(),
        clock = clock,
    )

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context,
        events: EventRepository,
        clock: Clock,
    ): ReminderScheduler = LocalReminderScheduler(events, WorkManagerReminderWork(context), clock)
}

package id.kelana.core.database.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.kelana.core.database.KelanaDatabase
import id.kelana.core.database.dao.EventDao
import id.kelana.core.database.dao.PlaceDao
import id.kelana.core.database.dao.RegionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KelanaDatabase =
        Room.databaseBuilder(context, KelanaDatabase::class.java, "kelana.db")
            // Only public content is cached and it is re-downloaded, so dropping it on upgrade is safe.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRegionDao(db: KelanaDatabase): RegionDao = db.regionDao()

    @Provides
    fun providePlaceDao(db: KelanaDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideEventDao(db: KelanaDatabase): EventDao = db.eventDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("kelana_prefs") }
}

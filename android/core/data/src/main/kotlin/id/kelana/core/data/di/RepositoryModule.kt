package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.remote.KelanaRemoteDataSource
import id.kelana.core.data.remote.SupabaseRemoteDataSource
import id.kelana.core.data.repository.SupabaseEventRepository
import id.kelana.core.data.repository.SupabasePlaceRepository
import id.kelana.core.data.repository.SupabaseRegionRepository
import id.kelana.core.data.repository.SupabaseReportRepository
import id.kelana.core.model.EventRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.ReportRepository

/**
 * The single place where content repositories are bound. Production binds the Supabase-backed implementations
 * (Room cache first, then network). The Fake* classes stay for previews and tests; to use one, change the
 * parameter type of the matching function.
 *
 * When SUPABASE_URL or the anon key is empty the Supabase repositories do not crash and do not fall back to
 * sample data: the remote source throws NotConfiguredException, the refresh is skipped, and they serve whatever
 * the cache holds (empty on a fresh install). Reports return Failed in that case.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindRemoteDataSource(impl: SupabaseRemoteDataSource): KelanaRemoteDataSource

    @Binds abstract fun bindPlaceRepository(impl: SupabasePlaceRepository): PlaceRepository

    @Binds abstract fun bindEventRepository(impl: SupabaseEventRepository): EventRepository

    @Binds abstract fun bindRegionRepository(impl: SupabaseRegionRepository): RegionRepository

    @Binds abstract fun bindReportRepository(impl: SupabaseReportRepository): ReportRepository
}

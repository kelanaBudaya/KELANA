package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.repository.FakeEventRepository
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.data.repository.FakeRegionRepository
import id.kelana.core.data.repository.FakeReportRepository
import id.kelana.core.model.EventRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.ReportRepository

/**
 * The single place where content repositories are bound. Today every binding is a fake backed by sample data.
 * To use the Supabase-backed implementation, change the parameter type of the matching function (one line each).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindPlaceRepository(impl: FakePlaceRepository): PlaceRepository

    @Binds abstract fun bindEventRepository(impl: FakeEventRepository): EventRepository

    @Binds abstract fun bindRegionRepository(impl: FakeRegionRepository): RegionRepository

    @Binds abstract fun bindReportRepository(impl: FakeReportRepository): ReportRepository
}

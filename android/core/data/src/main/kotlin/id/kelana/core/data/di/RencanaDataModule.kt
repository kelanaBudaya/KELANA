package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.repository.InMemoryPlanCache
import id.kelana.core.model.PlanCache

/** Bindings of the Rencana planner. Kept apart from [RepositoryModule] so features do not collide on one file. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RencanaDataModule {
    @Binds abstract fun bindPlanCache(impl: InMemoryPlanCache): PlanCache
}

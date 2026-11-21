package id.kelana.feature.rencana.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.model.ConnectivityMonitor
import id.kelana.feature.rencana.domain.PlanRepository

/**
 * Bindings of this feature. The plan repository is implemented here (not in :core:data) because the planner rules live
 * in this module; the cache binding is in `:core:data` (`RencanaDataModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RencanaModule {
    @Binds abstract fun bindPlanRepository(impl: LocalPlanRepository): PlanRepository

    @Binds abstract fun bindConnectivity(impl: AndroidConnectivityMonitor): ConnectivityMonitor
}

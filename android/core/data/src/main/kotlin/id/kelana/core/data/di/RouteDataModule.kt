package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.route.RouteRemote
import id.kelana.core.data.route.SupabaseRouteRemote
import id.kelana.core.data.route.SupabaseRouteRepository
import id.kelana.core.model.RouteRepository

/** Road routing for Peta rute. Kept in its own file so features do not collide on one module. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RouteDataModule {
    @Binds abstract fun bindRouteRemote(impl: SupabaseRouteRemote): RouteRemote

    @Binds abstract fun bindRouteRepository(impl: SupabaseRouteRepository): RouteRepository
}

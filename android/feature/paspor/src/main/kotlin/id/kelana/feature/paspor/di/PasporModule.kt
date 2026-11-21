package id.kelana.feature.paspor.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.feature.paspor.data.FusedLocationFixSource
import id.kelana.feature.paspor.data.LocationFixSource

@Module
@InstallIn(SingletonComponent::class)
abstract class PasporModule {
    @Binds abstract fun bindLocationFixSource(impl: FusedLocationFixSource): LocationFixSource
}

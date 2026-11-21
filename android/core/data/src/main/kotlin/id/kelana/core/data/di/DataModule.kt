package id.kelana.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Repository bindings land here once the data contract exists. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule

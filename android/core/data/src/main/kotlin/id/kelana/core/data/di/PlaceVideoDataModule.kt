package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.placevideo.PlaceVideoRemote
import id.kelana.core.data.placevideo.SupabasePlaceVideoRemote
import id.kelana.core.data.placevideo.SupabasePlaceVideoRepository
import id.kelana.core.model.PlaceVideoRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceVideoDataModule {
    @Binds abstract fun bindPlaceVideoRemote(impl: SupabasePlaceVideoRemote): PlaceVideoRemote

    @Binds abstract fun bindPlaceVideoRepository(impl: SupabasePlaceVideoRepository): PlaceVideoRepository
}

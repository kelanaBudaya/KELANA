package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.placephoto.PlaceCoverRemote
import id.kelana.core.data.placephoto.PlacePhotoRemote
import id.kelana.core.data.placephoto.SupabasePlaceCoverRemote
import id.kelana.core.data.placephoto.SupabasePlaceCoverRepository
import id.kelana.core.data.placephoto.SupabasePlacePhotoRemote
import id.kelana.core.data.placephoto.SupabasePlacePhotoRepository
import id.kelana.core.model.PlaceCoverRepository
import id.kelana.core.model.PlacePhotoRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class PlacePhotoDataModule {
    @Binds abstract fun bindPlacePhotoRemote(impl: SupabasePlacePhotoRemote): PlacePhotoRemote

    @Binds abstract fun bindPlacePhotoRepository(impl: SupabasePlacePhotoRepository): PlacePhotoRepository

    @Binds abstract fun bindPlaceCoverRemote(impl: SupabasePlaceCoverRemote): PlaceCoverRemote

    @Binds abstract fun bindPlaceCoverRepository(impl: SupabasePlaceCoverRepository): PlaceCoverRepository
}

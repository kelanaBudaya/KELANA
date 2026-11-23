package id.kelana.feature.postcard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.feature.postcard.data.AndroidCardExporter
import id.kelana.feature.postcard.data.AndroidPhotoProcessor
import id.kelana.feature.postcard.data.AndroidVideoMetadataReader
import id.kelana.feature.postcard.domain.CardExporter
import id.kelana.feature.postcard.domain.PhotoProcessor
import id.kelana.feature.postcard.domain.VideoMetadataReader

@Module
@InstallIn(SingletonComponent::class)
abstract class PostcardModule {
    @Binds abstract fun bindPhotoProcessor(impl: AndroidPhotoProcessor): PhotoProcessor

    @Binds abstract fun bindCardExporter(impl: AndroidCardExporter): CardExporter

    @Binds abstract fun bindVideoMetadataReader(impl: AndroidVideoMetadataReader): VideoMetadataReader
}

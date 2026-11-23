package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.video.FakeVideoUploadRepository
import id.kelana.core.model.VideoUploadRepository

/**
 * Binds [VideoUploadRepository]. FAKE for now: the video host is not chosen. When the real upload exists, change
 * only the parameter type here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VideoDataModule {
    @Binds abstract fun bindVideoUploadRepository(impl: FakeVideoUploadRepository): VideoUploadRepository
}

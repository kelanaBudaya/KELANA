package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.repository.EmptyStoryRepository
import id.kelana.core.model.StoryRepository

/**
 * Binds [StoryRepository]. Production serves an empty feed ([EmptyStoryRepository]) until the Supabase-backed
 * repository (tables `postcards` and `videos`, Realtime) is written; then change only this parameter type.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StoryDataModule {
    @Binds abstract fun bindStoryRepository(impl: EmptyStoryRepository): StoryRepository
}

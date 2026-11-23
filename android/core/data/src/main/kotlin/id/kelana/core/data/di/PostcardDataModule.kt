package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.postcard.PostcardRemote
import id.kelana.core.data.postcard.SupabasePostcardRemote
import id.kelana.core.data.postcard.SupabasePostcardRepository
import id.kelana.core.model.PostcardRepository

/** Binds [PostcardRepository] to the Supabase one; it answers NotConfigured when the build has no Supabase keys. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PostcardDataModule {
    @Binds abstract fun bindPostcardRemote(impl: SupabasePostcardRemote): PostcardRemote

    @Binds abstract fun bindPostcardRepository(impl: SupabasePostcardRepository): PostcardRepository
}

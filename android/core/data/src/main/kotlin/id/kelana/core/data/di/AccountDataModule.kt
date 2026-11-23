package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.account.AccountRemote
import id.kelana.core.data.account.SupabaseAccountDeletionRepository
import id.kelana.core.data.account.SupabaseAccountRemote
import id.kelana.core.model.AccountDeletionRepository

/** "Hapus akun" bindings. Kept in its own file so features do not collide on one module. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDataModule {
    @Binds abstract fun bindAccountRemote(impl: SupabaseAccountRemote): AccountRemote

    @Binds abstract fun bindAccountDeletionRepository(impl: SupabaseAccountDeletionRepository): AccountDeletionRepository
}

package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import id.kelana.core.data.account.AccountExportRemote
import id.kelana.core.data.account.ContentResolverExportDocumentWriter
import id.kelana.core.data.account.ExportDocumentWriter
import id.kelana.core.data.account.SupabaseAccountExportRemote
import id.kelana.core.data.account.SupabaseAccountExportRepository
import id.kelana.core.model.AccountExportRepository
import id.kelana.core.model.UserDataCleaner

/**
 * "Unduh dataku dulu" bindings, and the declaration of the [UserDataCleaner] set: each owner adds its own cleaner
 * with `@IntoSet`; declared here so the set exists (possibly empty) in every build.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountExportDataModule {
    @Binds abstract fun bindAccountExportRemote(impl: SupabaseAccountExportRemote): AccountExportRemote

    @Binds abstract fun bindExportDocumentWriter(impl: ContentResolverExportDocumentWriter): ExportDocumentWriter

    @Binds abstract fun bindAccountExportRepository(impl: SupabaseAccountExportRepository): AccountExportRepository

    @Multibinds abstract fun userDataCleaners(): Set<UserDataCleaner>
}

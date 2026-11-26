package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.language.AppCompatLocaleController
import id.kelana.core.data.language.AppLocaleController
import id.kelana.core.data.language.PlatformAppLanguageRepository
import id.kelana.core.model.AppLanguageRepository

/** Per-app language bindings. Kept in its own file so features do not collide on one module. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppLanguageDataModule {
    @Binds abstract fun bindAppLocaleController(impl: AppCompatLocaleController): AppLocaleController

    @Binds abstract fun bindAppLanguageRepository(impl: PlatformAppLanguageRepository): AppLanguageRepository
}

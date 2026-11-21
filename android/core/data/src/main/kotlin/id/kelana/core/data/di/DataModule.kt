package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.data.auth.SupabaseSessionRepository
import id.kelana.core.data.onboarding.DataStoreOnboardingRepository
import id.kelana.core.model.AuthConfig
import id.kelana.core.model.OnboardingRepository
import id.kelana.core.model.SessionRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindSessionRepository(impl: SupabaseSessionRepository): SessionRepository

    @Binds
    abstract fun bindOnboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    companion object {
        @Provides
        fun provideAuthConfig(provider: SupabaseClientProvider): AuthConfig = provider.authConfig
    }
}

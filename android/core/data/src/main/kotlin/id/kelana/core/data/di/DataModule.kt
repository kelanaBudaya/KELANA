package id.kelana.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.kelana.core.data.auth.EmailAuthApi
import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.data.auth.SupabaseEmailAuthApi
import id.kelana.core.data.auth.SupabaseEmailAuthRepository
import id.kelana.core.data.auth.SupabaseSessionRepository
import id.kelana.core.data.onboarding.DataStoreOnboardingRepository
import id.kelana.core.data.BuildConfig
import id.kelana.core.data.stamp.ConnectivityNetworkMonitor
import id.kelana.core.data.stamp.DevIntegrityTokenProvider
import id.kelana.core.data.stamp.IntegrityTokenProvider
import id.kelana.core.data.stamp.NetworkMonitor
import id.kelana.core.data.stamp.PlayIntegrityTokenProvider
import id.kelana.core.data.stamp.StampRemote
import id.kelana.core.data.stamp.StampSendScheduler
import id.kelana.core.data.stamp.SupabaseStampRemote
import id.kelana.core.data.stamp.WorkManagerStampSendScheduler
import java.time.Clock
import id.kelana.core.model.AuthConfig
import id.kelana.core.model.EmailAuthRepository
import id.kelana.core.model.OnboardingRepository
import id.kelana.core.model.SessionRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindSessionRepository(impl: SupabaseSessionRepository): SessionRepository

    @Binds
    abstract fun bindEmailAuthApi(impl: SupabaseEmailAuthApi): EmailAuthApi

    @Binds
    abstract fun bindEmailAuthRepository(impl: SupabaseEmailAuthRepository): EmailAuthRepository

    @Binds
    abstract fun bindOnboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    @Binds
    abstract fun bindStampRemote(impl: SupabaseStampRemote): StampRemote

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun bindStampSendScheduler(impl: WorkManagerStampSendScheduler): StampSendScheduler

    companion object {
        @Provides
        fun provideAuthConfig(provider: SupabaseClientProvider): AuthConfig = provider.authConfig

        @Provides
        fun provideClock(): Clock = Clock.systemUTC()

        /**
         * Production default is the real Play Integrity provider. The dev stub (`dev-token`, accepted only by the
         * server with KELANA_ENV=dev) needs a DEBUG build AND `integrity.devToken=true`; a release build ignores it.
         * An empty or invalid `google.cloudProjectNumber` makes the real provider answer NotConfigured.
         */
        @Provides
        fun provideIntegrityTokenProvider(@ApplicationContext context: Context): IntegrityTokenProvider =
            if (BuildConfig.DEBUG && BuildConfig.INTEGRITY_DEV_TOKEN) {
                DevIntegrityTokenProvider()
            } else {
                PlayIntegrityTokenProvider(
                    context,
                    BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER.toLongOrNull()?.takeIf { it > 0 },
                )
            }
    }
}

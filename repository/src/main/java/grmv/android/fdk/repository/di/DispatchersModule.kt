package grmv.android.fdk.repository.di

import grmv.android.fdk.repository.BaseDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [BaseDispatchers] as a process-scoped singleton.
 *
 * Inject [BaseDispatchers] anywhere in the DI graph to obtain the coroutine dispatchers
 * without a direct dependency on `kotlinx.coroutines.Dispatchers`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideBaseDispatchers(): BaseDispatchers = BaseDispatchers()
}

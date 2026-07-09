package grmv.android.fdk.crypto

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton

/**
 * Provides the singleton [CryptoManager] for injection.
 *
 * Uses the consumer-bound [CryptoConfig] if one is present, otherwise [DefaultCryptoConfig].
 */
@Module
@InstallIn(SingletonComponent::class)
class CryptoModule {

    @Provides
    @Singleton
    fun provideCryptoManager(
        @ApplicationContext context: Context,
        config: Optional<CryptoConfig>,
    ): CryptoManager {
        return CryptoManager(context, config.orElse(DefaultCryptoConfig))
    }
}
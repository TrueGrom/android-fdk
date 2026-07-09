package grmv.android.fdk.crypto

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Optional crypto tuning the consuming app MAY provide.
 *
 * If no implementation is bound, [DefaultCryptoConfig] is used. To override, bind an
 * implementation in a Hilt module:
 * ```kotlin
 * @Binds
 * fun bindCryptoConfig(impl: MyCryptoConfig): CryptoConfig
 * ```
 *
 * Changing [keysetName]/[prefFileName]/[masterKeyUri] points at a different keyset — existing
 * ciphertext encrypted under the previous values becomes undecryptable.
 */
interface CryptoConfig {
    /** Keystore URI of the master key that wraps the keyset. Default: `android-keystore://master_key`. */
    val masterKeyUri: String

    /** Name under which the wrapped keyset is stored in [prefFileName]. Default: `encrypted_datastore_keyset`. */
    val keysetName: String

    /** SharedPreferences file holding the encrypted keyset. Default: `tink_prefs`. */
    val prefFileName: String

    /** Tink key template name. Default: `AES256_GCM`. */
    val keyTemplate: String

    /**
     * Default associated data applied when a caller omits `aad`. `null` falls back to the device
     * `ANDROID_ID`. Note: AAD is authenticated, not secret — do not put confidential data here.
     * Default: `null`.
     */
    val defaultAad: ByteArray?
}

internal object DefaultCryptoConfig : CryptoConfig {
    override val masterKeyUri: String = "android-keystore://master_key"
    override val keysetName: String = "encrypted_datastore_keyset"
    override val prefFileName: String = "tink_prefs"
    override val keyTemplate: String = "AES256_GCM"
    override val defaultAad: ByteArray? = null
}

@Module
@InstallIn(SingletonComponent::class)
internal interface CryptoConfigModule {
    @BindsOptionalOf
    fun bindOptionalCryptoConfig(): CryptoConfig
}

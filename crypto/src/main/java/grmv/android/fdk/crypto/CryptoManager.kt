package grmv.android.fdk.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlin.jvm.java

/**
 * AES-256-GCM encrypt/decrypt backed by the Android Keystore via Tink.
 *
 * The keyset is wrapped by a hardware-backed Android Keystore master key, so ciphertext is
 * inherently bound to this device — it cannot be decrypted on another device or once the
 * Keystore key is lost (app data clear / uninstall / factory reset).
 *
 * Every operation accepts optional associated data (`aad`): authenticated-but-not-encrypted
 * context (e.g. `"refresh_token"`) that binds ciphertext to a logical purpose. The identical
 * `aad` must be supplied to decrypt; a mismatch makes decryption fail. AAD is *not* secret and
 * *not* a salt — never place confidential data in it. When `aad` is omitted it defaults to
 * [CryptoConfig.defaultAad], falling back to the device `ANDROID_ID` (or empty bytes if that is
 * unavailable).
 *
 * Inject this type directly; it is provided as a single `@Singleton` by [CryptoModule].
 */
class CryptoManager internal constructor(
    context: Context,
    config: CryptoConfig,
) {
    @SuppressLint("HardwareIds")
    private val defaultAad: ByteArray = config.defaultAad
        ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.toByteArray(Charsets.UTF_8)
        ?: ByteArray(0)

    init {
        AeadConfig.register()
    }

    private val aead: Aead = AndroidKeysetManager.Builder()
        .withSharedPref(context, config.keysetName, config.prefFileName)
        .withKeyTemplate(KeyTemplates.get(config.keyTemplate))
        .withMasterKeyUri(config.masterKeyUri)
        .build()
        .keysetHandle
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    /**
     * Encrypts a UTF-8 string and returns the ciphertext as a Base64 no-wrap string.
     *
     * @param aad associated data bound to the ciphertext; the same value must be passed to
     * [decryptString]. Defaults to the manager's default AAD ([CryptoConfig.defaultAad], else the
     * device `ANDROID_ID`).
     * @return failure if the Keystore is unavailable.
     */
    fun encryptString(data: String, aad: ByteArray = defaultAad): Result<String> {
        return encryptBytes(data.toByteArray(Charsets.UTF_8), aad)
            .map { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    /**
     * Decrypts a Base64 no-wrap ciphertext produced by [encryptString].
     *
     * @param aad must match the value supplied to [encryptString].
     * @return failure if the input is malformed, the `aad` does not match, or the Keystore is
     * unavailable.
     */
    fun decryptString(encryptedData: String, aad: ByteArray = defaultAad): Result<String> {
        return runCatching { Base64.decode(encryptedData, Base64.NO_WRAP) }
            .mapCatching { decryptBytes(it, aad).getOrThrow() }
            .map { String(it, Charsets.UTF_8) }
    }

    /**
     * Encrypts raw bytes without Base64 encoding. Prefer over [encryptString] for binary data.
     *
     * @param aad associated data bound to the ciphertext; the same value must be passed to
     * [decryptBytes]. Defaults to the manager's default AAD ([CryptoConfig.defaultAad], else the
     * device `ANDROID_ID`).
     * @return failure if the Keystore is unavailable.
     */
    fun encryptBytes(data: ByteArray, aad: ByteArray = defaultAad): Result<ByteArray> {
        return runCatching {
            aead.encrypt(data, aad)
        }
    }

    /**
     * Decrypts raw ciphertext produced by [encryptBytes].
     *
     * @param aad must match the value supplied to [encryptBytes].
     * @return failure if the `aad` does not match, or the Keystore is unavailable.
     */
    fun decryptBytes(encryptedData: ByteArray, aad: ByteArray = defaultAad): Result<ByteArray> {
        return runCatching {
            aead.decrypt(encryptedData, aad)
        }
    }
}
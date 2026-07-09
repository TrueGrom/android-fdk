package grmv.android.fdk.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Optional JSON serialization tuning the consuming app MAY provide.
 *
 * Controls how the shared [io.ktor.client.HttpClient] serializes and deserializes JSON bodies.
 * If no implementation is bound, [DefaultHttpJsonConfig] is used. To override, bind an
 * implementation in a Hilt module:
 * ```kotlin
 * @Binds
 * fun bindHttpJsonConfig(impl: MyJsonConfig): HttpJsonConfig
 * ```
 *
 * @property prettyPrint Whether outgoing JSON is indented for readability. Default `true`;
 *   set to `false` in production to reduce payload size.
 * @property isLenient Whether to accept malformed JSON (unquoted keys, relaxed literals).
 *   Default `false`; enable only for non-conforming servers.
 * @property ignoreUnknownKeys Whether to silently drop JSON keys with no matching field instead
 *   of failing. Default `true`, which keeps deserialization tolerant of server-added fields.
 * @property explicitNulls Whether properties with `null` values are emitted in the output (and
 *   `null` is required for nullable fields on input). Default `false`, so nulls are omitted.
 */
interface HttpJsonConfig {
    val prettyPrint: Boolean
    val isLenient: Boolean
    val ignoreUnknownKeys: Boolean
    val explicitNulls: Boolean
}

internal object DefaultHttpJsonConfig : HttpJsonConfig {
    override val prettyPrint: Boolean = true
    override val isLenient: Boolean = false
    override val ignoreUnknownKeys: Boolean = true
    override val explicitNulls: Boolean = false
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HttpJsonConfigModule {
    @BindsOptionalOf
    fun bindOptionalHttpJsonConfig(): HttpJsonConfig
}

package grmv.android.fdk.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Optional HTTP tuning the consuming app MAY provide.
 *
 * Controls connection timeouts and response handling for the shared
 * [io.ktor.client.HttpClient]. If no implementation is bound, [DefaultHttpTimeoutConfig] is used.
 * To override, bind an implementation in a Hilt module:
 * ```kotlin
 * @Binds
 * fun bindHttpTimeoutConfig(impl: MyTimeoutConfig): HttpTimeoutConfig
 * ```
 *
 * @property connectTimeout Maximum time to establish a TCP connection to the host. Default `30s`.
 * @property readTimeout Maximum idle time between receiving data packets. Default `20s`.
 * @property writeTimeout Maximum idle time between sending data packets. Default `35s`.
 * @property followRedirects Whether the engine automatically follows HTTP redirects. Default
 *   `false`, so 3xx responses are surfaced to the caller rather than transparently followed.
 * @property expectSuccess Whether non-2xx responses are treated as failures. When `true`
 *   (the default), the client throws on 3xx/4xx/5xx responses; set to `false` to inspect
 *   status codes manually.
 */
interface HttpTimeoutConfig {
    val connectTimeout: Duration
    val readTimeout: Duration
    val writeTimeout: Duration
    val followRedirects: Boolean
    val expectSuccess: Boolean
}

internal object DefaultHttpTimeoutConfig : HttpTimeoutConfig {
    override val connectTimeout: Duration = 30.seconds
    override val readTimeout: Duration = 20.seconds
    override val writeTimeout: Duration = 35.seconds
    override val followRedirects: Boolean = false
    override val expectSuccess: Boolean = true
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HttpTimeoutConfigModule {
    @BindsOptionalOf
    fun bindOptionalHttpTimeoutConfig(): HttpTimeoutConfig
}

package grmv.android.fdk.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.http.HttpMethod

/**
 * Optional automatic-retry tuning the consuming app MAY provide.
 *
 * Controls how the shared [io.ktor.client.HttpClient] replays failed requests — 5xx responses and
 * network [java.io.IOException]s — with exponential backoff. If no implementation is bound,
 * [DefaultHttpRetryConfig] is used. To override, bind an implementation in a Hilt module:
 * ```kotlin
 * @Binds
 * fun bindHttpRetryConfig(impl: MyRetryConfig): HttpRetryConfig
 * ```
 *
 * @property maxRetries Maximum retry attempts per request. Default `3`; set to `0` to disable
 *   automatic retries entirely, which skips installing the plugin.
 * @property retryableMethods HTTP methods eligible for an automatic replay. Default `GET`/`HEAD`/
 *   `OPTIONS` — replaying `POST`/`PUT`/`PATCH`/`DELETE` after a timeout can duplicate a request the
 *   server already committed. Widen this only when the API makes such requests idempotent
 *   (e.g. idempotency keys).
 */
interface HttpRetryConfig {
    val maxRetries: Int
    val retryableMethods: Set<HttpMethod>
}

internal object DefaultHttpRetryConfig : HttpRetryConfig {
    override val maxRetries: Int = 3
    override val retryableMethods: Set<HttpMethod> =
        setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HttpRetryConfigModule {
    @BindsOptionalOf
    fun bindOptionalHttpRetryConfig(): HttpRetryConfig
}

package grmv.android.fdk.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import java.io.IOException

/**
 * Optional automatic-retry tuning the consuming app MAY provide.
 *
 * Controls how the shared [io.ktor.client.HttpClient] replays failed requests — 5xx responses and
 * network [java.io.IOException]s — with exponential backoff. If no implementation is bound,
 * [DefaultHttpRetryConfig] is used, and it retries nothing: automatic replay is opt-in, because
 * only the app knows whether its API is safe to replay and whether its UI can afford the wait.
 * To turn it on, bind an implementation in a Hilt module:
 * ```kotlin
 * @Binds
 * fun bindHttpRetryConfig(impl: MyRetryConfig): HttpRetryConfig
 * ```
 *
 * @property maxRetries Maximum retry attempts per request. Default `0` — the plugin is not
 *   installed at all and every request fails on its first attempt. Raising it buys Ktor's
 *   `exponentialDelay()`: three attempts sleep roughly 1s, 2s and 4s plus jitter, so a failure the
 *   app renders as a loading state stays on screen at least ~7-10s longer before it resolves —
 *   longer still against a server that answers with `Retry-After`, which that delay honours.
 *   Weigh it against [shouldRetry], which can fail fast where backoff cannot help.
 * @property retryableMethods HTTP methods eligible for an automatic replay. Default `GET`/`HEAD`/
 *   `OPTIONS` — replaying `POST`/`PUT`/`PATCH`/`DELETE` after a timeout can duplicate a request the
 *   server already committed. Widen this only when the API makes such requests idempotent
 *   (e.g. idempotency keys).
 */
interface HttpRetryConfig {
    val maxRetries: Int
    val retryableMethods: Set<HttpMethod>

    /**
     * Whether this particular failure is worth replaying at all.
     *
     * Consulted once per replay, for both failure shapes, and only after [maxRetries] and
     * [retryableMethods] have already admitted it. Return `false` to surface the failure at once
     * instead of spending the backoff on it.
     *
     * Defaults to `true`, i.e. the two settings above decide alone — the behaviour of every
     * binding that ignores this method. The typical override answers from state the SDK cannot
     * see: with no transport at all (`ConnectivityManager` reports no connection) no amount of
     * backoff will help, and the attempts only delay an error the app could show immediately.
     *
     * Called on the client's request pipeline; keep it cheap and non-blocking.
     *
     * @param context what failed, and on which replay.
     */
    fun shouldRetry(context: HttpRetryContext): Boolean = true
}

/**
 * What [HttpRetryConfig.shouldRetry] is deciding about: one failed attempt, and the replay that
 * would follow it.
 *
 * Exactly one of [status] and [cause] is set — [status] for a 5xx response that came back, [cause]
 * for a transport failure where nothing did.
 *
 * A single object rather than loose parameters: Ktor hands the two retry hooks two different
 * request types, so they have to be reduced to something common anyway — and once reduced, a
 * later addition can arrive as a defaulted property here instead of breaking every override of
 * [HttpRetryConfig.shouldRetry].
 *
 * @property method HTTP method of the request about to be replayed.
 * @property url its full URL.
 * @property status status of the failed response, or `null` for a transport failure.
 * @property cause the transport failure, or `null` when a response came back.
 * @property retry which replay this would be, counting from `1`.
 */
data class HttpRetryContext(
    val method: HttpMethod,
    val url: Url,
    val status: HttpStatusCode?,
    val cause: Throwable?,
    val retry: Int,
)

internal object DefaultHttpRetryConfig : HttpRetryConfig {
    override val maxRetries: Int = 0
    override val retryableMethods: Set<HttpMethod> =
        setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)
}

/**
 * Whether a response that came back should be replayed: 5xx, an admitted method, and the config's
 * own verdict, in that order.
 *
 * Ktor calls this for *every* response, 2xx included, so the cheap checks come first and [url] is
 * a lambda — the [HttpRetryContext] is built only once a replay is actually on the table.
 */
internal inline fun HttpRetryConfig.shouldReplayResponse(
    status: HttpStatusCode,
    method: HttpMethod,
    retry: Int,
    url: () -> Url,
): Boolean =
    status.value in 500..599 &&
        method in retryableMethods &&
        shouldRetry(HttpRetryContext(method, url(), status, null, retry))

/**
 * Whether a request that failed in transport should be replayed.
 *
 * Only an [IOException] qualifies: anything else — a serialization failure, a cancellation — is
 * not going to come out differently the second time. Otherwise as [shouldReplayResponse].
 */
internal inline fun HttpRetryConfig.shouldReplayFailure(
    cause: Throwable,
    method: HttpMethod,
    retry: Int,
    url: () -> Url,
): Boolean =
    cause is IOException &&
        method in retryableMethods &&
        shouldRetry(HttpRetryContext(method, url(), null, cause, retry))

@Module
@InstallIn(SingletonComponent::class)
internal interface HttpRetryConfigModule {
    @BindsOptionalOf
    fun bindOptionalHttpRetryConfig(): HttpRetryConfig
}

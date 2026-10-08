package grmv.android.fdk.network

/**
 * Mandatory HTTP configuration the consuming app MUST provide.
 *
 * Supplies the base URL applied to every request made by the shared
 * [io.ktor.client.HttpClient]. Unlike the optional config interfaces, this binding is required:
 * Hilt injects it directly, so the dependency graph fails to build if no implementation is bound.
 *
 * Bind an implementation in a Hilt module, e.g.:
 * ```kotlin
 * @Binds
 * fun bindHttpConfig(impl: MyHttpConfig): HttpConfigProvider
 * ```
 */
interface HttpConfigProvider {
    /**
     * Returns the base URL prepended to relative request paths (e.g. `https://api.example.com/`).
     * Include a trailing slash so relative paths resolve as expected.
     *
     * Called for every request, on the client's request pipeline, so an implementation may return
     * a different URL over time and the next request picks it up. Keep it cheap, non-blocking and
     * free of side effects. Requests run in parallel, so it may be called from several threads at
     * once: read the URL from thread-safe state (a `@Volatile` field, `StateFlow.value`). An
     * automatic retry does not call it again: see [HttpRetryConfig].
     */
    fun getBaseUrl(): String
}
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
     * Resolved once per provided client; include a trailing slash so relative paths resolve as
     * expected.
     */
    fun getBaseUrl(): String
}
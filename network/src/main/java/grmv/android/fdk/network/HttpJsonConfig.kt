package grmv.android.fdk.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json

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
 * @property coerceInputValues Whether a value the client cannot represent falls back instead of
 *   failing the whole body. Default `true`, and — alone among these five — it carries that default
 *   on the interface, so an app that already binds an [HttpJsonConfig] is not forced to edit it.
 *   Two rules apply, both from kotlinx.serialization's `tryCoerceValue`: an unknown enum constant
 *   decodes to the property's declared default if it has one, or to `null` if the property is
 *   nullable and [explicitNulls] is `false`; and an incoming `null` for a non-nullable property
 *   with a default falls back to that default. Pairs with [ignoreUnknownKeys], which does the same
 *   for a server-added *key*.
 *
 *   Both rules are per *class property*: a list element is neither optional nor defaulted, so an
 *   unknown constant inside a `List<SomeEnum>` still fails the whole body — model those as a raw
 *   `String` and parse them in the repository, or give the enum its own fallback serializer.
 *
 *   The price is silence: a server sending `null` where its contract promises a value no longer
 *   fails loudly, it lands as the default. Set to `false` when an unmodelled value should be a
 *   hard decoding error.
 */
interface HttpJsonConfig {
    val prettyPrint: Boolean
    val isLenient: Boolean
    val ignoreUnknownKeys: Boolean
    val explicitNulls: Boolean
    val coerceInputValues: Boolean get() = true
}

internal object DefaultHttpJsonConfig : HttpJsonConfig {
    override val prettyPrint: Boolean = true
    override val isLenient: Boolean = false
    override val ignoreUnknownKeys: Boolean = true
    override val explicitNulls: Boolean = false
    override val coerceInputValues: Boolean = true
}

/** The [Json] the shared client encodes and decodes with, as described by this config. */
internal fun HttpJsonConfig.buildJson(): Json = Json {
    prettyPrint = this@buildJson.prettyPrint
    isLenient = this@buildJson.isLenient
    ignoreUnknownKeys = this@buildJson.ignoreUnknownKeys
    explicitNulls = this@buildJson.explicitNulls
    coerceInputValues = this@buildJson.coerceInputValues
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HttpJsonConfigModule {
    @BindsOptionalOf
    fun bindOptionalHttpJsonConfig(): HttpJsonConfig
}

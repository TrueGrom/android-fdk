package grmv.android.fdk.network

import dagger.Module

import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt qualifier for the SDK's pre-configured base [io.ktor.client.HttpClient].
 *
 * Apply it at injection sites to obtain the singleton client wired with the consumer's
 * [HttpConfigProvider] base URL and the resolved [HttpTimeoutConfig]/[HttpJsonConfig] settings:
 * ```kotlin
 * @Inject @BaseHttp lateinit var client: HttpClient
 * ```
 */
@Qualifier
annotation class BaseHttp

@Module
@InstallIn(SingletonComponent::class)
internal class HttpClientModule {

    @Provides
    @Singleton
    @BaseHttp
    fun provideBaseHttpClient(
        configProvider: HttpConfigProvider,
        timeoutConfig: Optional<HttpTimeoutConfig>,
        jsonConfig: Optional<HttpJsonConfig>,
    ): HttpClient {
        val timeouts = timeoutConfig.orElse(DefaultHttpTimeoutConfig)
        val jsonSettings = jsonConfig.orElse(DefaultHttpJsonConfig)
        return HttpClient(OkHttp) {
            expectSuccess = timeouts.expectSuccess

            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = jsonSettings.prettyPrint
                        isLenient = jsonSettings.isLenient
                        ignoreUnknownKeys = jsonSettings.ignoreUnknownKeys
                        explicitNulls = jsonSettings.explicitNulls
                    }
                )
            }
            install(HttpRequestRetry)

            defaultRequest {
                contentType(ContentType.Application.Json)
                url(configProvider.getBaseUrl())
            }

            engine {
                config {
                    followRedirects(timeouts.followRedirects)
                    connectTimeout(timeouts.connectTimeout)
                    readTimeout(timeouts.readTimeout)
                    writeTimeout(timeouts.writeTimeout)
                }
            }
        }
    }
}
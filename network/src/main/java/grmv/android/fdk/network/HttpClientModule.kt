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
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt qualifier for the SDK's pre-configured base [io.ktor.client.HttpClient].
 *
 * Apply it at injection sites to obtain the singleton client wired with the consumer's
 * [HttpConfigProvider] base URL and the resolved [HttpTimeoutConfig]/[HttpJsonConfig]/[HttpRetryConfig] settings:
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
        retryConfig: Optional<HttpRetryConfig>,
    ): HttpClient {
        val timeouts = timeoutConfig.orElse(DefaultHttpTimeoutConfig)
        val jsonSettings = jsonConfig.orElse(DefaultHttpJsonConfig)
        val retries = retryConfig.orElse(DefaultHttpRetryConfig)
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
            if (retries.maxRetries > 0) {
                install(HttpRequestRetry) {
                    maxRetries = retries.maxRetries
                    exponentialDelay()
                    retryIf { request, response ->
                        response.status.value in 500..599 && request.method in retries.retryableMethods
                    }
                    retryOnExceptionIf { request, cause ->
                        cause is IOException && request.method in retries.retryableMethods
                    }
                }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
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


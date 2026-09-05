package grmv.android.fdk.network

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HttpRetryConfigTest {

    private val url = Url("https://example.com/books")

    private class RecordingConfig(
        override val maxRetries: Int = 3,
        override val retryableMethods: Set<HttpMethod> = setOf(HttpMethod.Get),
        private val verdict: Boolean = true,
    ) : HttpRetryConfig {
        var seen: HttpRetryContext? = null

        override fun shouldRetry(context: HttpRetryContext): Boolean {
            seen = context
            return verdict
        }
    }

    @Test
    fun `defaults - retries are off until the app opts in`() {
        assertEquals(0, DefaultHttpRetryConfig.maxRetries)
        assertEquals(
            setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options),
            DefaultHttpRetryConfig.retryableMethods,
        )
    }

    @Test
    fun `shouldRetry - a config that ignores it keeps the pre-predicate behaviour`() {
        val bare = object : HttpRetryConfig {
            override val maxRetries: Int = 3
            override val retryableMethods: Set<HttpMethod> = setOf(HttpMethod.Get)
        }

        assertTrue(
            bare.shouldReplayResponse(HttpStatusCode.BadGateway, HttpMethod.Get, retry = 1) { url }
        )
    }

    @Test
    fun `response - 5xx on an admitted method replays`() {
        val config = RecordingConfig()

        assertTrue(
            config.shouldReplayResponse(HttpStatusCode.ServiceUnavailable, HttpMethod.Get, 2) { url }
        )
        val seen = config.seen!!
        assertEquals(HttpMethod.Get, seen.method)
        assertEquals(url, seen.url)
        assertEquals(HttpStatusCode.ServiceUnavailable, seen.status)
        assertNull(seen.cause)
        assertEquals(2, seen.retry)
    }

    @Test
    fun `response - success and 4xx never reach the predicate`() {
        val config = RecordingConfig()

        assertFalse(config.shouldReplayResponse(HttpStatusCode.OK, HttpMethod.Get, 1) { url })
        assertFalse(config.shouldReplayResponse(HttpStatusCode.NotFound, HttpMethod.Get, 1) { url })
        assertNull(config.seen)
    }

    @Test
    fun `response - a method outside retryableMethods never reaches the predicate`() {
        val config = RecordingConfig()

        assertFalse(
            config.shouldReplayResponse(HttpStatusCode.InternalServerError, HttpMethod.Post, 1) { url }
        )
        assertNull(config.seen)
    }

    @Test
    fun `response - the predicate can veto a failure the method check admitted`() {
        val config = RecordingConfig(verdict = false)

        assertFalse(
            config.shouldReplayResponse(HttpStatusCode.InternalServerError, HttpMethod.Get, 1) { url }
        )
        assertEquals(HttpStatusCode.InternalServerError, config.seen!!.status)
    }

    @Test
    fun `failure - an IOException on an admitted method replays and carries the cause`() {
        val config = RecordingConfig()
        val cause = IOException("airplane mode")

        assertTrue(config.shouldReplayFailure(cause, HttpMethod.Get, 1) { url })
        val seen = config.seen!!
        assertEquals(cause, seen.cause)
        assertNull(seen.status)
    }

    @Test
    fun `failure - a non-IO throwable never reaches the predicate`() {
        val config = RecordingConfig()

        assertFalse(config.shouldReplayFailure(IllegalStateException(), HttpMethod.Get, 1) { url })
        assertNull(config.seen)
    }

    @Test
    fun `failure - the predicate can veto, which is how an app fails fast offline`() {
        val config = RecordingConfig(verdict = false)

        assertFalse(config.shouldReplayFailure(IOException(), HttpMethod.Get, 1) { url })
    }
}

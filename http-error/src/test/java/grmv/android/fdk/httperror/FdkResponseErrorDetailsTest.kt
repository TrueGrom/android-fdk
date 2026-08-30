package grmv.android.fdk.httperror

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FdkResponseErrorDetailsTest {

    private data class AppDetails(val reason: String) : FdkResponseErrorDetails

    // --- ResponseError.details ---

    @Test
    fun `ResponseError - details omitted - defaults to null`() {
        val error = HttpError.ResponseError(code = 403)

        assertNull(error.details)
        assertNull(error.message)
    }

    @Test
    fun `ResponseError - positional construction - order is code cause message details`() {
        val cause = IllegalStateException("boom")
        val details = FdkCodedError("EXPIRED")

        val error = HttpError.ResponseError(403, cause, "denied", details)

        assertEquals(403, error.code)
        assertEquals(cause, error.cause)
        assertEquals("denied", error.message)
        assertEquals(details, error.details)
    }

    @Test
    fun `ResponseError - equality - distinguishes different details`() {
        val expired = responseError(FdkCodedError("EXPIRED"))

        assertNotEquals(expired, responseError(FdkCodedError("ACCESS_DENIED")))
        assertEquals(expired, responseError(FdkCodedError("EXPIRED")))
    }

    @Test
    fun `ResponseError - java serialization - preserves code message and details`() {
        val error = HttpError.ResponseError(
            code = 403,
            message = "denied",
            details = FdkCodedError("EXPIRED", detail = "link is older than the TTL")
        )

        val back = roundTrip(error)

        assertEquals(403, back.code)
        assertEquals("denied", back.message)
        val details = FdkCodedError("EXPIRED", "link is older than the TTL")
        assertEquals(details, back.detailsAs<FdkCodedError>())
    }

    @Test
    fun `ready-made shapes - java serialization - survive a round trip`() {
        val shapes = listOf(
            FdkCodedError("EXPIRED", detail = "gone"),
            FdkProblemDetails(type = "about:blank", title = "Forbidden", status = 403),
            FdkRawErrorBody(body = """{"error":"nope"}""", contentType = "application/json")
        )

        assertEquals(shapes, shapes.map { roundTrip(it) })
    }

    // --- detailsAs ---

    @Test
    fun `detailsAs - matching type - returns the details`() {
        val error: HttpError = responseError(FdkCodedError("EXPIRED"))

        assertEquals("EXPIRED", error.detailsAs<FdkCodedError>()?.code)
    }

    @Test
    fun `detailsAs - marker type - returns details of any shape`() {
        val error: HttpError = responseError(AppDetails("email"))

        assertEquals(AppDetails("email"), error.detailsAs<FdkResponseErrorDetails>())
    }

    @Test
    fun `detailsAs - other type - returns null`() {
        val error: HttpError = responseError(AppDetails("email"))

        assertNull(error.detailsAs<FdkCodedError>())
    }

    @Test
    fun `detailsAs - details absent - returns null`() {
        val error: HttpError = HttpError.ResponseError(code = 500)

        assertNull(error.detailsAs<FdkRawErrorBody>())
    }

    @Test
    fun `detailsAs - non-ResponseError subtype - returns null`() {
        val error: HttpError = HttpError.NetworkError(cause = RuntimeException("timeout"))

        assertNull(error.detailsAs<FdkCodedError>())
    }

    // --- ready-made shapes ---

    @Test
    fun `FdkCodedError - positional construction - member order is code detail`() {
        val coded = FdkCodedError("ACCESS_DENIED", "not your marathon")

        assertEquals("ACCESS_DENIED", coded.code)
        assertEquals("not your marathon", coded.detail)
        assertNull(FdkCodedError("ACCESS_DENIED").detail)
    }

    @Test
    fun `FdkProblemDetails - positional construction - order follows the RFC members`() {
        val problem =
            FdkProblemDetails("about:blank", "Forbidden", 403, "link expired", "/req-8172")

        assertEquals("about:blank", problem.type)
        assertEquals("Forbidden", problem.title)
        assertEquals(403, problem.status)
        assertEquals("link expired", problem.detail)
        assertEquals("/req-8172", problem.instance)
    }

    @Test
    fun `FdkProblemDetails - only type supplied - other members default to null`() {
        val problem = FdkProblemDetails(type = "https://api.example.com/probs/token-expired")

        assertNull(problem.title)
        assertNull(problem.status)
        assertNull(problem.detail)
        assertNull(problem.instance)
    }

    @Test
    fun `FdkRawErrorBody - toString - reports length and media type instead of the body`() {
        val raw = FdkRawErrorBody(body = """{"token":"secret"}""", contentType = "application/json")

        assertEquals(
            "FdkRawErrorBody(body=<18 chars>, contentType=application/json)",
            raw.toString()
        )
        assertEquals(
            "FdkRawErrorBody(body=<2 chars>, contentType=null)",
            FdkRawErrorBody("{}").toString()
        )
    }

    @Test
    fun `FdkRawErrorBody - carried by ResponseError - body stays out of the error toString`() {
        val error = responseError(FdkRawErrorBody(body = """{"token":"secret"}"""))

        assertFalse(error.toString().contains("secret"))
    }

    // --- sealed hierarchy ---

    /**
     * Guards the `sealed` modifier on [HttpError]: relax it to `abstract` and the `when` below
     * stops being exhaustive, degrading from an expression to a statement — `described` then
     * returns `kotlin.Unit` and these assertions fail. The assertions are the mechanism, not
     * decoration.
     */
    @Test
    fun `HttpError - when over every subtype - yields a value proving the hierarchy is sealed`() {
        val described = { error: HttpError ->
            when (error) {
                is HttpError.ResponseError -> "response"
                is HttpError.NetworkError -> "network"
                is HttpError.ContentError -> "content"
                is HttpError.UnknownError -> "unknown"
            }
        }

        val errors = listOf(
            HttpError.ResponseError(code = 500),
            HttpError.NetworkError(),
            HttpError.ContentError(),
            HttpError.UnknownError()
        )

        assertEquals(listOf("response", "network", "content", "unknown"), errors.map(described))
    }

    private fun responseError(details: FdkResponseErrorDetails) =
        HttpError.ResponseError(code = 403, details = details)

    private fun <T : Serializable> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().also { out ->
            ObjectOutputStream(out).use { it.writeObject(value) }
        }.toByteArray()

        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as T
    }
}

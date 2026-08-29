package grmv.android.fdk.httperror

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpErrorTest {

    @Test
    fun `every subtype - cause supplied - message defaults to the cause message`() {
        val cause = IllegalStateException("boom")
        val errors: List<HttpError> = listOf(
            HttpError.ResponseError(code = 500, cause = cause),
            HttpError.NetworkError(cause = cause),
            HttpError.UnknownError(cause = cause),
            HttpError.ContentError(cause = cause),
        )

        assertEquals(List(errors.size) { "boom" }, errors.map { it.message })
    }
}

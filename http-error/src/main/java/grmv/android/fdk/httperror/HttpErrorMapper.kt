package grmv.android.fdk.httperror

/**
 * Converts a raw [Throwable] into a typed [HttpError].
 *
 * Implement and inject this interface to adapt HTTP-client-specific exceptions
 * (e.g. Retrofit `HttpException`, Ktor `ResponseException`) to the SDK error hierarchy.
 */
fun interface HttpErrorMapper {
    /**
     * Maps [e] to an [HttpError] subtype appropriate for the failure.
     *
     * Must not rethrow [e]; return [HttpError.UnknownError] for unrecognized exceptions.
     *
     * @param e The exception thrown by the underlying HTTP client.
     * @return A typed [HttpError] representing the failure.
     */
    suspend fun map(e: Throwable): HttpError
}

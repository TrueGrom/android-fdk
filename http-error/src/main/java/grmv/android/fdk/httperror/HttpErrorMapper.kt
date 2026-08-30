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
     * This is the only place in the stack that has the error response body available.
     * When a status alone does not identify the failure, parse the body here and attach
     * the result as [HttpError.ResponseError.details] — see [FdkResponseErrorDetails] for the
     * ready-made shapes — instead of hiding it in the [Throwable.cause] chain.
     *
     * @param e The exception thrown by the underlying HTTP client.
     * @return A typed [HttpError] representing the failure.
     */
    suspend fun map(e: Throwable): HttpError
}

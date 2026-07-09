package grmv.android.fdk.httperror

/**
 * Sealed hierarchy of errors produced by API calls.
 *
 * Catch [HttpError] to handle all SDK-level failures uniformly, or branch on
 * specific subtypes for targeted error handling.
 */
abstract class HttpError : Throwable() {
    abstract override val cause: Throwable?
    abstract override val message: String?

    /**
     * The server returned an HTTP response with a non-success status code (typically 4xx or 5xx).
     *
     * Branch on [code] to distinguish the specific failure.
     *
     * @property code The HTTP status code returned by the server.
     */
    data class ResponseError(
        val code: Int,
        override val cause: Throwable? = null,
        override val message: String? = cause?.message
    ) : HttpError()

    /**
     * A connectivity or socket-level failure — no HTTP response was received.
     *
     * Occurs when the request never reaches the server or no reply comes back: no
     * network, DNS resolution failure, connection refused, or a read/write timeout.
     * Such failures are usually transient, so this is the subtype to branch on when
     * offering the user a retry.
     */
    data class NetworkError(
        override val cause: Throwable? = null,
        override val message: String? = cause?.message
    ) : HttpError()

    /**
     * An error that does not fit any other category.
     *
     * Returned by an [HttpErrorMapper] for an exception it does not recognize. Treat
     * this as a non-retryable, unexpected failure and inspect [cause] when diagnosing.
     */
    data class UnknownError(
        override val cause: Throwable? = null,
        override val message: String? = cause?.message
    ) : HttpError()

    /**
     * A failure to parse or decode the response body.
     *
     * The transport succeeded and the server replied, but the payload could not be
     * deserialized — e.g. malformed JSON or a schema mismatch between the response and
     * the expected model. Retrying rarely helps; this usually signals a client/server
     * contract bug. The underlying parse exception is available via [cause].
     */
    data class ContentError(
        override val cause: Throwable? = null,
        override val message: String? = cause?.message
    ) : HttpError()
}
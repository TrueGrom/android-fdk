package grmv.android.fdk.httperror

import java.io.Serializable

/**
 * App-defined contents of an error response body, carried by [HttpError.ResponseError.details].
 *
 * The SDK attaches no meaning to implementations: it never inspects them, never branches on
 * them, and never parses a response body itself — producing a value is the job of the app's
 * [HttpErrorMapper], which is the only place in the stack that has the body parsed.
 *
 * Implement this in the app when the backend speaks a shape of its own, or use one of the
 * ready-made implementations for the common ones: [FdkCodedError] (application error code),
 * [FdkProblemDetails] (RFC 9457 `application/problem+json`), and [FdkRawErrorBody] (the body
 * verbatim). To extend a ready-made shape, compose rather than reimplement it — e.g.
 * `data class MyProblem(val standard: FdkProblemDetails, val balance: Int)` implementing
 * this interface.
 *
 * Implementations MUST be immutable value types: [HttpError.ResponseError] is a data class, so
 * the value participates in its `equals`/`hashCode` and its `toString`. [Serializable] is
 * required because [Throwable] is — an implementation holding a non-serializable field breaks
 * any consumer that puts the error into a `Bundle` or a Java-serialization path.
 */
interface FdkResponseErrorDetails : Serializable

/**
 * Returns [HttpError.ResponseError.details] when this error carries details of type [T], or
 * `null` otherwise — including for every non-[HttpError.ResponseError] subtype.
 *
 * ```kotlin
 * val HttpError.serverErrorCode: String?
 *     get() = detailsAs<FdkCodedError>()?.code
 * ```
 *
 * [T] is matched by its erasure, so a generic implementation type must not be used as [T] —
 * `detailsAs<Envelope<String>>()` would also match an `Envelope<Int>`.
 *
 * @return The details typed as [T], or `null` when absent or of another type.
 */
inline fun <reified T : FdkResponseErrorDetails> HttpError.detailsAs(): T? =
    (this as? HttpError.ResponseError)?.details as? T

/**
 * The symbolic or numeric error code an API returns alongside an HTTP status.
 *
 * Use when one status covers several situations that the app must word differently — e.g. a
 * `403` that means "recovery link expired", "link already spent", or a plain refusal. Branch
 * on [code]; it is the stable machine-readable part.
 *
 * @property code The code exactly as the server sent it — no normalization is applied. A
 *   numeric code is carried as its string form; convert it in the mapper.
 * @property detail Optional human-readable text accompanying the code.
 */
data class FdkCodedError(
    val code: String,
    val detail: String? = null
) : FdkResponseErrorDetails

/**
 * The standard members of an RFC 9457 (formerly RFC 7807) `application/problem+json` body.
 *
 * Emitted out of the box by Spring Boot, ASP.NET Core, Quarkus and many API gateways. Branch
 * on [type] — a URI identifying the *class* of problem, and the stable machine-readable
 * member; [title] and [detail] are human-readable text.
 *
 * Extension members are deliberately not modelled: their value types are arbitrary, so an app
 * that needs them wraps this type in one of its own (see [FdkResponseErrorDetails]).
 *
 * @property type URI identifying the problem class, or `null` when the mapper did not set one.
 *   RFC 9457 treats an absent `type` as `about:blank` — normalize in the mapper if the app
 *   relies on that. Compare it as a string: it need not resolve, and must not be fetched.
 * @property title Short, human-readable summary of the problem class.
 * @property status HTTP status quoted by the body. Diagnostic only — the authority is
 *   [HttpError.ResponseError.code], which may legitimately differ.
 * @property detail Human-readable explanation specific to this occurrence.
 * @property instance URI identifying this specific occurrence of the problem.
 */
data class FdkProblemDetails(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null
) : FdkResponseErrorDetails

/**
 * An error response body kept verbatim, for backends whose shape the app does not model.
 *
 * The fallback that always applies: it preserves what the server said without committing the
 * app to a schema, and keeps the value out of the [Throwable.cause] chain. [toString] reports
 * only the body's length and media type, so an error carrying credentials or personal data in
 * its body does not leak them into logs and crash reports.
 *
 * @property body The raw body as received.
 * @property contentType The body's media type, when the response declared one.
 */
data class FdkRawErrorBody(
    val body: String,
    val contentType: String? = null
) : FdkResponseErrorDetails {
    override fun toString(): String =
        "FdkRawErrorBody(body=<${body.length} chars>, contentType=$contentType)"
}

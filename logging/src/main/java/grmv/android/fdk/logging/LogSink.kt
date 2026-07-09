package grmv.android.fdk.logging

/**
 * Backend contract for SDK log output.
 *
 * Implement this interface to redirect SDK log messages to your own logging infrastructure
 * (Logcat, Crashlytics, a file, etc.), then install the implementation via [FdkLog.install].
 *
 * Every method accepts a printf-style [format string][java.util.Formatter] in `message` plus
 * positional `args` that are interpolated only if the message is actually emitted. Implementations
 * should defer formatting until they know the entry will be logged, so unused log calls stay cheap.
 */
interface LogSink {
    /**
     * Emits a verbose-level log message.
     *
     * Use for the most fine-grained, high-volume tracing. Typically stripped from release builds.
     */
    fun v(message: String, vararg args: Any?)

    /**
     * Emits a debug-level log message.
     *
     * Use for diagnostic detail useful during development. Typically disabled in release builds.
     */
    fun d(message: String, vararg args: Any?)

    /**
     * Emits an info-level log message.
     *
     * Use for noteworthy, expected lifecycle events worth keeping in release logs.
     */
    fun i(message: String, vararg args: Any?)

    /**
     * Emits a warning-level log message.
     *
     * Use for recoverable problems or unexpected-but-handled conditions that may warrant attention.
     */
    fun w(message: String, vararg args: Any?)

    /**
     * Emits an error-level log message, optionally with an associated exception.
     *
     * Use for failures the SDK could not recover from. Pass the [throwable] so backends can capture
     * a stack trace (and, for crash reporters, the cause chain).
     *
     * @param throwable Exception to include in the log entry; `null` to log without a stack trace.
     */
    fun e(throwable: Throwable? = null, message: String = "", vararg args: Any?)

    /**
     * Returns a [LogSink] that associates [tag] with every message written through it.
     *
     * Useful for attributing log lines to a specific class or subsystem. The default implementation
     * wraps this sink and prefixes each message with `[tag]`; backends may override to use a native
     * tagging mechanism instead.
     *
     * @param tag Short label identifying the source of the log lines.
     * @return A [LogSink] that routes to this sink while applying [tag].
     */
    fun tagged(tag: String): LogSink = TaggedFallbackSink(tag, this)
}

internal class TaggedFallbackSink(private val tag: String, private val sink: LogSink) : LogSink {
    override fun v(message: String, vararg args: Any?) = sink.v("[$tag] $message", *args)
    override fun d(message: String, vararg args: Any?) = sink.d("[$tag] $message", *args)
    override fun i(message: String, vararg args: Any?) = sink.i("[$tag] $message", *args)
    override fun w(message: String, vararg args: Any?) = sink.w("[$tag] $message", *args)
    override fun e(throwable: Throwable?, message: String, vararg args: Any?) = sink.e(throwable, "[$tag] $message", *args)
    override fun tagged(tag: String): LogSink = TaggedFallbackSink(tag, sink)
}

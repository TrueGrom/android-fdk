package grmv.android.fdk.logging

/**
 * SDK-wide logging entry point.
 *
 * All SDK modules route their log output through this singleton. By default, it is a no-op;
 * call [install] once at app startup to activate logging. Call [uninstall] to revert to no-op.
 */
object FdkLog : LogSink {

    @Volatile
    private var delegate: LogSink = NoOpLogger

    /**
     * Activates [backend] as the active log sink for all SDK modules.
     *
     * Replaces any previously installed backend. Safe to call from any thread.
     *
     * @param backend [LogSink] implementation to receive all subsequent log calls.
     */
    fun install(backend: LogSink) {
        delegate = backend
    }

    /**
     * Reverts to the no-op sink, suppressing all further SDK log output.
     *
     * Safe to call from any thread.
     */
    fun uninstall() {
        delegate = NoOpLogger
    }

    override fun tagged(tag: String): LogSink = delegate.tagged(tag)

    override fun v(message: String, vararg args: Any?) = delegate.v(message, *args)
    override fun d(message: String, vararg args: Any?) = delegate.d(message, *args)
    override fun i(message: String, vararg args: Any?) = delegate.i(message, *args)
    override fun w(message: String, vararg args: Any?) = delegate.w(message, *args)
    override fun e(throwable: Throwable?, message: String, vararg args: Any?) = delegate.e(throwable, message, *args)
}

/**
 * Creates a [LogSink] tagged with the simple name of [T].
 *
 * Intended for use inside a class to obtain a per-class logger:
 * ```kotlin
 * private val log = logger<MyClass>()
 * ```
 *
 * @return A [LogSink] that prefixes every message with the class simple name.
 */
inline fun <reified T> T.logger(): LogSink = FdkLog.tagged(T::class.simpleName ?: "Unknown")

/**
 * Creates a [LogSink] tagged with the runtime class name of the receiver.
 *
 * Use in base classes where the compile-time type would always resolve to the base
 * rather than the concrete subclass:
 * ```kotlin
 * protected val logger: LogSink by lazy { loggerForClass() }
 * ```
 */
fun Any.loggerForClass(): LogSink = FdkLog.tagged(this::class.simpleName ?: "Unknown")

private object NoOpLogger : LogSink {
    override fun v(message: String, vararg args: Any?) = Unit
    override fun d(message: String, vararg args: Any?) = Unit
    override fun i(message: String, vararg args: Any?) = Unit
    override fun w(message: String, vararg args: Any?) = Unit
    override fun e(throwable: Throwable?, message: String, vararg args: Any?) = Unit
}

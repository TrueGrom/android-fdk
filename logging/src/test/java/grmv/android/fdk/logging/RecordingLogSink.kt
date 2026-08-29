package grmv.android.fdk.logging

/** Shared test double capturing every [LogSink] call in order. */
internal class RecordingLogSink : LogSink {
    enum class Level { V, D, I, W, E }

    data class Call(val level: Level, val throwable: Throwable?, val message: String, val args: List<Any?>)

    private val _calls = mutableListOf<Call>()

    /** Calls recorded so far, oldest first. */
    val calls: List<Call> get() = _calls.toList()

    override fun v(message: String, vararg args: Any?) {
        _calls += Call(Level.V, null, message, args.toList())
    }

    override fun d(message: String, vararg args: Any?) {
        _calls += Call(Level.D, null, message, args.toList())
    }

    override fun i(message: String, vararg args: Any?) {
        _calls += Call(Level.I, null, message, args.toList())
    }

    override fun w(message: String, vararg args: Any?) {
        _calls += Call(Level.W, null, message, args.toList())
    }

    override fun e(throwable: Throwable?, message: String, vararg args: Any?) {
        _calls += Call(Level.E, throwable, message, args.toList())
    }
}

package grmv.android.fdk.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FdkLogTest {

    private open class Base
    private class Derived : Base()

    @After
    fun tearDown() {
        // FdkLog is a process-wide singleton; never let one test's installed sink leak into another.
        FdkLog.uninstall()
    }

    // --- install / uninstall ---

    @Test
    fun `install - backend installed - subsequent calls are delegated to it`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)

        FdkLog.i("hello %s", "world")

        val call = sink.calls.single()
        assertEquals(RecordingLogSink.Level.I, call.level)
        assertEquals("hello %s", call.message)
        assertEquals(listOf("world"), call.args)
    }

    @Test
    fun `install - error with throwable - forwards the throwable to the backend`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)
        val error = IllegalStateException("failure")

        FdkLog.e(error, "context")

        assertSame(error, sink.calls.single().throwable)
    }

    @Test
    fun `uninstall - after a backend was installed - reverts to no-op`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)
        FdkLog.uninstall()

        FdkLog.w("should be dropped")

        assertEquals(0, sink.calls.size)
    }

    // --- tagged ---

    @Test
    fun `FdkLog tagged - delegate installed - routes through the delegate's tagged sink`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)

        val tagged = FdkLog.tagged("MyTag")
        tagged.i("payload")

        // LogSink's default tagged() wraps with TaggedFallbackSink, prefixing the message.
        assertEquals("[MyTag] payload", sink.calls.single().message)
    }

    @Test
    fun `FdkLog tagged - obtained before install - still reaches the backend installed later`() {
        // A `by lazy { loggerForClass() }` field on a long-lived object can bind before the app
        // calls install(); the tagged sink must resolve the backend per call, not capture it once.
        val tagged = FdkLog.tagged("Early")
        val sink = RecordingLogSink()
        FdkLog.install(sink)

        tagged.i("payload")

        assertEquals("[Early] payload", sink.calls.single().message)
    }

    @Test
    fun `FdkLog tagged - backend uninstalled after binding - stops reaching the old backend`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)
        val tagged = FdkLog.tagged("Early")
        tagged.i("first")

        FdkLog.uninstall()
        tagged.i("second")

        assertEquals(1, sink.calls.size)
    }

    // --- logger<T>() (reified compile-time type) vs loggerForClass() (runtime type) ---

    @Test
    fun `loggerForClass vs logger - same instance different tag source - tags differ`() {
        val sink = RecordingLogSink()
        FdkLog.install(sink)
        val base: Base = Derived()

        base.logger<Base>().v("a")
        base.loggerForClass().v("b")

        // logger<T>() tags with the compile-time type, loggerForClass() with the runtime one.
        assertEquals("[Base] a", sink.calls[0].message)
        assertEquals("[Derived] b", sink.calls[1].message)
    }
}

package grmv.android.fdk.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LogSinkTest {

    // --- LogSink.tagged() default implementation ---

    @Test
    fun `tagged - v d i w - prefixes the tag on every level`() {
        val recorder = RecordingLogSink()
        val tagged = recorder.tagged("Net")

        tagged.v("verbose")
        tagged.d("debug")
        tagged.i("info")
        tagged.w("warn")

        assertEquals(
            listOf("[Net] verbose", "[Net] debug", "[Net] info", "[Net] warn"),
            recorder.calls.map { it.message }
        )
    }

    @Test
    fun `tagged - e with throwable - prefixes message and forwards the throwable unchanged`() {
        val recorder = RecordingLogSink()
        val tagged = recorder.tagged("Net")
        val error = RuntimeException("failure")

        tagged.e(error, "request failed")

        val call = recorder.calls.single()
        assertEquals("[Net] request failed", call.message)
        assertSame(error, call.throwable)
    }

    @Test
    fun `tagged - vararg args - forwarded to the underlying sink unchanged`() {
        val recorder = RecordingLogSink()
        val tagged = recorder.tagged("Net")

        tagged.i("count=%d name=%s", 5, "abc")

        assertEquals(listOf(5, "abc"), recorder.calls.single().args)
    }

    @Test
    fun `tagged called again on a tagged sink - does not nest the original tag, retagging the root sink`() {
        val recorder = RecordingLogSink()
        val once = recorder.tagged("Outer")

        val twice = once.tagged("Inner")
        twice.d("payload")

        // TaggedFallbackSink.tagged() rewraps the *original* sink with the new tag, so "Outer"
        // is dropped rather than accumulating into "[Outer][Inner]".
        assertEquals("[Inner] payload", recorder.calls.single().message)
    }
}

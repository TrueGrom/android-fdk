package grmv.android.fdk.screen.snackbar

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarBuilderTest {

    private val context = mockk<Context>()

    private fun build(block: SnackbarBuilder.() -> Unit): SnackbarMessage =
        SnackbarBuilderImpl(context).apply(block).build()

    @Test
    fun `message text - emits with defaults`() {
        val msg = build { message("Hello") }

        assertEquals("Hello", msg.message)
        assertNull(msg.actionLabel)
        assertFalse(msg.withDismissAction)
        assertEquals(SnackbarDuration.Short, msg.duration)
        assertNull(msg.onAction)
        assertNull(msg.onDismiss)
    }

    @Test
    fun `full DSL - sets every field and preserves callbacks`() {
        var actioned = false
        var dismissed = false
        val msg = build {
            message("Saved")
            actionLabel("Undo")
            duration(SnackbarDuration.Long)
            withDismissAction()
            onAction { actioned = true }
            onDismiss { dismissed = true }
        }

        assertEquals("Saved", msg.message)
        assertEquals("Undo", msg.actionLabel)
        assertEquals(SnackbarDuration.Long, msg.duration)
        assertTrue(msg.withDismissAction)
        msg.onAction!!.invoke()
        msg.onDismiss!!.invoke()
        assertTrue(actioned)
        assertTrue(dismissed)
    }

    @Test
    fun `resId overloads - resolve through context`() {
        every { context.getString(1) } returns "res-message"
        every { context.getString(2) } returns "res-action"

        val msg = build {
            message(1)
            actionLabel(2)
        }

        assertEquals("res-message", msg.message)
        assertEquals("res-action", msg.actionLabel)
    }

    @Test
    fun `no message set - builds empty text so the manager skips it`() {
        val msg = build { actionLabel("Undo") }

        assertTrue(msg.message.isEmpty())
    }
}

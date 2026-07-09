package grmv.android.fdk.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionEmitterTest {

    private sealed class TestAction {
        data object First : TestAction()
        data object Second : TestAction()
        data class Payload(val id: Int) : TestAction()
    }

    // --- screenActions initial value ---

    @Test
    fun `screenActions - new manager - is null`() {
        val manager = MutableActionManager<TestAction>()
        assertNull(manager.screenActions.value)
    }

    // --- sendAction ---

    @Test
    fun `sendAction - first emission - exposes the event`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.First)
        assertEquals(TestAction.First, manager.screenActions.value)
    }

    @Test
    fun `sendAction - second emission - replaces previous unconsumed event`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.First)
        manager.sendAction(TestAction.Second)
        assertEquals(TestAction.Second, manager.screenActions.value)
    }

    @Test
    fun `sendAction - emits payload with data - preserves payload`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.Payload(7))
        assertEquals(TestAction.Payload(7), manager.screenActions.value)
    }

    // --- consumeAction ---

    @Test
    fun `consumeAction - matches current value - clears to null`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.First)
        manager.consumeAction(TestAction.First)
        assertNull(manager.screenActions.value)
    }

    @Test
    fun `consumeAction - differs from current value - leaves state unchanged`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.First)
        manager.consumeAction(TestAction.Second)
        assertEquals(TestAction.First, manager.screenActions.value)
    }

    @Test
    fun `consumeAction - matches by equality not identity - clears state`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.Payload(42))
        manager.consumeAction(TestAction.Payload(42))
        assertNull(manager.screenActions.value)
    }

    @Test
    fun `consumeAction - state already null - stays null`() {
        val manager = MutableActionManager<TestAction>()
        manager.consumeAction(TestAction.First)
        assertNull(manager.screenActions.value)
    }

    @Test
    fun `consumeAction - payload with different data - does not clear`() {
        val manager = MutableActionManager<TestAction>()
        manager.sendAction(TestAction.Payload(1))
        manager.consumeAction(TestAction.Payload(2))
        assertNotEquals(null, manager.screenActions.value)
        assertEquals(TestAction.Payload(1), manager.screenActions.value)
    }
}

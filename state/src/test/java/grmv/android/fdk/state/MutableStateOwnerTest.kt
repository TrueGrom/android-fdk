package grmv.android.fdk.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MutableStateOwnerTest {

    private data class S(val count: Int = 0, val name: String = "") : BaseState

    // --- initial state ---

    @Test
    fun `initial - factory invoked once - state holds initial value`() {
        var invocations = 0
        val owner = MutableStateOwner {
            invocations++
            S(count = 1, name = "init")
        }
        assertEquals(1, invocations)
        assertEquals(S(count = 1, name = "init"), owner.state.value)
    }

    // --- setState ---

    @Test
    fun `setState - replaces current value entirely`() {
        val owner = MutableStateOwner { S(count = 0) }
        owner.setState(S(count = 99, name = "set"))
        assertEquals(S(count = 99, name = "set"), owner.state.value)
    }

    // --- updateState ---

    @Test
    fun `updateState - returns the new state value`() {
        val owner = MutableStateOwner { S(count = 1) }
        val updated = owner.updateState { it.copy(count = it.count + 1) }
        assertEquals(S(count = 2), updated)
    }

    @Test
    fun `updateState - publishes new value to state flow`() {
        val owner = MutableStateOwner { S(count = 1) }
        owner.updateState { it.copy(count = 10) }
        assertEquals(S(count = 10), owner.state.value)
    }

    @Test
    fun `updateState - receives current state as input`() {
        val owner = MutableStateOwner { S(count = 5, name = "x") }
        var seen: S? = null
        owner.updateState { current ->
            seen = current
            current
        }
        assertEquals(S(count = 5, name = "x"), seen)
    }

    @Test
    fun `updateState - identical return value - state remains unchanged`() {
        val owner = MutableStateOwner { S(count = 3) }
        val before = owner.state.value
        owner.updateState { it }
        assertSame(before, owner.state.value)
    }

    @Test
    fun `updateState - sequential calls - each sees previous result`() {
        val owner = MutableStateOwner { S(count = 0) }
        owner.updateState { it.copy(count = it.count + 1) }
        owner.updateState { it.copy(count = it.count + 1) }
        owner.updateState { it.copy(count = it.count + 1) }
        assertEquals(3, owner.state.value.count)
    }

    // --- StateOwner exposure ---

    @Test
    fun `state - exposed as read-only StateOwner`() {
        val owner: StateOwner<S> = MutableStateOwner { S(count = 4) }
        assertEquals(4, owner.state.value.count)
    }
}

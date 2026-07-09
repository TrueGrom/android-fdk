package grmv.android.fdk.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateViewModelDelegateTest {

    private data class S(val count: Int = 0, val label: String = "") : BaseState

    private class SBuilder(override val initial: S) : BaseStateBuilder<S>() {
        fun count(value: Int) = accumulate { it.copy(count = value) }
        fun label(value: String) = accumulate { it.copy(label = value) }
        fun incrementCount() = accumulate { it.copy(count = it.count + 1) }
    }

    private class TestVM(
        owner: MutableStateOwner<S> = MutableStateOwner { S() },
    ) : StateViewModel<S, SBuilder>(owner, ::SBuilder)

    private class TestDelegate : StateViewModelDelegate<S, SBuilder>()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun delegate(vm: TestVM): TestDelegate =
        TestDelegate().also { it.initialize(vm) }

    // --- initialize ---

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `getActualState - before initialize - throws UninitializedPropertyAccessException`() {
        TestDelegate().getActualState()
    }

    @Test
    fun `initialize - second call replaces owner`() {
        val vm1 = TestVM(MutableStateOwner { S(count = 1) })
        val vm2 = TestVM(MutableStateOwner { S(count = 2) })
        val d = TestDelegate()
        d.initialize(vm1)
        d.initialize(vm2)
        assertEquals(S(count = 2), d.getActualState())
    }

    // --- getActualState ---

    @Test
    fun `getActualState - after initialize - returns owner current state`() {
        val vm = TestVM(MutableStateOwner { S(count = 7) })
        assertEquals(S(count = 7), delegate(vm).getActualState())
    }

    @Test
    fun `getActualState - reflects state after owner mutation`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val d = delegate(vm)
        vm.state { count(42) }
        assertEquals(S(count = 42), d.getActualState())
    }

    // --- state ---

    @Test
    fun `state - applies updater on owner builder - returns new state`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val result = delegate(vm).state { count(1) }
        assertEquals(S(count = 1), result)
    }

    @Test
    fun `state - publishes new value to owner state flow`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        delegate(vm).state { label("hello") }
        assertEquals("hello", vm.state.value.label)
    }

    @Test
    fun `state - sequential calls - each seeded from previous result`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val d = delegate(vm)
        d.state { incrementCount() }
        d.state { incrementCount() }
        d.state { incrementCount() }
        assertEquals(3, vm.state.value.count)
    }

    // --- task ---

    @Test
    fun `task - executes coroutine block via owner`() = runTest {
        val vm = TestVM()
        var ran = false
        delegate(vm).task { ran = true }
        assertTrue(ran)
    }

    // --- read-your-own-write & shared owner across delegates ---

    @Test
    fun `getActualState - after this delegate's state mutation - reflects new value`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val d = delegate(vm)
        d.state {
            count(11)
            label("via-delegate")
        }
        assertEquals(S(count = 11, label = "via-delegate"), d.getActualState())
    }

    @Test
    fun `two delegates on same owner - mutations from one are visible to the other`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val d1 = delegate(vm)
        val d2 = delegate(vm)
        d1.state { count(5) }
        assertEquals(S(count = 5), d2.getActualState())
        d2.state { accumulate { it.copy(count = it.count + 10, label = "via-d2") } }
        assertEquals(S(count = 15, label = "via-d2"), d1.getActualState())
    }
}

package grmv.android.fdk.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class StateViewModelTest {

    private data class S(val count: Int = 0, val label: String = "") : BaseState

    private class SBuilder(override val initial: S) : BaseStateBuilder<S>() {
        fun count(value: Int) = accumulate { it.copy(count = value) }
        fun label(value: String) = accumulate { it.copy(label = value) }
        fun incrementCount() = accumulate { it.copy(count = it.count + 1) }
    }

    private open class TestVM(
        owner: MutableStateOwner<S> = MutableStateOwner { S() },
        builderFactory: (S) -> SBuilder = ::SBuilder,
    ) : StateViewModel<S, SBuilder>(owner, builderFactory) {
        fun actualState(): S = getActualState()
    }

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    // --- state ---

    @Test
    fun `state - applies updater mutations - returns new state`() {
        val vm = TestVM(MutableStateOwner { S(count = 1, label = "a") })
        val result = vm.state { count(5); label("b") }
        assertEquals(S(count = 5, label = "b"), result)
    }

    @Test
    fun `state - publishes new value to state flow`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        vm.state { count(7) }
        assertEquals(7, vm.state.value.count)
    }

    @Test
    fun `state - no-op updater - returns state equal to initial`() {
        val initial = S(count = 3, label = "x")
        val vm = TestVM(MutableStateOwner { initial })
        val result = vm.state { }
        assertEquals(initial, result)
    }

    @Test
    fun `state - sequential calls - each seeded from previous result`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        vm.state { count(1) }
        vm.state { incrementCount() }
        vm.state { incrementCount() }
        assertEquals(3, vm.state.value.count)
    }

    @Test
    fun `state - builderFactory seeded with current snapshot on first call`() {
        val initial = S(count = 42, label = "init")
        val inputs = mutableListOf<S>()
        val vm = TestVM(
            owner = MutableStateOwner { initial },
            builderFactory = { snapshot -> inputs += snapshot; SBuilder(snapshot) },
        )
        vm.state { }
        assertEquals(listOf(initial), inputs)
    }

    @Test
    fun `state - builderFactory seeded with result of prior state call`() {
        val inputs = mutableListOf<S>()
        val vm = TestVM(
            owner = MutableStateOwner { S(count = 0) },
            builderFactory = { snapshot -> inputs += snapshot; SBuilder(snapshot) },
        )
        val first = vm.state { count(10) }
        vm.state { }
        assertEquals(first, inputs[1])
    }

    // --- getActualState ---

    @Test
    fun `getActualState - reflects latest mutation`() {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        vm.state { count(99) }
        assertEquals(S(count = 99), vm.actualState())
    }

    // --- delegatedTask ---

    @Test
    fun `delegatedTask - executes coroutine block`() = runTest {
        val vm = TestVM()
        var ran = false
        vm.delegatedTask { ran = true }
        assertTrue(ran)
    }

    // --- atomicity & external mutation ---

    @Test
    fun `state - single block with multiple mutations - emits exactly once to flow`() = runTest {
        val vm = TestVM(MutableStateOwner { S(count = 0) })
        val received = mutableListOf<S>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect { received += it }
        }
        val baseline = received.size
        vm.state {
            count(1)
            count(2)
            label("final")
        }
        assertEquals(baseline + 1, received.size)
        assertEquals(S(count = 2, label = "final"), received.last())
    }

    @Test
    fun `state - external setState then state block - builder seeded with external value`() {
        val owner = MutableStateOwner { S(count = 0, label = "") }
        val vm = TestVM(owner)
        owner.setState(S(count = 100, label = "ext"))
        val result = vm.state { incrementCount() }
        assertEquals(S(count = 101, label = "ext"), result)
    }
}

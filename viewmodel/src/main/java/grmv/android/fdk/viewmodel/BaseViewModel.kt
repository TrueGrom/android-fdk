package grmv.android.fdk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import grmv.android.fdk.coroutines.onError
import grmv.android.fdk.logging.LogSink
import grmv.android.fdk.logging.loggerForClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private data class TaskId(val key: Any)
private fun Any.taskId(): TaskId = TaskId(this)

/**
 * Carries a [TaskId] inside a coroutine's [CoroutineContext]; the id then lives within the [Job]
 * itself and is reclaimed with it — no external table, so no leak is possible.
 */
private class TaskIdElement(val taskId: TaskId) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TaskIdElement>
}

/**
 * The [TaskId] stored in this [Job]'s context, or null if absent.
 *
 * Relies on jobs returned by [launch]/[async] being [CoroutineScope]s (an `AbstractCoroutine`),
 * which is how kotlinx-coroutines exposes the running context — not part of the bare [Job] contract.
 */
private val Job.taskId: TaskId?
    get() = (this as? CoroutineScope)?.coroutineContext?.get(TaskIdElement)?.taskId

/**
 * Abstract base for all SDK ViewModels.
 *
 * Subclass it and launch background work through the `task`/`asyncTask` helpers instead of calling
 * [viewModelScope] directly: every coroutine started here is bound to [viewModelScope] and is therefore
 * canceled automatically when the ViewModel is cleared, so no manual teardown is required. For work that
 * should supersede an earlier in-flight run (typed-ahead search, pull-to-refresh, debounced saves), use the
 * keyed [uniqueTask]/[asyncUniqueTask] variants, which cancel the previous job sharing the same id.
 *
 * Also exposes a class-tagged [logger] and the [handledError] `Result` helper for terminal handling
 * of suspend results; wrap the chain in `try/finally` for cleanup that must run even on cancellation.
 */
abstract class BaseViewModel : ViewModel() {

    /** Logger tagged with the concrete subclass name; use it for all logging from within the ViewModel. */
    protected val logger: LogSink by lazy { loggerForClass() }

    private val jobs = ConcurrentHashMap<TaskId, Job>()

    /**
     * Launches fire-and-forget work in [viewModelScope] and returns its [Job].
     *
     * The job is canceled with the ViewModel and is not tracked by id, so overlapping calls run
     * concurrently. Use [uniqueTask] instead when a new run should cancel a previous one.
     */
    protected fun task(taskJob: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(block = taskJob)
    }

    /**
     * Launches work in [viewModelScope] and returns a [Deferred] the caller can `await`.
     *
     * The job is canceled with the ViewModel and is not tracked by id; awaiting a canceled job
     * throws [CancellationException]. Use [asyncUniqueTask] when a new run should cancel a previous one.
     */
    protected fun <T> asyncTask(taskJob: suspend CoroutineScope.() -> T): Deferred<T> {
        return viewModelScope.async(block = taskJob)
    }

    /**
     * Launches a keyed async coroutine in [viewModelScope], cancelling any previously running job
     * registered under the same [id] before starting the new one.
     *
     * @param id Arbitrary key that identifies this task slot; the same key cancels the previous run.
     * @return [Deferred] that resolves to the result of [task].
     */
    protected fun <T> asyncUniqueTask(id: Any, task: suspend CoroutineScope.() -> T): Deferred<T> {
        return managedUniqueJob(id) { ctx -> viewModelScope.async(ctx, block = task) }
    }

    /**
     * Launches a keyed coroutine in [viewModelScope], cancelling any previously running job
     * registered under the same [id] before starting the new one.
     *
     * @param id Arbitrary key that identifies this task slot; the same key cancels the previous run.
     * @return The launched [Job].
     */
    protected fun uniqueTask(id: Any, task: suspend CoroutineScope.() -> Unit): Job {
        return managedUniqueJob(id) { ctx -> viewModelScope.launch(ctx, block = task) }
    }

    private inline fun <reified J : Job> managedUniqueJob(
        id: Any,
        crossinline jobFactory: (CoroutineContext) -> J,
    ): J {
        val taskId = id.taskId()
        val newJob = jobs.compute(taskId) { _, oldJob ->
            oldJob?.cancel()
            jobFactory(TaskIdElement(taskId))
        }!!

        newJob.invokeOnCompletion {
            val completedId = newJob.taskId ?: return@invokeOnCompletion
            jobs.compute(completedId) { _, currentJob ->
                if (currentJob == newJob) null else currentJob
            }
        }
        return newJob as J
    }

    /**
     * Logs the failure via [logger] and invokes [block] when this [Result] holds an error; a no-op on success.
     *
     * @param block Side effect run with the captured [Throwable], after it has been logged.
     * @return The original [Result] unchanged, so further chaining is possible.
     */
    protected infix fun <T> Result<T>.handledError(block: (Throwable) -> Unit): Result<T> {
        return onError { e ->
            e.log()
            block(e)
        }
    }

    /** Logs this throwable at error level through [logger], with an optional context [message]. */
    protected fun Throwable.log(message: String = "") {
        logger.e(this, message)
    }
}

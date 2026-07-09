package grmv.android.fdk.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


/**
 * Holder for coroutine dispatchers used across repository operations.
 *
 * Pass a custom [io] dispatcher in tests to replace [Dispatchers.IO] with a
 * test-controlled dispatcher (e.g. `StandardTestDispatcher`).
 *
 * @param io Dispatcher for blocking I/O work. Defaults to [Dispatchers.IO].
 */
class BaseDispatchers(
    internal val io: CoroutineDispatcher = Dispatchers.IO
)
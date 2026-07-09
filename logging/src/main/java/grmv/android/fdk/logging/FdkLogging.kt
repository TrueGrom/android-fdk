package grmv.android.fdk.logging

import timber.log.Timber

/**
 * Convenience helpers for configuring SDK logging during app setup.
 *
 * These wire the SDK to a [Timber](https://github.com/JakeWharton/timber) backend so consumers
 * can get working log output without implementing a custom [LogSink].
 */
object FdkLogging {

    /**
     * Enables human-readable debug logging and installs it as the active SDK log backend.
     *
     * Plants a Timber [debug tree][Timber.DebugTree] (which writes to Logcat) and routes all
     * [FdkLog] output to it. Intended for debug builds only; for release builds either leave SDK
     * logging at its no-op default or [install][FdkLog.install] a custom [LogSink].
     *
     * Call once in `Application.onCreate` before any SDK usage. Calling it again plants an
     * additional debug tree, which results in duplicate log lines, so guard against repeat calls.
     */
    fun setupDebug() {
        Timber.plant(Timber.DebugTree())
        FdkLog.installTimber()
    }

}

package grmv.android.fdk.logging

import timber.log.Timber

internal class TimberLogger : LogSink {
    override fun v(message: String, vararg args: Any?) = Timber.v(message, *args)
    override fun d(message: String, vararg args: Any?) = Timber.d(message, *args)
    override fun i(message: String, vararg args: Any?) = Timber.i(message, *args)
    override fun w(message: String, vararg args: Any?) = Timber.w(message, *args)
    override fun e(throwable: Throwable?, message: String, vararg args: Any?) = Timber.e(throwable, message, *args)
    override fun tagged(tag: String): LogSink = TimberTaggedLogger(tag)
}

private class TimberTaggedLogger(private val tag: String) : LogSink {
    override fun v(message: String, vararg args: Any?) = Timber.tag(tag).v(message, *args)
    override fun d(message: String, vararg args: Any?) = Timber.tag(tag).d(message, *args)
    override fun i(message: String, vararg args: Any?) = Timber.tag(tag).i(message, *args)
    override fun w(message: String, vararg args: Any?) = Timber.tag(tag).w(message, *args)
    override fun e(throwable: Throwable?, message: String, vararg args: Any?) = Timber.tag(tag).e(throwable, message, *args)
    override fun tagged(tag: String): LogSink = TimberTaggedLogger(tag)
}

internal fun FdkLog.installTimber() = install(TimberLogger())

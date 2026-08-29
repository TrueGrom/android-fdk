package grmv.android.fdk.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class FdkLoggingTest {

    @Before
    fun setUp() {
        Timber.uprootAll()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        FdkLog.uninstall()
    }

    // --- setupDebug ---

    @Test
    fun `setupDebug - plants a DebugTree and routes FdkLog through Timber`() {
        assertTrue(Timber.forest().isEmpty())

        FdkLogging.setupDebug()

        assertEquals(1, Timber.forest().size)
        assertTrue(Timber.forest().single() is Timber.DebugTree)
    }
}

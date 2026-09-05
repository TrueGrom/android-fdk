package grmv.android.fdk.network

import org.junit.Assert.assertTrue
import org.junit.Test

class HttpJsonConfigTest {

    @Test
    fun `defaults - coercion is on, so an unmodelled value costs its own property`() {
        assertTrue(DefaultHttpJsonConfig.coerceInputValues)
    }

    @Test
    fun `coerceInputValues - a config declaring only the original four still gets it`() {
        val legacy = object : HttpJsonConfig {
            override val prettyPrint: Boolean = false
            override val isLenient: Boolean = false
            override val ignoreUnknownKeys: Boolean = true
            override val explicitNulls: Boolean = false
        }

        assertTrue(legacy.coerceInputValues)
    }
}

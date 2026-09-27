package com.nexvault.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test: the target context's package must match the built application id.
 *
 * Roadmap 2.0.6: the template asserted the literal `"com.nexvault.wallet"`, which fails for the
 * debug variant (`com.nexvault.wallet.debug`) and had been turning every connected run red as
 * legacy CR finding 1.1-3. Asserting against [BuildConfig.APPLICATION_ID] keeps the check honest
 * for every variant.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}

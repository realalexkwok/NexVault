package com.nexvault.wallet.core.datastore.state

import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The in-memory authentication session (roadmap 2.0.2b): the flag that gates wallet material
 * retrieval. Kept in its own class so [AppStateManagerTest] stays within the detekt
 * function-count threshold.
 */
class AppStateManagerSessionTest {

    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var securityPreferences: SecurityPreferencesDataStore
    private lateinit var appStateManager: AppStateManager

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        securityPreferences = mockk(relaxed = true)

        every { userPreferences.autoLockTimeout } returns flowOf(AutoLockTimeout.FIVE_MINUTES)
        every { securityPreferences.isWalletSetUp } returns flowOf(true)
        every { securityPreferences.lastAuthTimestamp } returns flowOf(System.currentTimeMillis())
        every { securityPreferences.failedAttemptCount } returns flowOf(0)
        every { securityPreferences.lockoutEndTime } returns flowOf(0L)

        appStateManager = AppStateManager(
            userPreferences = userPreferences,
            securityPreferences = securityPreferences,
            walletMetadata = mockk(relaxed = true),
            walletStore = mockk(relaxed = true),
            keyStoreManager = mockk(relaxed = true)
        )
    }

    @Test
    fun sessionStartsLockedAndAuthenticationSuccessUnlocksIt() = runTest {
        assertFalse(appStateManager.isUnlocked.value)

        appStateManager.onAuthenticationSuccess()

        assertTrue(appStateManager.isUnlocked.value)
    }

    @Test
    fun lockClearsTheUnlockedSession() = runTest {
        appStateManager.unlock()
        assertTrue(appStateManager.isUnlocked.value)

        appStateManager.lock()

        assertFalse(appStateManager.isUnlocked.value)
    }

    @Test
    fun wipeAtTwentyAttemptsLocksTheSession() = runTest {
        appStateManager.unlock()
        every { securityPreferences.failedAttemptCount } returns flowOf(19)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 20

        assertTrue(appStateManager.onAuthenticationFailure() is AuthFailureResult.WalletWiped)

        assertFalse(appStateManager.isUnlocked.value)
    }

    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.test.runTest {
            block()
        }
    }
}

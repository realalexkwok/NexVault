package com.nexvault.wallet.core.datastore.state

import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.wallet.WalletMetadataDataStore
import com.nexvault.wallet.core.security.keystore.KeyStoreManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppStateManagerTest {

    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var securityPreferences: SecurityPreferencesDataStore
    private lateinit var walletMetadata: WalletMetadataDataStore
    private lateinit var walletStore: WalletStore
    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var appStateManager: AppStateManager

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        securityPreferences = mockk(relaxed = true)
        walletMetadata = mockk(relaxed = true)
        walletStore = mockk(relaxed = true)
        keyStoreManager = mockk(relaxed = true)

        every { userPreferences.autoLockTimeout } returns flowOf(AutoLockTimeout.FIVE_MINUTES)
        every { securityPreferences.isWalletSetUp } returns flowOf(true)
        every { securityPreferences.lastAuthTimestamp } returns flowOf(System.currentTimeMillis())
        every { securityPreferences.failedAttemptCount } returns flowOf(0)
        every { securityPreferences.lockoutEndTime } returns flowOf(0L)

        appStateManager = AppStateManager(
            userPreferences = userPreferences,
            securityPreferences = securityPreferences,
            walletMetadata = walletMetadata,
            walletStore = walletStore,
            keyStoreManager = keyStoreManager
        )
    }

    @Test
    fun onAuthenticationFailure_after5Failures_30SecondLockout() = runTest {
        every { securityPreferences.failedAttemptCount } returns flowOf(4)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 5

        val result = appStateManager.onAuthenticationFailure()

        assertTrue(result is AuthFailureResult.TemporaryLockout)
        val lockout = result as AuthFailureResult.TemporaryLockout
        assertEquals(30L, lockout.seconds)
        assertEquals(5, lockout.attemptCount)
    }

    @Test
    fun onAuthenticationFailure_after8Failures_5MinuteLockout() = runTest {
        every { securityPreferences.failedAttemptCount } returns flowOf(7)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 8

        val result = appStateManager.onAuthenticationFailure()

        assertTrue(result is AuthFailureResult.TemporaryLockout)
        val lockout = result as AuthFailureResult.TemporaryLockout
        assertEquals(300L, lockout.seconds)
        assertEquals(8, lockout.attemptCount)
    }

    @Test
    fun onAuthenticationFailure_after10Failures_15MinuteLockout() = runTest {
        every { securityPreferences.failedAttemptCount } returns flowOf(9)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 10

        val result = appStateManager.onAuthenticationFailure()

        assertTrue(result is AuthFailureResult.TemporaryLockout)
        val lockout = result as AuthFailureResult.TemporaryLockout
        assertEquals(900L, lockout.seconds)
        assertEquals(10, lockout.attemptCount)
    }

    @Test
    fun onAuthenticationFailure_after15Failures_1HourLockoutWithWarning() = runTest {
        every { securityPreferences.failedAttemptCount } returns flowOf(14)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 15

        val result = appStateManager.onAuthenticationFailure()

        assertTrue(result is AuthFailureResult.Warning)
        val warning = result as AuthFailureResult.Warning
        assertEquals(15, warning.attemptCount)
        assertTrue(warning.message.contains("1 hour"))
    }

    @Test
    fun onAuthenticationFailure_wipesWalletAt20AttemptsAndNotBefore() = runTest {
        // 19 attempts: escalation only, nothing is destroyed.
        every { securityPreferences.failedAttemptCount } returns flowOf(18)
        coEvery { securityPreferences.incrementFailedAttempts() } returns 19

        assertTrue(appStateManager.onAuthenticationFailure() is AuthFailureResult.Warning)
        coVerify(exactly = 0) { walletStore.wipeAll() }
        verify(exactly = 0) { keyStoreManager.deleteAllKeys() }
        coVerify(exactly = 0) { securityPreferences.clearAll() }

        // 20 attempts: terminal state — the wallet is really wiped, not just locked out.
        coEvery { securityPreferences.incrementFailedAttempts() } returns 20

        assertTrue(appStateManager.onAuthenticationFailure() is AuthFailureResult.WalletWiped)
        coVerify { walletStore.wipeAll() }
        verify { keyStoreManager.deleteAllKeys() }
        coVerify { userPreferences.clearAll() }
        coVerify { securityPreferences.clearAll() }
        coVerify { walletMetadata.clearAll() }
    }

    @Test
    fun onAuthenticationSuccess_resetsFailedAttempts() = runTest {
        coEvery { securityPreferences.resetFailedAttempts() } returns Unit
        coEvery { securityPreferences.setLockoutEndTime(any()) } returns Unit
        coEvery { securityPreferences.setLastAuthTimestamp(any()) } returns Unit

        appStateManager.onAuthenticationSuccess()

        coVerify { securityPreferences.resetFailedAttempts() }
        coVerify { securityPreferences.setLockoutEndTime(0L) }
        coVerify { securityPreferences.setLastAuthTimestamp(any()) }
    }

    @Test
    fun isFirstRun_whenNoWalletAndNoOnboarding_returnsTrue() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(false)
        every { userPreferences.hasCompletedOnboarding } returns flowOf(false)

        val result = appStateManager.isFirstRun

        result.collect { isFirst ->
            assertEquals(true, isFirst)
        }
    }

    @Test
    fun isFirstRun_whenWalletExists_returnsFalse() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(true)
        every { userPreferences.hasCompletedOnboarding } returns flowOf(false)

        val result = appStateManager.isFirstRun

        result.collect { isFirst ->
            assertEquals(false, isFirst)
        }
    }

    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.test.runTest {
            block()
        }
    }
}

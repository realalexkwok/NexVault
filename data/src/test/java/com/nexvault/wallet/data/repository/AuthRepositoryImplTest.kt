package com.nexvault.wallet.data.repository

import app.cash.turbine.test
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.AuthMethod
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.core.datastore.state.AuthFailureResult
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.core.security.biometric.BiometricStatus
import com.nexvault.wallet.core.security.util.SecurityUtils
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.model.common.DataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private lateinit var securityPreferences: SecurityPreferencesDataStore
    private lateinit var appStateManager: AppStateManager
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        securityPreferences = mockk(relaxed = true)
        appStateManager = mockk(relaxed = true)
        biometricHelper = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        repository = AuthRepositoryImpl(
            securityPreferences,
            appStateManager,
            biometricHelper,
            userPreferences,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(SecurityUtils)
    }

    @Test
    fun setPin_storesHashedPin() = runTest {
        coEvery { securityPreferences.storePasswordHash(any(), any()) } returns Unit
        coEvery { securityPreferences.setAuthMethod(any()) } returns Unit

        val result = repository.setPin("123456")

        assertTrue(result is DataResult.Success)
        coVerify { securityPreferences.storePasswordHash(any(), any()) }
        coVerify { securityPreferences.setAuthMethod(AuthMethod.PIN) }
    }

    @Test
    fun verifyPin_correctPin_returnsSuccess() = runTest {
        mockkObject(SecurityUtils)
        every { appStateManager.isLockedOut } returns flowOf(false)
        every { appStateManager.lockoutRemainingSeconds } returns flowOf(0L)
        coEvery { securityPreferences.getPasswordHash() } returns Pair("hashedpin123".toByteArray(), "salt".toByteArray())
        coEvery { securityPreferences.setLastAuthTimestamp(any()) } returns Unit
        coEvery { appStateManager.onAuthenticationSuccess() } returns Unit
        every { SecurityUtils.verifyPassword(any(), any()) } returns true

        val result = repository.verifyPin("123456")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun verifyPin_incorrectPin_returnsFailed() = runTest {
        mockkObject(SecurityUtils)
        every { appStateManager.isLockedOut } returns flowOf(false)
        every { appStateManager.lockoutRemainingSeconds } returns flowOf(0L)
        coEvery { securityPreferences.getPasswordHash() } returns Pair("hashedpin123".toByteArray(), "salt".toByteArray())
        coEvery { appStateManager.onAuthenticationFailure() } returns AuthFailureResult.NoLockout(1)
        every { SecurityUtils.verifyPassword(any(), any()) } returns false

        val result = repository.verifyPin("wrongpin")

        assertTrue(result is AuthResult.Failed)
    }

    @Test
    fun verifyPin_lockedOut_returnsLockedOut() = runTest {
        every { appStateManager.isLockedOut } returns flowOf(true)
        every { appStateManager.lockoutRemainingSeconds } returns flowOf(25L)

        val result = repository.verifyPin("123456")

        assertTrue(result is AuthResult.LockedOut)
        assertEquals(25L, (result as AuthResult.LockedOut).durationSeconds)
    }

    @Test
    fun changePin_success_returnsSuccess() = runTest {
        mockkObject(SecurityUtils)
        every { appStateManager.isLockedOut } returns flowOf(false)
        every { appStateManager.lockoutRemainingSeconds } returns flowOf(0L)
        coEvery { securityPreferences.getPasswordHash() } returns Pair("hashedpin123".toByteArray(), "salt".toByteArray())
        coEvery { securityPreferences.setLastAuthTimestamp(any()) } returns Unit
        coEvery { appStateManager.onAuthenticationSuccess() } returns Unit
        coEvery { securityPreferences.storePasswordHash(any(), any()) } returns Unit
        coEvery { securityPreferences.setAuthMethod(any()) } returns Unit
        every { SecurityUtils.verifyPassword(any(), any()) } returns true

        val result = repository.changePin("123456", "789012")

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun changePin_wrongCurrentPin_returnsError() = runTest {
        mockkObject(SecurityUtils)
        every { appStateManager.isLockedOut } returns flowOf(false)
        every { appStateManager.lockoutRemainingSeconds } returns flowOf(0L)
        coEvery { securityPreferences.getPasswordHash() } returns Pair("hashedpin123".toByteArray(), "salt".toByteArray())
        coEvery { appStateManager.onAuthenticationFailure() } returns AuthFailureResult.NoLockout(1)
        every { SecurityUtils.verifyPassword(any(), any()) } returns false

        val result = repository.changePin("wrong", "789012")

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun isBiometricAvailable_delegatesToHelper() = runTest {
        every { biometricHelper.isBiometricAvailable() } returns BiometricStatus.AVAILABLE

        val result = repository.isBiometricAvailable()

        assertTrue(result)
    }

    @Test
    fun onAuthSuccess_resetsState() = runTest {
        coEvery { securityPreferences.setLastAuthTimestamp(any()) } returns Unit
        coEvery { appStateManager.onAuthenticationSuccess() } returns Unit

        repository.onAuthSuccess()

        coVerify { securityPreferences.setLastAuthTimestamp(any()) }
        coVerify { appStateManager.onAuthenticationSuccess() }
    }

    @Test
    fun hasPinSet_reflectsDataStoreState() = runTest {
        every { securityPreferences.hasPasswordHash() } returns flowOf(true)

        repository.hasPinSet().test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }
}

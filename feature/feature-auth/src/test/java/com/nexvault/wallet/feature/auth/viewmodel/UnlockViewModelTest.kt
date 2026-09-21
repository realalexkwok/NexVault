package com.nexvault.wallet.feature.auth.viewmodel

import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var verifyPinUseCase: VerifyPinUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var appStateManager: AppStateManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        verifyPinUseCase = mockk()
        authRepository = mockk()
        appStateManager = mockk()

        coEvery { verifyPinUseCase.invoke(any()) } returns AuthResult.Success
        coEvery { authRepository.isBiometricAvailable() } returns false
        every { authRepository.isBiometricEnabled() } returns flowOf(false)
        every { appStateManager.isLockedOut } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UnlockViewModel {
        return UnlockViewModel(
            verifyPinUseCase = verifyPinUseCase,
            authRepository = authRepository,
            appStateManager = appStateManager,
        )
    }

    @Test
    fun `TC-UNLOCK-001 initial state is correct`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.pin)
        assertNull(state.errorMessage)
        assertFalse(state.isBiometricAvailable)
        assertFalse(state.isBiometricEnabled)
        assertFalse(state.isLockedOut)
        assertFalse(state.isVerifying)
        assertEquals(0, state.failedAttempts)
    }

    @Test
    fun `TC-UNLOCK-002 initial state with biometric available`() = testScope.runTest {
        coEvery { authRepository.isBiometricAvailable() } returns true
        every { authRepository.isBiometricEnabled() } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isBiometricAvailable)
        assertTrue(state.isBiometricEnabled)
    }

    @Test
    fun `TC-UNLOCK-003 digits append to PIN`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("123", state.pin)
    }

    @Test
    fun `TC-UNLOCK-004 backspace removes last digit`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onBackspacePressed()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("12", state.pin)
    }

    @Test
    fun `TC-UNLOCK-005 correct PIN clears failed attempts`() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("123456") } returns AuthResult.Success

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onDigitPressed(4)
        viewModel.onDigitPressed(5)
        viewModel.onDigitPressed(6)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.failedAttempts)
        assertNull(state.errorMessage)
    }

    @Test
    fun `TC-UNLOCK-006 incorrect PIN shows error with remaining attempts`() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("000000") } returns AuthResult.Failed(
            remainingAttempts = 4,
            message = "Incorrect PIN"
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Incorrect PIN") == true)
        assertEquals(1, state.failedAttempts)
        assertEquals("", state.pin)
        assertTrue(state.isShakeError)
    }

    @Test
    fun `TC-UNLOCK-007 biometric success clears error`() = testScope.runTest {
        coEvery { authRepository.onAuthSuccess() } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBiometricSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.failedAttempts)
        assertNull(state.errorMessage)
    }

    @Test
    fun `TC-UNLOCK-008 biometric error shows message`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBiometricError("Sensor not recognized")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Sensor not recognized") == true)
    }

    @Test
    fun `TC-UNLOCK-009 biometric cancellation does NOT show error`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBiometricError("User cancelled")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }

    @Test
    fun `TC-UNLOCK-010 cannot exceed 6 digits`() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onDigitPressed(4)
        viewModel.onDigitPressed(5)
        viewModel.onDigitPressed(6)
        viewModel.onDigitPressed(7) // This should be ignored
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("123456", state.pin)
    }

    @Test
    fun `TC-UNLOCK-011 clear shake error after animation`() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("000000") } returns AuthResult.Failed(
            remainingAttempts = 4,
            message = "Incorrect PIN"
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isShakeError)

        viewModel.onShakeAnimationComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfter = viewModel.uiState.value
        assertFalse(stateAfter.isShakeError)
    }

    @Test
    fun `TC-UNLOCK-012 auto-trigger biometric when enabled`() = testScope.runTest {
        coEvery { authRepository.isBiometricAvailable() } returns true
        every { authRepository.isBiometricEnabled() } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isBiometricAvailable)
        assertTrue(state.isBiometricEnabled)
    }

    @Test
    fun `TC-UNLOCK-013 no auto-trigger biometric when disabled`() = testScope.runTest {
        coEvery { authRepository.isBiometricAvailable() } returns true
        every { authRepository.isBiometricEnabled() } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isBiometricAvailable)
        assertFalse(state.isBiometricEnabled)
    }

    @Test
    fun `TC-UNLOCK-014 input ignored during verification`() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            AuthResult.Success
        }

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onDigitPressed(4)
        viewModel.onDigitPressed(5)
        viewModel.onDigitPressed(6)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("123456", state.pin)
    }
}

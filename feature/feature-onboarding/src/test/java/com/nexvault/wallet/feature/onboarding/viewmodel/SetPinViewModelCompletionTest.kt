package com.nexvault.wallet.feature.onboarding.viewmodel

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.SetPinUseCase
import com.nexvault.wallet.domain.usecase.wallet.CompleteOnboardingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

/**
 * Roadmap 2.0.2b, option A: onboarding completes only after the PIN is stored, and only then
 * does the screen navigate to main. Kept separate from [SetPinViewModelTest] so both classes
 * stay within the detekt function-count threshold.
 */
class SetPinViewModelCompletionTest {

    private lateinit var setPinUseCase: SetPinUseCase
    private lateinit var completeOnboardingUseCase: CompleteOnboardingUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SetPinViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        setPinUseCase = mockk(relaxed = true)
        completeOnboardingUseCase = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        coEvery { authRepository.isBiometricAvailable() } returns false
        viewModel = SetPinViewModel(setPinUseCase, completeOnboardingUseCase, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun enterPin(pin: String = "123456") {
        pin.forEach { viewModel.onDigitPressed(it.digitToInt()) }
    }

    @Test
    fun matchingPinsCompleteOnboardingThenNavigate() = runTest {
        coEvery { setPinUseCase.invoke(any()) } returns DataResult.Success(Unit)
        coEvery { completeOnboardingUseCase.invoke() } returns DataResult.Success(Unit)

        val events = mutableListOf<SetPinViewModel.NavigationEvent>()
        val collector = launch { viewModel.navigationEvent.collect { events.add(it) } }

        enterPin()
        enterPin()
        advanceUntilIdle()
        collector.cancel()

        coVerify(exactly = 1) { setPinUseCase.invoke("123456") }
        // Option A: onboarding completes only here, after the PIN is stored.
        coVerify(exactly = 1) { completeOnboardingUseCase.invoke() }
        assertEquals(1, events.size)
        assertTrue(events.first() is SetPinViewModel.NavigationEvent.NavigateToMain)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun biometricChoiceIsSavedBeforeOnboardingCompletes() = runTest {
        // Device-walk regression (2026-09-25): completing onboarding switches the root router to
        // `main`, which cancels this ViewModel's scope — a biometric write attempted after that
        // never lands. It must happen first.
        coEvery { setPinUseCase.invoke(any()) } returns DataResult.Success(Unit)
        coEvery { completeOnboardingUseCase.invoke() } returns DataResult.Success(Unit)
        coEvery { authRepository.setBiometricEnabled(any()) } returns DataResult.Success(Unit)

        viewModel.onBiometricToggled(true)
        enterPin()
        enterPin()
        advanceUntilIdle()

        coVerifyOrder {
            authRepository.setBiometricEnabled(true)
            completeOnboardingUseCase.invoke()
        }
    }

    @Test
    fun completionFailureBlocksNavigation() = runTest {
        coEvery { setPinUseCase.invoke(any()) } returns DataResult.Success(Unit)
        coEvery { completeOnboardingUseCase.invoke() } returns DataResult.Error(
            exception = Exception("DataStore offline"),
            message = "DataStore offline",
        )

        val events = mutableListOf<SetPinViewModel.NavigationEvent>()
        val collector = launch { viewModel.navigationEvent.collect { events.add(it) } }

        enterPin()
        enterPin()
        advanceUntilIdle()
        collector.cancel()

        assertTrue(events.isEmpty())
        assertTrue(viewModel.uiState.value.errorMessage?.contains("DataStore offline") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun pinStoreFailureBlocksOnboardingCompletion() = runTest {
        coEvery { setPinUseCase.invoke(any()) } returns DataResult.Error(
            exception = Exception("Keystore unavailable"),
            message = "Keystore unavailable",
        )

        val events = mutableListOf<SetPinViewModel.NavigationEvent>()
        val collector = launch { viewModel.navigationEvent.collect { events.add(it) } }

        enterPin()
        enterPin()
        advanceUntilIdle()
        collector.cancel()

        coVerify(exactly = 0) { completeOnboardingUseCase.invoke() }
        assertTrue(events.isEmpty())
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Keystore unavailable") == true)
    }
}

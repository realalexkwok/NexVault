package com.nexvault.wallet.feature.onboarding.viewmodel

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.SetPinUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetPinViewModelTest {

    private lateinit var setPinUseCase: SetPinUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SetPinViewModel

    @Before
    fun setup() {
        setPinUseCase = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        coEvery { authRepository.isBiometricAvailable() } returns false
        viewModel = SetPinViewModel(setPinUseCase, authRepository)
    }

    @Test
    fun initialStateIsSetPhase() {
        val state = viewModel.uiState.value
        assertEquals(SetPinViewModel.PinPhase.SET, state.phase)
        assertEquals("", state.pin)
        assertNull(state.errorRes)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertFalse(state.isBiometricAvailable)
        assertFalse(state.isBiometricEnabled)
        assertFalse(state.isShakeError)
    }

    @Test
    fun digitsAppendToPin() {
        viewModel.onDigitPressed(1)
        assertEquals("1", viewModel.uiState.value.pin)

        viewModel.onDigitPressed(2)
        assertEquals("12", viewModel.uiState.value.pin)

        viewModel.onDigitPressed(3)
        assertEquals("123", viewModel.uiState.value.pin)
    }

    @Test
    fun backspaceRemovesLastDigit() {
        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)

        viewModel.onBackspacePressed()

        assertEquals("12", viewModel.uiState.value.pin)
    }

    @Test
    fun backspaceOnEmptyPinIsNoOp() {
        viewModel.onBackspacePressed()
        assertEquals("", viewModel.uiState.value.pin)
    }

    @Test
    fun sixDigitsInSetPhaseTransitionsToConfirm() {
        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onDigitPressed(4)
        viewModel.onDigitPressed(5)
        viewModel.onDigitPressed(6)

        val state = viewModel.uiState.value
        assertEquals(SetPinViewModel.PinPhase.CONFIRM, state.phase)
        assertEquals("", state.pin)
    }

    @Test
    fun cannotExceedSixDigitsInSetPhase() {
        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        viewModel.onDigitPressed(4)
        viewModel.onDigitPressed(5)
        viewModel.onDigitPressed(6)

        // After 6 digits, phase changes to CONFIRM
        val state = viewModel.uiState.value
        assertEquals(SetPinViewModel.PinPhase.CONFIRM, state.phase)
    }

    @Test
    fun biometricToggleUpdatesState() {
        assertFalse(viewModel.uiState.value.isBiometricEnabled)

        viewModel.onBiometricToggled(true)
        assertTrue(viewModel.uiState.value.isBiometricEnabled)

        viewModel.onBiometricToggled(false)
        assertFalse(viewModel.uiState.value.isBiometricEnabled)
    }
}

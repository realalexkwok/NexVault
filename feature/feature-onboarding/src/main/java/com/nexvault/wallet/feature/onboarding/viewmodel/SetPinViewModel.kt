package com.nexvault.wallet.feature.onboarding.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.SetPinUseCase
import com.nexvault.wallet.domain.usecase.wallet.CompleteOnboardingUseCase
import com.nexvault.wallet.feature.onboarding.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Set PIN screen.
 *
 * Two-phase flow:
 * 1. "Set Your PIN" — user enters a 6-digit PIN
 * 2. "Confirm Your PIN" — user re-enters the same PIN
 *
 * If PINs match, the PIN is stored, onboarding is marked complete and biometric preference is
 * set. If PINs don't match, error is shown and user resets to phase 1.
 *
 * Onboarding completion (which writes `is_wallet_set_up` and unlocks the session) happens here
 * and nowhere earlier — that is what stops the root router from swapping graphs mid-flow
 * (roadmap 2.0.2b, option A).
 *
 * Also handles optional biometric enable toggle.
 */
@HiltViewModel
class SetPinViewModel @Inject constructor(
    private val setPinUseCase: SetPinUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    enum class PinPhase {
        SET,
        CONFIRM,
    }

    data class UiState(
        val phase: PinPhase = PinPhase.SET,
        val pin: String = "",
        @StringRes val errorRes: Int? = null,
        val errorArgs: List<Any> = emptyList(),
        val errorMessage: String? = null,
        val isLoading: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val isBiometricEnabled: Boolean = false,
        val isShakeError: Boolean = false,
    )

    sealed interface NavigationEvent {
        data object NavigateToMain : NavigationEvent
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private var firstPin: String = ""

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        viewModelScope.launch {
            val available = authRepository.isBiometricAvailable()
            _uiState.update { it.copy(isBiometricAvailable = available) }
        }
    }

    /**
     * Called when a digit is pressed on the PIN keypad.
     * Appends the digit to the current PIN.
     * When PIN reaches 6 digits, automatically handles phase transition.
     */
    fun onDigitPressed(digit: Int) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.pin.length >= 6) return

        val newPin = currentState.pin + digit.toString()
        _uiState.update {
            it.copy(pin = newPin, errorRes = null, errorMessage = null, isShakeError = false)
        }

        if (newPin.length == 6) {
            handlePinComplete(newPin)
        }
    }

    /**
     * Called when the backspace/delete button is pressed on the PIN keypad.
     * Removes the last digit from the current PIN.
     */
    fun onBackspacePressed() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.pin.isEmpty()) return

        _uiState.update {
            it.copy(
                pin = it.pin.dropLast(1),
                errorRes = null,
                errorMessage = null,
                isShakeError = false,
            )
        }
    }

    private fun handlePinComplete(pin: String) {
        when (_uiState.value.phase) {
            PinPhase.SET -> {
                firstPin = pin
                _uiState.update {
                    it.copy(
                        phase = PinPhase.CONFIRM,
                        pin = "",
                        errorRes = null,
                        errorMessage = null,
                    )
                }
            }
            PinPhase.CONFIRM -> {
                if (pin == firstPin) {
                    storePin(pin)
                } else {
                    _uiState.update {
                        it.copy(
                            isShakeError = true,
                            errorRes = R.string.set_pin_mismatch,
                            errorArgs = emptyList(),
                            errorMessage = null,
                        )
                    }
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1000)
                        firstPin = ""
                        _uiState.update {
                            it.copy(
                                phase = PinPhase.SET,
                                pin = "",
                                errorRes = null,
                                errorMessage = null,
                                isShakeError = false,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun storePin(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = setPinUseCase(pin)

                when (result) {
                    is DataResult.Success -> {
                        // Save the biometric choice *before* onboarding completes: completion
                        // switches the root router to `main`, which destroys this screen and
                        // cancels this ViewModel's scope (found by the 2.0.2b device walk).
                        if (_uiState.value.isBiometricEnabled) {
                            authRepository.setBiometricEnabled(true)
                        }
                        when (val completion = completeOnboardingUseCase()) {
                            is DataResult.Success -> {
                                _uiState.update { it.copy(isLoading = false) }
                                _navigationEvent.tryEmit(NavigationEvent.NavigateToMain)
                            }
                            is DataResult.Error -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorRes = R.string.set_pin_failed,
                                        errorArgs = emptyList(),
                                        errorMessage = completion.message,
                                    )
                                }
                            }
                        }
                    }
                    is DataResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorRes = R.string.set_pin_failed,
                                errorArgs = emptyList(),
                                errorMessage = result.message,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorRes = R.string.set_pin_failed_with_reason,
                        errorArgs = listOf(e.message.orEmpty()),
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun onBiometricToggled(enabled: Boolean) {
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    /**
     * Clear shake error state after animation completes.
     * Called from the composable after the shake animation finishes.
     */
    fun onShakeAnimationComplete() {
        _uiState.update { it.copy(isShakeError = false) }
    }
}

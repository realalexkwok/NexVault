package com.nexvault.wallet.feature.auth.viewmodel

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import com.nexvault.wallet.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Unlock screen.
 *
 * Handles PIN verification, biometric authentication trigger,
 * and lockout after consecutive failed attempts.
 *
 * Lockout behavior (ref: AC-1.5):
 * - After 5 consecutive wrong PIN attempts, lock the user out for 30 seconds.
 * - Display a countdown timer during lockout.
 * - After the countdown, allow PIN entry again (attempt counter resets).
 *
 * Biometric behavior:
 * - If biometric is enabled in preferences, auto-trigger biometric prompt on screen load.
 * - Provide a "Use Biometric" button as a fallback to re-trigger.
 * - Successful biometric auth navigates to main graph directly (no PIN needed).
 */
@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
        private val verifyPinUseCase: VerifyPinUseCase,
        private val authRepository: AuthRepository,
        private val appStateManager: AppStateManager,
    ) : ViewModel() {
        companion object {
            private const val MAX_ATTEMPTS = 5
            private const val LOCKOUT_DURATION_SECONDS = 30
            private const val PIN_LENGTH = 6
        }

        data class UiState(
            val pin: String = "",
            @StringRes val errorRes: Int? = null,
            @PluralsRes val errorPluralsRes: Int? = null,
            val errorQuantity: Int = 0,
            val errorArgs: List<Any> = emptyList(),
            val errorMessage: String? = null,
            val isLockedOut: Boolean = false,
            val lockoutRemainingSeconds: Int = 0,
            val isBiometricAvailable: Boolean = false,
            val isBiometricEnabled: Boolean = false,
            val isVerifying: Boolean = false,
            val isShakeError: Boolean = false,
            val failedAttempts: Int = 0,
        )

        sealed interface NavigationEvent {
            data object NavigateToMain : NavigationEvent

            data object NavigateToOnboarding : NavigationEvent
        }

        sealed interface BiometricEvent {
            data object ShowBiometricPrompt : BiometricEvent
        }

        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
        val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

        private val _biometricEvent = MutableSharedFlow<BiometricEvent>(extraBufferCapacity = 1)
        val biometricEvent: SharedFlow<BiometricEvent> = _biometricEvent.asSharedFlow()

        private var lockoutJob: Job? = null

        /**
         * Replaces the whole error state so a stale resource, quantity or message can never survive
         * the next error: [UiState.errorMessage] wins over the resource fields when both are set.
         */
        private fun UiState.withError(
            @StringRes res: Int? = null,
            @PluralsRes pluralsRes: Int? = null,
            quantity: Int = 0,
            args: List<Any> = emptyList(),
            message: String? = null,
        ): UiState =
            copy(
                errorRes = res,
                errorPluralsRes = pluralsRes,
                errorQuantity = quantity,
                errorArgs = args,
                errorMessage = message,
            )

        init {
            checkBiometricStatus()
            observeLockoutState()
        }

        private fun checkBiometricStatus() {
            viewModelScope.launch {
                val available = authRepository.isBiometricAvailable()
                val enabled = authRepository.isBiometricEnabled().first()

                _uiState.update {
                    it.copy(
                        isBiometricAvailable = available,
                        isBiometricEnabled = available && enabled,
                    )
                }

                // Auto-trigger biometric prompt if enabled
                if (available && enabled) {
                    _biometricEvent.tryEmit(BiometricEvent.ShowBiometricPrompt)
                }
            }
        }

        private fun observeLockoutState() {
            viewModelScope.launch {
                appStateManager.isLockedOut.collect { isLockedOut ->
                    if (isLockedOut) {
                        startLockoutCountdown()
                    }
                }
            }
        }

        /**
         * Called when a digit is pressed on the PIN keypad.
         * When PIN reaches 6 digits, automatically verifies.
         */
        fun onDigitPressed(digit: Int) {
            val currentState = _uiState.value
            if (currentState.isLockedOut || currentState.isVerifying || currentState.pin.length >= PIN_LENGTH) return

            val newPin = currentState.pin + digit.toString()
            _uiState.update { it.copy(pin = newPin, isShakeError = false).withError() }

            if (newPin.length == PIN_LENGTH) {
                verifyPin(newPin)
            }
        }

        /**
         * Called when the backspace/delete button is pressed.
         */
        fun onBackspacePressed() {
            val currentState = _uiState.value
            if (currentState.isLockedOut || currentState.isVerifying || currentState.pin.isEmpty()) return

            _uiState.update {
                it
                    .copy(
                        pin = it.pin.dropLast(1),
                        isShakeError = false,
                    ).withError()
            }
        }

        private fun verifyPin(pin: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isVerifying = true) }

                try {
                    val result = verifyPinUseCase(pin)

                    when (result) {
                        is AuthResult.Success -> {
                            _uiState.update {
                                it
                                    .copy(
                                        isVerifying = false,
                                        failedAttempts = 0,
                                    ).withError()
                            }
                            _navigationEvent.tryEmit(NavigationEvent.NavigateToMain)
                        }

                        is AuthResult.Failed -> {
                            handleFailedAttempt(result.remainingAttempts, result.message)
                        }

                        is AuthResult.LockedOut -> {
                            _uiState.update {
                                it
                                    .copy(
                                        isVerifying = false,
                                        pin = "",
                                        isLockedOut = true,
                                        lockoutRemainingSeconds = result.durationSeconds.toInt(),
                                        isShakeError = true,
                                    ).withError(res = R.string.unlock_too_many_attempts)
                            }
                            startLockoutCountdown()
                        }

                        is AuthResult.WalletWiped -> {
                            _uiState.update {
                                it
                                    .copy(
                                        isVerifying = false,
                                        pin = "",
                                        isShakeError = true,
                                    ).withError(res = R.string.unlock_wallet_wiped)
                            }
                            _navigationEvent.tryEmit(NavigationEvent.NavigateToOnboarding)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it
                            .copy(
                                isVerifying = false,
                                pin = "",
                                isShakeError = true,
                            ).withError(
                                res = R.string.unlock_verification_error,
                                args = listOf(e.message.orEmpty()),
                            )
                    }
                }
            }
        }

        private fun handleFailedAttempt(
            remainingAttempts: Int?,
            message: String,
        ) {
            val newFailedAttempts = _uiState.value.failedAttempts + 1

            if (newFailedAttempts >= MAX_ATTEMPTS) {
                _uiState.update {
                    it
                        .copy(
                            isVerifying = false,
                            pin = "",
                            failedAttempts = newFailedAttempts,
                            isLockedOut = true,
                            lockoutRemainingSeconds = LOCKOUT_DURATION_SECONDS,
                            isShakeError = true,
                        ).withError(res = R.string.unlock_too_many_attempts)
                }
                startLockoutCountdown()
            } else {
                val remaining = MAX_ATTEMPTS - newFailedAttempts
                val fallbackPluralsRes = if (message.isEmpty()) R.plurals.unlock_incorrect_pin_attempts else null
                _uiState.update {
                    it
                        .copy(
                            isVerifying = false,
                            pin = "",
                            failedAttempts = newFailedAttempts,
                            isShakeError = true,
                        ).withError(
                            pluralsRes = fallbackPluralsRes,
                            quantity = remaining,
                            message = message.ifEmpty { null },
                        )
                }
            }
        }

        private fun startLockoutCountdown() {
            lockoutJob?.cancel()
            lockoutJob =
                viewModelScope.launch {
                    for (remaining in LOCKOUT_DURATION_SECONDS downTo 1) {
                        _uiState.update { it.copy(lockoutRemainingSeconds = remaining) }
                        delay(1000L)
                    }
                    // Lockout expired — reset
                    _uiState.update {
                        it
                            .copy(
                                isLockedOut = false,
                                lockoutRemainingSeconds = 0,
                                failedAttempts = 0,
                                isShakeError = false,
                            ).withError()
                    }
                }
        }

        /**
         * Called when the user taps "Use Biometric" button to re-trigger the prompt.
         */
        fun onBiometricRequested() {
            if (_uiState.value.isLockedOut) return
            _biometricEvent.tryEmit(BiometricEvent.ShowBiometricPrompt)
        }

        /**
         * Called when biometric authentication succeeds.
         */
        fun onBiometricSuccess() {
            viewModelScope.launch {
                authRepository.onAuthSuccess()
            }
            _uiState.update {
                it
                    .copy(failedAttempts = 0)
                    .withError()
            }
            _navigationEvent.tryEmit(NavigationEvent.NavigateToMain)
        }

        /**
         * Called when biometric authentication fails or is cancelled.
         * User can still use PIN as fallback.
         */
        fun onBiometricError(errorMessage: String?) {
            if (errorMessage != null && !errorMessage.contains("cancel", ignoreCase = true)) {
                _uiState.update {
                    it.withError(message = errorMessage)
                }
            }
        }

        /**
         * Clear shake error state after animation completes.
         */
        fun onShakeAnimationComplete() {
            _uiState.update { it.copy(isShakeError = false) }
        }

        override fun onCleared() {
            super.onCleared()
            lockoutJob?.cancel()
        }
    }

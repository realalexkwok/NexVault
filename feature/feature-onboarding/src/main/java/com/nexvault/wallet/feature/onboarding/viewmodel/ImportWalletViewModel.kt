package com.nexvault.wallet.feature.onboarding.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.usecase.wallet.ImportWalletUseCase
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
 * ViewModel for the Import Wallet screen.
 *
 * Supports two import modes:
 * 1. Mnemonic phrase (12 or 24 words separated by spaces)
 * 2. Raw private key (64 hex characters, optionally prefixed with 0x)
 *
 * Validates input in real time and shows appropriate error messages.
 * On successful import, navigates to the Set PIN screen.
 */
@HiltViewModel
class ImportWalletViewModel @Inject constructor(
    private val importWalletUseCase: ImportWalletUseCase,
) : ViewModel() {

    enum class ImportMode {
        MNEMONIC,
        PRIVATE_KEY,
    }

    /**
     * Validation failure that carries runtime values, so the UI resolves it
     * from a resource id plus positional arguments.
     */
    private data class ValidationError(
        @StringRes val messageRes: Int,
        val args: List<Any> = emptyList(),
    )

    data class UiState(
        val importMode: ImportMode = ImportMode.MNEMONIC,
        val mnemonicInput: String = "",
        val privateKeyInput: String = "",
        @StringRes val mnemonicErrorRes: Int? = null,
        val mnemonicErrorArgs: List<Any> = emptyList(),
        @StringRes val privateKeyErrorRes: Int? = null,
        val isLoading: Boolean = false,
        @StringRes val generalErrorRes: Int? = null,
        val generalErrorMessage: String? = null,
        val isImportEnabled: Boolean = false,
    )

    sealed interface NavigationEvent {
        data object NavigateToSetPin : NavigationEvent
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    fun onImportModeChanged(mode: ImportMode) {
        _uiState.update {
            it.copy(
                importMode = mode,
                mnemonicErrorRes = null,
                mnemonicErrorArgs = emptyList(),
                privateKeyErrorRes = null,
                generalErrorRes = null,
                generalErrorMessage = null,
                isImportEnabled = when (mode) {
                    ImportMode.MNEMONIC -> isValidMnemonicFormat(it.mnemonicInput)
                    ImportMode.PRIVATE_KEY -> isValidPrivateKeyFormat(it.privateKeyInput)
                },
            )
        }
    }

    fun onMnemonicInputChanged(input: String) {
        val trimmed = input.lowercase().trim()
        val error = validateMnemonicInput(trimmed)
        _uiState.update {
            it.copy(
                mnemonicInput = input,
                mnemonicErrorRes = error?.messageRes,
                mnemonicErrorArgs = error?.args ?: emptyList(),
                generalErrorRes = null,
                generalErrorMessage = null,
                isImportEnabled = error == null && isValidMnemonicFormat(trimmed),
            )
        }
    }

    fun onPrivateKeyInputChanged(input: String) {
        val trimmed = input.trim()
        val error = validatePrivateKeyInput(trimmed)
        _uiState.update {
            it.copy(
                privateKeyInput = input,
                privateKeyErrorRes = error,
                generalErrorRes = null,
                generalErrorMessage = null,
                isImportEnabled = error == null && isValidPrivateKeyFormat(trimmed),
            )
        }
    }

    fun onImportClicked() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalErrorRes = null, generalErrorMessage = null) }

            val result = when (currentState.importMode) {
                ImportMode.MNEMONIC -> {
                    val mnemonic = currentState.mnemonicInput.trim()
                        .lowercase()
                        .replace("\\s+".toRegex(), " ")
                    importWalletUseCase.fromMnemonic(mnemonic)
                }
                ImportMode.PRIVATE_KEY -> {
                    val key = currentState.privateKeyInput.trim()
                        .removePrefix("0x")
                        .removePrefix("0X")
                    importWalletUseCase.fromPrivateKey(key)
                }
            }

            when (result) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.tryEmit(NavigationEvent.NavigateToSetPin)
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalErrorRes = R.string.import_wallet_failed,
                            generalErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    /**
     * Real-time validation for mnemonic input.
     * Returns the error resource (with its arguments) or null if input is valid so far.
     * Note: this is format validation, not BIP-39 checksum validation.
     * Full checksum validation happens when the user taps Import.
     */
    private fun validateMnemonicInput(input: String): ValidationError? {
        if (input.isEmpty()) return null

        val words = input.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        if (words.size > 24) {
            return ValidationError(R.string.import_wallet_mnemonic_too_many_words)
        }

        val hasInvalidChars = words.any { word -> !word.all { it.isLetter() } }
        if (hasInvalidChars) {
            return ValidationError(R.string.import_wallet_mnemonic_invalid_chars)
        }

        if (words.size in 13..23) {
            return ValidationError(
                messageRes = R.string.import_wallet_mnemonic_wrong_count,
                args = listOf(words.size),
            )
        }

        return null
    }

    private fun isValidMnemonicFormat(input: String): Boolean {
        val words = input.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return words.size == 12 || words.size == 24
    }

    private fun validatePrivateKeyInput(input: String): Int? {
        if (input.isEmpty()) return null

        val cleaned = input.removePrefix("0x").removePrefix("0X")

        if (cleaned.length > 64) {
            return R.string.import_wallet_private_key_too_long
        }

        val hasInvalidChars = !cleaned.all {
            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
        }
        if (hasInvalidChars && cleaned.isNotEmpty()) {
            return R.string.import_wallet_private_key_not_hex
        }

        return null
    }

    private fun isValidPrivateKeyFormat(input: String): Boolean {
        val cleaned = input.removePrefix("0x").removePrefix("0X")
        return cleaned.length == 64 && cleaned.all {
            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
        }
    }
}

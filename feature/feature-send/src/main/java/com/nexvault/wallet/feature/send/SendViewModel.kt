package com.nexvault.wallet.feature.send

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InsufficientBalanceException
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.transaction.EstimateSendGasUseCase
import com.nexvault.wallet.domain.usecase.transaction.SendTransactionUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for the send flow: form -> gas estimate -> PIN-confirmed submit -> result.
 *
 * Roadmap 2.6. The PIN re-entry is the AC-2.6 confirmation ("PIN or biometric"): the review step
 * opens the PIN dialog and only a verified PIN triggers the submit (TC-VM-003…005 cover the form).
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getActiveWalletUseCase: GetActiveWalletUseCase,
    private val estimateSendGasUseCase: EstimateSendGasUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            SendUiState(selectedTokenAddress = savedStateHandle.get<String>("tokenAddress")),
        )
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    private var estimateJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(chain = getSelectedChainUseCase().first()) }
        }
        viewModelScope.launch {
            getPortfolioUseCase().collect { result ->
                if (result is DataResult.Success) {
                    _uiState.update { it.copy(tokens = result.data.tokens) }
                    scheduleGasEstimate()
                }
            }
        }
    }

    fun onToAddressChanged(value: String) {
        val cleaned = value.trim()
        _uiState.update {
            it.copy(
                toAddress = cleaned,
                addressErrorRes = if (isValidAddress(cleaned)) null else R.string.send_error_invalid_address,
            )
        }
        scheduleGasEstimate()
    }

    fun onAmountChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                amount = value,
                amountErrorRes = amountError(state, value),
            )
        }
        scheduleGasEstimate()
    }

    fun onTokenSelected(tokenAddress: String?) {
        _uiState.update { it.copy(selectedTokenAddress = tokenAddress, amount = "", amountErrorRes = null) }
        scheduleGasEstimate()
    }

    fun onGasSelected(index: Int) {
        _uiState.update { it.copy(selectedGasIndex = index) }
    }

    /**
     * TC-VM-005: MAX fills the balance minus the estimated gas cost (native), or the full token
     * balance (ERC-20 — the fee is paid in the native coin).
     */
    fun onMaxClicked() {
        val state = _uiState.value
        val token = state.selectedToken
        val max =
            if (token == null) {
                val native = state.nativeToken?.balance ?: BigDecimal.ZERO
                val fee = gasFeeInNativeUnits(state.gasEstimate, state.selectedGasIndex)
                (native - fee).coerceAtLeast(BigDecimal.ZERO)
            } else {
                token.balance
            }
        _uiState.update { it.copy(amount = max.stripTrailingZeros().toPlainString(), amountErrorRes = null) }
    }

    fun onSubmitClicked() {
        val state = _uiState.value
        if (!state.isFormValid) return
        _uiState.update { it.copy(showPinDialog = true, pin = "", pinErrorRes = null) }
    }

    fun onPinDialogDismissed() {
        _uiState.update { it.copy(showPinDialog = false, pin = "", pinErrorRes = null) }
    }

    fun onPinDigitPressed(digit: Int) {
        val state = _uiState.value
        if (state.pin.length >= PIN_LENGTH || state.isSubmitting) return
        val newPin = state.pin + digit
        _uiState.update { it.copy(pin = newPin, pinErrorRes = null) }
        if (newPin.length == PIN_LENGTH) {
            confirmAndSubmit(newPin)
        }
    }

    fun onPinBackspacePressed() {
        _uiState.update { state ->
            if (state.pin.isEmpty()) state else state.copy(pin = state.pin.dropLast(1))
        }
    }

    fun onResultDone() {
        _uiState.update { it.copy(submittedHash = null, showPinDialog = false, pin = "") }
    }

    private fun confirmAndSubmit(pin: String) {
        viewModelScope.launch {
            when (verifyPinUseCase(pin)) {
                is AuthResult.Success -> submit()
                is AuthResult.LockedOut ->
                    _uiState.update { it.copy(pin = "", pinErrorRes = R.string.send_pin_error) }
                else -> _uiState.update { it.copy(pin = "", pinErrorRes = R.string.send_pin_error) }
            }
        }
    }

    private suspend fun submit() {
        val state = _uiState.value
        val chain = state.chain ?: return
        val wallet = getActiveWalletUseCase().first() ?: return
        val account = wallet.accounts.firstOrNull { it.isActive } ?: return
        val gas = state.selectedGas ?: return
        _uiState.update { it.copy(isSubmitting = true, pinErrorRes = null) }
        val params =
            SendTransactionParams(
                toAddress = state.toAddress,
                amount = state.amount.toBigDecimal(),
                tokenAddress = state.selectedTokenAddress,
                gasOption = gas,
                chainId = chain.chainId,
                data = null,
            )
        when (val result = sendTransactionUseCase(params, wallet.id, account.index)) {
            is DataResult.Success -> {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        showPinDialog = false,
                        pin = "",
                        submittedHash = result.data,
                        errorMessage = null,
                    )
                }
            }
            is DataResult.Error -> {
                val notConfigured = result.exception is ApiKeyNotConfiguredException
                val insufficient = result.exception is InsufficientBalanceException
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        showPinDialog = false,
                        pin = "",
                        isRpcNotConfigured = notConfigured,
                        amountErrorRes =
                            if (insufficient) {
                                R.string.send_error_insufficient_balance
                            } else {
                                it.amountErrorRes
                            },
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun scheduleGasEstimate() {
        estimateJob?.cancel()
        estimateJob =
            viewModelScope.launch {
                delay(ESTIMATE_DEBOUNCE_MILLIS)
                refreshGasEstimate()
            }
    }

    private suspend fun refreshGasEstimate() {
        val state = _uiState.value
        val chain = state.chain ?: return
        val wallet = getActiveWalletUseCase().first() ?: return
        val account = wallet.accounts.firstOrNull { it.isActive } ?: return
        if (!isValidAddress(state.toAddress)) return
        val amount = state.amount.toBigDecimalOrNull() ?: return
        if (amount <= BigDecimal.ZERO) return
        _uiState.update { it.copy(isEstimating = true, isRpcNotConfigured = false) }
        when (
            val result =
                estimateSendGasUseCase(
                    fromAddress = account.address,
                    toAddress = state.toAddress,
                    amount = amount,
                    tokenAddress = state.selectedTokenAddress,
                    chainId = chain.chainId,
                )
        ) {
            is DataResult.Success -> _uiState.update { it.copy(isEstimating = false, gasEstimate = result.data) }
            is DataResult.Error -> {
                val notConfigured = result.exception is ApiKeyNotConfiguredException
                _uiState.update {
                    it.copy(
                        isEstimating = false,
                        gasEstimate = null,
                        isRpcNotConfigured = notConfigured,
                    )
                }
            }
        }
    }

    private fun amountError(state: SendUiState, value: String): Int? {
        val parsed = value.toBigDecimalOrNull()
            ?: return R.string.send_error_invalid_amount
        if (parsed <= BigDecimal.ZERO) return R.string.send_error_invalid_amount
        val token = state.selectedToken
        val balance = token?.balance ?: state.nativeToken?.balance
        if (balance != null && parsed > balance) {
            return R.string.send_error_amount_exceeds_balance
        }
        return null
    }

    private fun gasFeeInNativeUnits(estimate: GasEstimate?, gasIndex: Int): BigDecimal {
        val estimate = estimate ?: return BigDecimal.ZERO
        val gas = listOf(estimate.slow, estimate.normal, estimate.fast).getOrNull(gasIndex)
            ?: return BigDecimal.ZERO
        return BigDecimal(estimate.gasLimit.multiply(gas.gasPrice)).movePointLeft(NATIVE_DECIMALS)
    }

    private fun isValidAddress(address: String): Boolean {
        return address.startsWith("0x") &&
            address.length == 42 &&
            address.substring(2).all { it in "0123456789abcdefABCDEF" }
    }

    private companion object {
        const val PIN_LENGTH = 6
        const val ESTIMATE_DEBOUNCE_MILLIS = 400L
        const val NATIVE_DECIMALS = 18
    }
}

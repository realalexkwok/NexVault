package com.nexvault.wallet.feature.send

import androidx.annotation.StringRes
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.GasOption

/**
 * UI state for the send flow (form -> PIN-confirmed submit -> result).
 *
 * Roadmap 2.6. The token list and selected chain come from the portfolio/chain flows; the gas
 * estimate refreshes on every valid form change.
 */
data class SendUiState(
    val chain: Chain? = null,
    val tokens: List<Token> = emptyList(),
    /** Contract address of the token being sent, or null for the native coin. */
    val selectedTokenAddress: String? = null,
    val toAddress: String = "",
    val amount: String = "",
    @StringRes val addressErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null,
    val gasEstimate: GasEstimate? = null,
    /** Index into slow/normal/fast (0/1/2). */
    val selectedGasIndex: Int = 1,
    val isEstimating: Boolean = false,
    /** Roadmap 2.0.4: key-absent is a persistent, explicit state. */
    val isRpcNotConfigured: Boolean = false,
    val showPinDialog: Boolean = false,
    val pin: String = "",
    @StringRes val pinErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val submittedHash: String? = null,
    val errorMessage: String? = null,
) {
    val selectedToken: Token?
        get() = tokens.firstOrNull { it.contractAddress == selectedTokenAddress }

    val nativeToken: Token?
        get() = tokens.firstOrNull { it.isNative }

    val selectedGas: GasOption?
        get() {
            val estimate = gasEstimate ?: return null
            return listOf(estimate.slow, estimate.normal, estimate.fast).getOrNull(selectedGasIndex)
        }

    val isFormValid: Boolean
        get() = addressErrorRes == null && amountErrorRes == null &&
            toAddress.isNotBlank() && amount.isNotBlank() && selectedGas != null
}

package com.nexvault.wallet.core.network.dto

import com.nexvault.wallet.domain.model.common.NexVaultException

/**
 * Response envelope of the Etherscan API V2 `account/tokentx` action (ERC-20 transfer events).
 *
 * Same three wire shapes as [EtherscanTransactionListResponse], and for the same reason it is
 * bound by `EtherscanEnvelopeAdapterFactory` rather than Moshi codegen.
 *
 * @property status "1" for success, "0" for error
 * @property message Status message from the API
 * @property result Token transfer rows, or null when the wire `result` was not an array
 * @property resultText The explorer's explanation, present only on rejection envelopes
 */
data class EtherscanTokenTransferListResponse(
    val status: String = "0",
    val message: String = "",
    val result: List<TokenTransferDto>? = null,
    val resultText: String? = null,
) {
    val isSuccess: Boolean get() = status == "1"

    /**
     * The explorer's rejection as a domain exception, or null when the envelope carries data or
     * a legitimate empty result (roadmap 2.0.4b, AC-4/AC-8).
     */
    fun errorOrNull(): NexVaultException? = envelopeErrorOrNull(isSuccess, message, result, resultText)
}

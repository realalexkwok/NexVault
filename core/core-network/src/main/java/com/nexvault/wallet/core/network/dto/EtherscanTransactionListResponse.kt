package com.nexvault.wallet.core.network.dto

import com.nexvault.wallet.domain.model.common.NexVaultException

/**
 * Response envelope of the Etherscan API V2 `account/txlist` action.
 *
 * Wire shapes (roadmap 2.0.4b):
 * - `{"status":"1","message":"OK","result":[{…}]}` — data;
 * - `{"status":"0","message":"No transactions found","result":[]}` — legitimate empty result;
 * - `{"status":"0","message":"NOTOK","result":"<text>"}` — rejected call (V1 host, bad key,
 *   rate limit, or the plan gate on a chain the key's plan does not cover).
 *
 * `result` is a JSON array on success and a JSON **String** on rejection, so this class carries
 * both [result] and [resultText] and is bound by `EtherscanEnvelopeAdapterFactory` instead of Moshi
 * codegen: with a non-null `List` field the rejection body did not parse at all and the
 * explorer's explanation was lost.
 *
 * @property status "1" for success, "0" for error
 * @property message Status message from the API
 * @property result Transaction rows, or null when the wire `result` was not an array
 * @property resultText The explorer's explanation, present only on rejection envelopes
 */
data class EtherscanTransactionListResponse(
    val status: String = "0",
    val message: String = "",
    val result: List<TransactionDto>? = null,
    val resultText: String? = null,
) {
    val isSuccess: Boolean get() = status == "1"

    /**
     * The explorer's rejection as a domain exception, or null when the envelope carries data or
     * a legitimate empty result (roadmap 2.0.4b, AC-4/AC-8).
     */
    fun errorOrNull(): NexVaultException? = envelopeErrorOrNull(isSuccess, message, result, resultText)
}

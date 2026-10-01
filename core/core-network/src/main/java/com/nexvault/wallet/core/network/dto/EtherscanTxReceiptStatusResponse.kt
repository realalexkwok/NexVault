package com.nexvault.wallet.core.network.dto

import com.nexvault.wallet.domain.model.common.NexVaultException

/**
 * Response envelope of the Etherscan API V2 `transaction/gettxreceiptstatus` action (roadmap 2.8).
 *
 * Wire shapes:
 * - `{"status":"1","message":"OK","result":{"status":"1"}}` — receipt found; the inner status is
 *   `"1"` when the transaction succeeded and `"0"` when it reverted;
 * - `{"status":"0","message":"NOTOK","result":"<text>"}` — rejected call (bad key, rate limit, plan
 *   gate). As with the list envelopes, `result` is a String on rejection, so this type is bound by
 *   `EtherscanEnvelopeAdapterFactory` rather than Moshi codegen.
 *
 * @property status "1" for a successful call (not the transaction's own status), "0" for error
 * @property message Status message from the API
 * @property result Receipt payload, or null when the wire `result` was not an object
 * @property resultText The explorer's explanation, present only on rejection envelopes
 */
data class EtherscanTxReceiptStatusResponse(
    val status: String = "0",
    val message: String = "",
    val result: ReceiptStatusDto? = null,
    val resultText: String? = null,
) {
    val isSuccess: Boolean get() = status == "1"

    /**
     * The transaction's own receipt status: `true` when it was mined successfully, `false` when it
     * reverted, **null while the transaction has no receipt yet**.
     *
     * The live no-receipt shape is `{"status":"1","message":"OK","result":{"status":""}}` (probed
     * 2026-09-30): the call succeeds but the inner status is empty, so a blank status must stay
     * "unknown" — treating it as `false` would persist a pending transaction as FAILED.
     */
    val receiptSucceeded: Boolean?
        get() = result?.status?.takeIf { it.isNotBlank() }?.let { it == "1" }

    /** The explorer's rejection as a domain exception, or null when the envelope carries data. */
    fun errorOrNull(): NexVaultException? = envelopeErrorOrNull(isSuccess, message, result, resultText)
}

/** Inner payload of a receipt-status envelope. */
data class ReceiptStatusDto(
    @com.squareup.moshi.Json(name = "status") val status: String? = null,
)

package com.nexvault.wallet.core.network.dto

import com.nexvault.wallet.domain.model.common.ExplorerApiException
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.model.common.NexVaultException

/**
 * The explorer's own wording for "this chain is not in your plan" (roadmap 2.0.4b).
 *
 * Observed verbatim on `chainid=56` with the free Etherscan plan, 2026-09-26:
 * *"Free API access is not supported for this chain. Please upgrade your api plan for full
 * chain coverage. https://etherscan.io/apis"*.
 */
internal const val PLAN_GATE_MARKER = "Free API access is not supported"

/**
 * Classifies an Etherscan V2 envelope into the domain state the UI needs (roadmap 2.0.4b).
 *
 * Deliberately **message-driven**: the plan gate is recognised from the explorer's own text, not
 * from a hard-coded chain id, so a newly plan-gated chain behaves the same and a reworded or
 * newly covered chain degrades to a visible generic error — never to a silent empty list.
 *
 * @param isSuccess whether `status == "1"`
 * @param message the envelope's `message` field
 * @param result the parsed array payload, or null when the wire value was not an array
 * @param resultText the String payload of a rejection envelope
 */
internal fun envelopeErrorOrNull(
    isSuccess: Boolean,
    message: String,
    result: Any?,
    resultText: String?,
): NexVaultException? =
    when {
        isSuccess -> null
        // Rejection envelope: `result` is a String carrying the explorer's explanation.
        resultText != null -> explorerError(message, resultText)
        // `result` is an array (possibly empty): a legitimate "No transactions found".
        result != null -> null
        // No usable payload at all: surface the status message instead of an empty success.
        else -> ExplorerApiException(message.ifBlank { "Block explorer request failed" })
    }

/**
 * Maps a rejection envelope's fields onto the two explorer exceptions.
 *
 * @param message the envelope's `message` field (Etherscan uses "NOTOK" for rejections)
 * @param resultText the envelope's String `result`, i.e. the explorer's explanation
 */
internal fun explorerError(
    message: String,
    resultText: String,
): NexVaultException =
    if (message == "NOTOK" && resultText.contains(PLAN_GATE_MARKER)) {
        ExplorerPlanUnsupportedException(resultText)
    } else {
        ExplorerApiException(resultText.ifBlank { message.ifBlank { "Block explorer error" } })
    }

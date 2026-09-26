package com.nexvault.wallet.core.network.dto

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.network.adapter.BigDecimalAdapter
import com.nexvault.wallet.core.network.adapter.BigIntegerAdapter
import com.nexvault.wallet.core.network.adapter.EtherscanEnvelopeAdapterFactory
import com.nexvault.wallet.domain.model.common.ExplorerApiException
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * Roadmap 2.0.4b: the Etherscan V2 list envelopes must parse in every shape the explorer actually
 * sends — success rows, the legitimate empty result, and the rejection envelopes whose `result` is
 * a **String** (deprecated V1 host, invalid key, plan gate). Before this item the last shape did
 * not parse at all and the explorer's explanation was lost.
 *
 * Uses the real Moshi configuration from `NetworkModule.provideMoshi()`.
 */
class EtherscanEnvelopeParsingTest {
    private val moshi: Moshi =
        Moshi
            .Builder()
            .add(BigDecimalAdapter())
            .add(BigIntegerAdapter())
            .add(EtherscanEnvelopeAdapterFactory())
            .build()

    @Test
    fun successEnvelope_parsesTheRows() {
        val envelope =
            transactionEnvelope(
                """{"status":"1","message":"OK","result":[$TRANSACTION_ROW]}""",
            )

        assertThat(envelope.isSuccess).isTrue()
        assertThat(envelope.result).hasSize(1)
        assertThat(envelope.result?.first()?.hash).isEqualTo("0xnative")
        assertThat(envelope.resultText).isNull()
        assertThat(envelope.errorOrNull()).isNull()
    }

    @Test
    fun noTransactionsFound_isAnEmptySuccess_notAnError() {
        val envelope =
            transactionEnvelope(
                """{"status":"0","message":"No transactions found","result":[]}""",
            )

        assertThat(envelope.isSuccess).isFalse()
        assertThat(envelope.result).isEmpty()
        assertThat(envelope.resultText).isNull()
        assertThat(envelope.errorOrNull()).isNull()
    }

    @Test
    fun deprecatedV1Envelope_carriesTheExplorerMessage() {
        val envelope =
            transactionEnvelope(
                """{"status":"0","message":"NOTOK",""" +
                    """"result":"You are using a deprecated V1 endpoint, switch to Etherscan API V2"}""",
            )

        val error = envelope.errorOrNull()
        assertThat(error).isInstanceOf(ExplorerApiException::class.java)
        assertThat(error?.message).contains("deprecated V1 endpoint")
    }

    /** The verbatim body the owner's free key answered on `chainid=56`, 2026-09-26 (AC-8). */
    @Test
    fun planGateEnvelope_mapsToTheDedicatedException() {
        val envelope =
            transactionEnvelope(
                """{"status":"0","message":"NOTOK","result":"Free API access is not supported for this """ +
                    """chain. Please upgrade your api plan for full chain coverage. https://etherscan.io/apis"}""",
            )

        val error = envelope.errorOrNull()
        assertThat(error).isInstanceOf(ExplorerPlanUnsupportedException::class.java)
        assertThat(error?.message).contains(PLAN_GATE_MARKER)
        assertThat(error?.message).contains("Please upgrade your api plan")
    }

    @Test
    fun tokenTransferEnvelope_handlesDataAndRejection() {
        val success =
            tokenTransferEnvelope(
                """{"status":"1","message":"OK","result":[$TOKEN_TRANSFER_ROW]}""",
            )
        assertThat(success.result).hasSize(1)
        assertThat(success.errorOrNull()).isNull()

        val rejected =
            tokenTransferEnvelope(
                """{"status":"0","message":"NOTOK","result":"Missing/Invalid API Key"}""",
            )
        val error = rejected.errorOrNull()
        assertThat(error).isInstanceOf(ExplorerApiException::class.java)
        assertThat(error?.message).isEqualTo("Missing/Invalid API Key")
    }

    @Test
    fun missingResult_fallsBackToTheStatusMessage() {
        val envelope = transactionEnvelope("""{"status":"0","message":"NOTOK","result":null}""")

        assertThat(envelope.result).isNull()
        assertThat(envelope.resultText).isNull()
        val error = envelope.errorOrNull()
        assertThat(error).isInstanceOf(ExplorerApiException::class.java)
        assertThat(error?.message).isEqualTo("NOTOK")
    }

    @Test
    fun malformedRow_stillThrowsInsteadOfBeingSwallowed() {
        val failure =
            runCatching {
                transactionEnvelope("""{"status":"1","message":"OK","result":[{"hash":"0xnative"}]}""")
            }.exceptionOrNull()

        assertThat(failure).isInstanceOf(JsonDataException::class.java)
    }

    private fun transactionEnvelope(json: String): EtherscanTransactionListResponse =
        checkNotNull(
            moshi.adapter(EtherscanTransactionListResponse::class.java).fromJson(json),
        )

    private fun tokenTransferEnvelope(json: String): EtherscanTokenTransferListResponse =
        checkNotNull(
            moshi.adapter(EtherscanTokenTransferListResponse::class.java).fromJson(json),
        )

    private companion object {
        const val TRANSACTION_ROW =
            """{"hash":"0xnative","from":"0xfrom","to":"0xto","value":"1","gas":"21000",""" +
                """"gasPrice":"5","gasUsed":"21000","blockNumber":"1","timeStamp":"1700000000",""" +
                """"nonce":"0","isError":"0","txreceipt_status":"1","input":"0x",""" +
                """"contractAddress":"","confirmations":"10"}"""

        const val TOKEN_TRANSFER_ROW =
            """{"hash":"0xtoken","from":"0xfrom","to":"0xto","value":"100","tokenName":"USD Coin",""" +
                """"tokenSymbol":"USDC","tokenDecimal":"6","contractAddress":"0xusdc","gas":"21000",""" +
                """"gasPrice":"5","gasUsed":"21000","blockNumber":"1","timeStamp":"1700000000",""" +
                """"nonce":"0","confirmations":"10"}"""
    }
}

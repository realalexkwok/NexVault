package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.network.dto.TokenTransferDto
import com.nexvault.wallet.core.network.dto.TransactionDto

/**
 * Minimal explorer rows for the transaction repository tests (roadmap 2.0.4b).
 *
 * Kept out of the test class so its function count stays under the detekt threshold.
 */
internal object TransactionTestFixtures {
    fun transactionDto(hash: String) =
        TransactionDto(
            hash = hash,
            from = "0xfrom",
            to = "0xto",
            value = "1",
            gas = "21000",
            gasPrice = "5",
            gasUsed = "21000",
            blockNumber = "1",
            timestamp = "1700000000",
            nonce = "0",
            isError = "0",
            txReceiptStatus = "1",
            input = "0x",
            contractAddress = null,
            confirmations = "10",
        )

    fun tokenTransferDto(hash: String) =
        TokenTransferDto(
            hash = hash,
            from = "0xfrom",
            to = "0xto",
            value = "100",
            tokenName = "USD Coin",
            tokenSymbol = "USDC",
            tokenDecimal = "6",
            contractAddress = "0xusdc",
            gas = "21000",
            gasPrice = "5",
            gasUsed = "21000",
            blockNumber = "1",
            timestamp = "1700000000",
            nonce = "0",
            confirmations = "10",
        )
}

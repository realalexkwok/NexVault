package com.nexvault.wallet.domain.model.transaction

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Domain model for a blockchain transaction.
 */
data class Transaction(
    val txHash: String,
    val chainId: Int,
    val fromAddress: String,
    val toAddress: String,
    val value: BigDecimal,
    val gasUsed: BigInteger?,
    val gasPrice: BigInteger?,
    val tokenSymbol: String?,
    val tokenContractAddress: String?,
    val tokenDecimals: Int?,
    val blockNumber: Long,
    val timestamp: Long,
    val status: TransactionStatus,
    val type: TransactionType,
)

enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED,
}

enum class TransactionType {
    SEND,
    RECEIVE,
    SWAP,
    CONTRACT_INTERACTION,
    APPROVAL,
}

/**
 * Gas estimation options for sending transactions.
 */
data class GasEstimate(
    val slow: GasOption,
    val normal: GasOption,
    val fast: GasOption,
    /** The estimated gas limit shared by all three tiers (roadmap 2.6). */
    val gasLimit: BigInteger,
)

data class GasOption(
    val gasPrice: BigInteger,
    val maxFeePerGas: BigInteger?,
    val maxPriorityFeePerGas: BigInteger?,
    val estimatedTimeSeconds: Int,
    val estimatedTimeName: String,
    val fiatCost: Double?,
)

/**
 * Parameters for sending a transaction.
 */
data class SendTransactionParams(
    val toAddress: String,
    val amount: BigDecimal,
    val tokenAddress: String?,
    val gasOption: GasOption,
    val chainId: Int,
    val data: String?,
)

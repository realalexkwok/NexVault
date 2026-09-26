package com.nexvault.wallet.data.mapper

import com.nexvault.wallet.core.database.entity.TransactionEntity
import com.nexvault.wallet.core.network.dto.TokenTransferDto
import com.nexvault.wallet.core.network.dto.TransactionDto
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Maps Etherscan DTOs to [TransactionEntity] rows for Room.
 *
 * @param walletAddress The wallet address used to label each row as send vs receive.
 */
fun TransactionDto.toEntity(chainId: Int, walletAddress: String): TransactionEntity {
    val walletLower = walletAddress.lowercase()
    val fromLower = from.lowercase()
    val typeStr = if (fromLower == walletLower) "send" else "receive"
    val statusVal = when {
        isError == "1" -> 2
        txReceiptStatus == "0" -> 2
        txReceiptStatus == "1" -> 1
        else -> 1
    }
    return TransactionEntity(
        txHash = hash,
        chainId = chainId,
        fromAddress = fromLower,
        toAddress = to.lowercase(),
        value = value,
        gasUsed = gasUsed,
        gasPrice = gasPrice,
        tokenSymbol = null,
        tokenContractAddress = null,
        tokenDecimals = null,
        blockNumber = blockNumber.toLongOrNull() ?: 0L,
        timestamp = timestamp.toLongOrNull() ?: 0L,
        status = statusVal,
        type = typeStr,
    )
}

/**
 * Maps ERC-20 transfer DTOs to [TransactionEntity] rows for Room.
 *
 * @param walletAddress The wallet address used to label each row as send vs receive.
 */
fun TokenTransferDto.toEntity(chainId: Int, walletAddress: String): TransactionEntity {
    val walletLower = walletAddress.lowercase()
    val fromLower = from.lowercase()
    val typeStr = if (fromLower == walletLower) "send" else "receive"
    return TransactionEntity(
        txHash = hash,
        chainId = chainId,
        fromAddress = fromLower,
        toAddress = to.lowercase(),
        value = value,
        gasUsed = gasUsed,
        gasPrice = gasPrice,
        tokenSymbol = tokenSymbol,
        tokenContractAddress = contractAddress.lowercase(),
        tokenDecimals = tokenDecimal.toIntOrNull(),
        blockNumber = blockNumber.toLongOrNull() ?: 0L,
        timestamp = timestamp.toLongOrNull() ?: 0L,
        status = 1,
        type = typeStr,
    )
}

/**
 * Maps a stored [TransactionEntity] to the domain [Transaction].
 *
 * @param walletAddress Active account address; used to refine [TransactionType] when
 *   [TransactionEntity.type] is unknown.
 */
fun TransactionEntity.toDomain(walletAddress: String): Transaction {
    val typeEnum = when (type.lowercase()) {
        "send" -> TransactionType.SEND
        "receive" -> TransactionType.RECEIVE
        "swap" -> TransactionType.SWAP
        "approve" -> TransactionType.APPROVAL
        "contract" -> TransactionType.CONTRACT_INTERACTION
        else -> if (fromAddress.equals(walletAddress, ignoreCase = true)) {
            TransactionType.SEND
        } else {
            TransactionType.RECEIVE
        }
    }
    val statusEnum = when (status) {
        0 -> TransactionStatus.PENDING
        1 -> TransactionStatus.CONFIRMED
        else -> TransactionStatus.FAILED
    }
    val humanValue = humanReadableAmount(
        raw = value,
        tokenContractAddress = tokenContractAddress,
        tokenDecimals = tokenDecimals,
    )
    return Transaction(
        txHash = txHash,
        chainId = chainId,
        fromAddress = fromAddress,
        toAddress = toAddress,
        value = humanValue,
        gasUsed = gasUsed?.let { runCatching { BigInteger(it) }.getOrNull() },
        gasPrice = gasPrice?.let { runCatching { BigInteger(it) }.getOrNull() },
        tokenSymbol = tokenSymbol,
        tokenContractAddress = tokenContractAddress,
        tokenDecimals = tokenDecimals,
        blockNumber = blockNumber,
        timestamp = timestamp,
        status = statusEnum,
        type = typeEnum,
    )
}

private fun humanReadableAmount(
    raw: String,
    tokenContractAddress: String?,
    tokenDecimals: Int?,
): BigDecimal {
    val base = runCatching { BigDecimal(raw.ifBlank { "0" }) }.getOrNull() ?: BigDecimal.ZERO
    return if (tokenContractAddress.isNullOrBlank()) {
        base.divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN)
    } else {
        val dec = tokenDecimals ?: 18
        base.divide(BigDecimal.TEN.pow(dec), dec, RoundingMode.DOWN)
    }
}

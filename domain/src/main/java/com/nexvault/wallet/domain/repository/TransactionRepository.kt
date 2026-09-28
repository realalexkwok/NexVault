package com.nexvault.wallet.domain.repository

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.model.transaction.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository for transaction operations (send, history, status).
 */
interface TransactionRepository {
    /**
     * Estimate the gas options for a prospective send (roadmap 2.6).
     *
     * The repository resolves the token's decimals and encodes the transfer calldata itself, so
     * callers never touch ABI encoding.
     *
     * @param fromAddress sender address
     * @param toAddress recipient address
     * @param amount transfer amount in display units (native coin or token)
     * @param tokenAddress contract address of the token, or null for a native-coin send
     */
    suspend fun estimateSendGas(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        tokenAddress: String?,
        chainId: Int,
    ): DataResult<GasEstimate>

    /**
     * Send a native coin transaction (ETH, BNB, MATIC).
     * Returns the transaction hash.
     */
    suspend fun sendNativeTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String>

    /**
     * Send an ERC-20 token transfer.
     * Returns the transaction hash.
     */
    suspend fun sendTokenTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String>

    /**
     * Get transaction history for an address on a chain.
     * Paginated — returns a page of transactions.
     */
    fun getTransactionHistory(
        chainId: Int,
        address: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<Transaction>>

    /**
     * Force refresh transaction history from the block explorer API.
     */
    suspend fun refreshTransactionHistory(
        chainId: Int,
        address: String,
    ): DataResult<Unit>

    /**
     * Returns recent transactions involving [address] for a specific token.
     *
     * @param chainId The chain ID
     * @param address The wallet address (lowercase matching is applied in the DAO)
     * @param tokenContractAddress ERC-20 contract address, or null for native coin transfers only
     * @param limit Max rows to return (newest first)
     */
    suspend fun getRecentTransactionsForToken(
        chainId: Int,
        address: String,
        tokenContractAddress: String?,
        limit: Int = 5,
    ): List<Transaction>

    /**
     * Get a single transaction's full details.
     */
    suspend fun getTransactionDetail(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction>

    /**
     * Get pending transactions that need status polling.
     */
    fun getPendingTransactions(chainId: Int, address: String): Flow<List<Transaction>>

    /**
     * Check and update the status of a pending transaction.
     * Returns updated transaction.
     */
    suspend fun updateTransactionStatus(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction>
}

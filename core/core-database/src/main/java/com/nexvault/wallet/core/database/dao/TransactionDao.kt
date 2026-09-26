/*
 * Copyright (c) 2025 NexVault
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexvault.wallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nexvault.wallet.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction history operations.
 *
 * Supports paginated queries for efficient loading of large transaction histories.
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.2
 * Ref: doc/07-TEST-CASES.md TC-DB-003
 */
@Dao
interface TransactionDao {
    /**
     * Gets paginated transactions for a wallet address on a specific chain.
     *
     * Returns transactions where the address is either the sender or receiver,
     * ordered by timestamp descending (newest first).
     *
     * Ref: doc/07-TEST-CASES.md TC-DB-003 - paginated query returns correct page
     *
     * @param chainId The blockchain chain ID
     * @param address The wallet address (matches both from and to)
     * @param limit Number of results per page
     * @param offset Starting offset for pagination
     * @return List of transactions for the requested page
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE chainId = :chainId
          AND (LOWER(fromAddress) = LOWER(:address) OR LOWER(toAddress) = LOWER(:address))
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getTransactions(
        chainId: Int,
        address: String,
        limit: Int,
        offset: Int,
    ): List<TransactionEntity>

    /**
     * Observes all transactions for a wallet address on a specific chain.
     * Used for the transaction history screen's reactive updates.
     *
     * @param chainId The blockchain chain ID
     * @param address The wallet address
     * @return Flow emitting the transaction list whenever data changes
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE chainId = :chainId
          AND (LOWER(fromAddress) = LOWER(:address) OR LOWER(toAddress) = LOWER(:address))
        ORDER BY timestamp DESC
        """,
    )
    fun observeTransactions(chainId: Int, address: String): Flow<List<TransactionEntity>>

    /**
     * Gets transactions filtered by type for a wallet address.
     *
     * @param chainId The blockchain chain ID
     * @param address The wallet address
     * @param type Transaction type filter ("send", "receive", "swap", "contract")
     * @param limit Number of results per page
     * @param offset Starting offset for pagination
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE chainId = :chainId
          AND (LOWER(fromAddress) = LOWER(:address) OR LOWER(toAddress) = LOWER(:address))
          AND type = :type
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getTransactionsByType(
        chainId: Int,
        address: String,
        type: String,
        limit: Int,
        offset: Int,
    ): List<TransactionEntity>

    /**
     * Gets transactions filtered by status.
     * Useful for finding pending transactions that need status polling.
     *
     * @param chainId The blockchain chain ID
     * @param status Transaction status (0 = pending, 1 = confirmed, 2 = failed)
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE chainId = :chainId AND status = :status
        ORDER BY timestamp DESC
        """,
    )
    suspend fun getTransactionsByStatus(chainId: Int, status: Int): List<TransactionEntity>

    /**
     * Gets all pending transactions across all chains.
     * Used by the background PendingTxWorker to check for confirmations.
     */
    @Query("SELECT * FROM transactions WHERE status = 0")
    suspend fun getAllPendingTransactions(): List<TransactionEntity>

    /**
     * Gets a single transaction by its hash and chain.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE txHash = :txHash AND chainId = :chainId
        """,
    )
    suspend fun getTransaction(txHash: String, chainId: Int): TransactionEntity?

    /**
     * Observes a single transaction for status updates (pending -> confirmed).
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE txHash = :txHash AND chainId = :chainId
        """,
    )
    fun observeTransaction(txHash: String, chainId: Int): Flow<TransactionEntity?>

    /**
     * Gets recent transactions for a specific token.
     * Used on the Token Detail screen to show token-specific history.
     *
     * @param chainId The blockchain chain ID
     * @param address The wallet address
     * @param tokenContractAddress The ERC-20 contract address (null for native coin)
     * @param limit Maximum number of transactions to return
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE chainId = :chainId
          AND (LOWER(fromAddress) = LOWER(:address) OR LOWER(toAddress) = LOWER(:address))
          AND (tokenContractAddress = :tokenContractAddress
               OR (:tokenContractAddress IS NULL AND tokenContractAddress IS NULL))
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentTransactionsForToken(
        chainId: Int,
        address: String,
        tokenContractAddress: String?,
        limit: Int,
    ): List<TransactionEntity>

    /**
     * Inserts or updates transactions.
     */
    @Upsert
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    /**
     * Updates the status of a specific transaction.
     * Used when a pending transaction is confirmed or fails.
     *
     * @param txHash Transaction hash
     * @param chainId The blockchain chain ID
     * @param status New status (0 = pending, 1 = confirmed, 2 = failed)
     * @param gasUsed Actual gas used (available after confirmation)
     */
    @Query(
        """
        UPDATE transactions
        SET status = :status, gasUsed = :gasUsed
        WHERE txHash = :txHash AND chainId = :chainId
        """,
    )
    suspend fun updateTransactionStatus(
        txHash: String,
        chainId: Int,
        status: Int,
        gasUsed: String?,
    )

    /**
     * Gets the count of transactions for a wallet on a chain.
     * Useful for determining if pagination is needed.
     */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE chainId = :chainId
          AND (LOWER(fromAddress) = LOWER(:address) OR LOWER(toAddress) = LOWER(:address))
        """,
    )
    suspend fun getTransactionCount(chainId: Int, address: String): Int

    /**
     * Deletes all transactions for a specific chain.
     */
    @Query("DELETE FROM transactions WHERE chainId = :chainId")
    suspend fun deleteAllTransactionsForChain(chainId: Int)
}

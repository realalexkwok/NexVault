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
import androidx.room.Transaction
import androidx.room.Upsert
import com.nexvault.wallet.core.database.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for token operations.
 *
 * All queries that return Flow are observable — they automatically emit
 * new values when the underlying data changes.
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.2
 * Ref: doc/07-TEST-CASES.md TC-DB-001, TC-DB-002
 */
@Dao
interface TokenDao {
    /**
     * Observes all tokens for a specific chain, ordered by fiat value descending.
     * Tokens with null fiat value are sorted to the bottom.
     *
     * @param chainId The blockchain chain ID
     * @return Flow emitting the current token list whenever data changes
     */
    @Query(
        """
        SELECT * FROM tokens
        WHERE chainId = :chainId
        ORDER BY COALESCE(fiatValue, 0) DESC, sortOrder ASC
        """,
    )
    fun getTokensByChain(chainId: Int): Flow<List<TokenEntity>>

    /**
     * One-shot fetch of all tokens for a chain (same ordering as [getTokensByChain] without Flow).
     */
    @Query(
        """
        SELECT * FROM tokens
        WHERE chainId = :chainId
        ORDER BY COALESCE(fiatValue, 0) DESC, sortOrder ASC
        """,
    )
    suspend fun getTokensByChainOnce(chainId: Int): List<TokenEntity>

    /**
     * Number of token rows stored for a chain (used to detect whether default list was seeded).
     */
    @Query("SELECT COUNT(*) FROM tokens WHERE chainId = :chainId")
    suspend fun getTokenCountByChain(chainId: Int): Int

    /**
     * Gets a single token by its contract address and chain.
     *
     * @param contractAddress Token contract address or "native"
     * @param chainId The blockchain chain ID
     * @return The token entity, or null if not found
     */
    @Query(
        """
        SELECT * FROM tokens
        WHERE contractAddress = :contractAddress AND chainId = :chainId
        """,
    )
    suspend fun getToken(contractAddress: String, chainId: Int): TokenEntity?

    /**
     * Observes a single token by its contract address and chain.
     */
    @Query(
        """
        SELECT * FROM tokens
        WHERE contractAddress = :contractAddress AND chainId = :chainId
        """,
    )
    fun observeToken(contractAddress: String, chainId: Int): Flow<TokenEntity?>

    /**
     * Observes the total fiat value of all tokens on a specific chain.
     *
     * @param chainId The blockchain chain ID
     * @return Flow emitting the summed fiat value, or null if no tokens
     */
    @Query("SELECT SUM(fiatValue) FROM tokens WHERE chainId = :chainId")
    fun getTotalFiatValue(chainId: Int): Flow<Double?>

    /**
     * Inserts or updates a list of tokens.
     *
     * Uses Room's @Upsert which inserts new rows and updates existing ones
     * based on the primary key (contractAddress + chainId).
     *
     * Ref: doc/07-TEST-CASES.md TC-DB-001 - upsert should update balance
     */
    @Upsert
    suspend fun upsertTokens(tokens: List<TokenEntity>)

    /**
     * Inserts or updates a single token.
     */
    @Upsert
    suspend fun upsertToken(token: TokenEntity)

    /**
     * Deletes a specific token.
     *
     * @param contractAddress Token contract address
     * @param chainId The blockchain chain ID
     */
    @Query(
        """
        DELETE FROM tokens
        WHERE contractAddress = :contractAddress AND chainId = :chainId
        """,
    )
    suspend fun deleteToken(contractAddress: String, chainId: Int)

    /**
     * Deletes all tokens for a specific chain.
     * Used when resetting or switching wallet.
     */
    @Query("DELETE FROM tokens WHERE chainId = :chainId")
    suspend fun deleteAllTokensForChain(chainId: Int)

    /**
     * Gets all tokens across all chains.
     * Useful for background sync worker.
     */
    @Query("SELECT * FROM tokens")
    suspend fun getAllTokens(): List<TokenEntity>

    /**
     * Gets all unique chain IDs that have tokens.
     * Useful for background sync to know which chains to refresh.
     */
    @Query("SELECT DISTINCT chainId FROM tokens")
    suspend fun getActiveChainIds(): List<Int>

    /**
     * Batch upsert wrapped in a Room transaction for performance.
     *
     * Ref: doc/09-PERFORMANCE-OPTIMIZATION.md Section 4.3 - Transaction Batching
     */
    @Transaction
    suspend fun refreshTokens(chainId: Int, tokens: List<TokenEntity>) {
        upsertTokens(tokens)
    }
}

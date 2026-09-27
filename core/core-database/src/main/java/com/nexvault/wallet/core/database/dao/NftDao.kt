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
import com.nexvault.wallet.core.database.entity.NftEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for NFT operations.
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.2
 */
@Dao
interface NftDao {
    /**
     * Observes all NFTs for a wallet on a specific chain.
     *
     * @param chainId The blockchain chain ID
     * @return Flow emitting the NFT list whenever data changes
     */
    @Query(
        """
        SELECT * FROM nfts
        WHERE chainId = :chainId
        ORDER BY collectionName ASC, name ASC
        """,
    )
    fun getNftsByChain(chainId: Int): Flow<List<NftEntity>>

    /**
     * Gets a single NFT by its contract address, token ID, and chain.
     */
    @Query(
        """
        SELECT * FROM nfts
        WHERE contractAddress = :contractAddress
          AND tokenId = :tokenId
          AND chainId = :chainId
        """,
    )
    suspend fun getNft(contractAddress: String, tokenId: String, chainId: Int): NftEntity?

    /**
     * Observes a single NFT for detail screen.
     */
    @Query(
        """
        SELECT * FROM nfts
        WHERE contractAddress = :contractAddress
          AND tokenId = :tokenId
          AND chainId = :chainId
        """,
    )
    fun observeNft(
        contractAddress: String,
        tokenId: String,
        chainId: Int,
    ): Flow<NftEntity?>

    /**
     * Inserts or updates NFTs.
     */
    @Upsert
    suspend fun upsertNfts(nfts: List<NftEntity>)

    /**
     * Inserts or updates a single NFT.
     */
    @Upsert
    suspend fun upsertNft(nft: NftEntity)

    /**
     * Deletes all NFTs for a specific chain.
     * Used when refreshing the full NFT list from the API.
     */
    @Query("DELETE FROM nfts WHERE chainId = :chainId")
    suspend fun deleteAllNftsForChain(chainId: Int)

    /**
     * Gets the count of NFTs for a chain.
     */
    @Query("SELECT COUNT(*) FROM nfts WHERE chainId = :chainId")
    suspend fun getNftCount(chainId: Int): Int
}

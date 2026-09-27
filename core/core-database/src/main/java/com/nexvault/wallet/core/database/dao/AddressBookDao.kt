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
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.nexvault.wallet.core.database.entity.AddressBookEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for address book operations.
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.2
 */
@Dao
interface AddressBookDao {
    /**
     * Observes all address book entries, ordered by creation date descending.
     *
     * @return Flow emitting the address list whenever data changes
     */
    @Query("SELECT * FROM address_book ORDER BY createdAt DESC")
    fun getAllAddresses(): Flow<List<AddressBookEntity>>

    /**
     * Gets address book entries filtered by chain.
     * Entries with null chainId (applicable to all chains) are always included.
     *
     * @param chainId The blockchain chain ID to filter by
     * @return Flow emitting filtered address list
     */
    @Query(
        """
        SELECT * FROM address_book
        WHERE chainId = :chainId OR chainId IS NULL
        ORDER BY createdAt DESC
        """,
    )
    fun getAddressesByChain(chainId: Int): Flow<List<AddressBookEntity>>

    /**
     * Gets a single address book entry by ID.
     */
    @Query("SELECT * FROM address_book WHERE id = :id")
    suspend fun getAddressById(id: Int): AddressBookEntity?

    /**
     * Searches address book entries by name or address.
     *
     * @param query Search query string
     * @return Matching entries
     */
    @Query(
        """
        SELECT * FROM address_book
        WHERE name LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY name ASC
        """,
    )
    fun searchAddresses(query: String): Flow<List<AddressBookEntity>>

    /**
     * Inserts or updates an address book entry.
     */
    @Upsert
    suspend fun upsertAddress(address: AddressBookEntity)

    /**
     * Deletes an address book entry.
     */
    @Delete
    suspend fun deleteAddress(address: AddressBookEntity)

    /**
     * Deletes an address book entry by ID.
     */
    @Query("DELETE FROM address_book WHERE id = :id")
    suspend fun deleteAddressById(id: Int)

    /**
     * Checks if an address already exists in the address book.
     * Prevents duplicate entries for the same address.
     *
     * @param address The Ethereum address to check
     * @return true if the address already exists
     */
    @Query(
        """
        SELECT COUNT(*) > 0 FROM address_book
        WHERE LOWER(address) = LOWER(:address)
        """,
    )
    suspend fun addressExists(address: String): Boolean
}

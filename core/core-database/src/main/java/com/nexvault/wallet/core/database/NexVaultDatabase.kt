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

package com.nexvault.wallet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexvault.wallet.core.database.dao.AddressBookDao
import com.nexvault.wallet.core.database.dao.NftDao
import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.database.entity.AddressBookEntity
import com.nexvault.wallet.core.database.entity.NftEntity
import com.nexvault.wallet.core.database.entity.TokenEntity
import com.nexvault.wallet.core.database.entity.TransactionEntity

/**
 * Room database for NexVault.
 *
 * Contains all tables for tokens, transactions, NFTs, and address book.
 *
 * Version history:
 * - v1: Initial schema with tokens, transactions, nfts, address_book tables
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.3
 */
@Database(
    entities = [
        TokenEntity::class,
        TransactionEntity::class,
        NftEntity::class,
        AddressBookEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NexVaultDatabase : RoomDatabase() {
    /** DAO for token balance and price operations. */
    abstract fun tokenDao(): TokenDao

    /** DAO for transaction history operations. */
    abstract fun transactionDao(): TransactionDao

    /** DAO for NFT gallery operations. */
    abstract fun nftDao(): NftDao

    /** DAO for address book operations. */
    abstract fun addressBookDao(): AddressBookDao

    companion object {
        /** Database file name. */
        const val DATABASE_NAME = "nexvault_database"
    }
}

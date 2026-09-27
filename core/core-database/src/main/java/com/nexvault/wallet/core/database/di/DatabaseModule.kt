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

package com.nexvault.wallet.core.database.di

import android.content.Context
import androidx.room.Room
import com.nexvault.wallet.core.database.NexVaultDatabase
import com.nexvault.wallet.core.database.dao.AddressBookDao
import com.nexvault.wallet.core.database.dao.NftDao
import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAOs.
 *
 * The database is a singleton — only one instance exists for the entire app lifecycle.
 * Each DAO is provided as a separate dependency so that repositories only depend
 * on the DAOs they need, not the entire database.
 *
 * Ref: doc/02-ARCHITECTURE-AND-TECH-STACK.md Section 3 - DI Graph
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.4
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Provides the Room database singleton.
     *
     * Uses fallbackToDestructiveMigration for now since we're in development.
     * In production, proper Migration objects should be written for each
     * schema change to preserve user data.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NexVaultDatabase {
        return Room.databaseBuilder(
            context,
            NexVaultDatabase::class.java,
            NexVaultDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * Provides the TokenDao from the database.
     */
    @Provides
    @Singleton
    fun provideTokenDao(database: NexVaultDatabase): TokenDao {
        return database.tokenDao()
    }

    /**
     * Provides the TransactionDao from the database.
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: NexVaultDatabase): TransactionDao {
        return database.transactionDao()
    }

    /**
     * Provides the NftDao from the database.
     */
    @Provides
    @Singleton
    fun provideNftDao(database: NexVaultDatabase): NftDao {
        return database.nftDao()
    }

    /**
     * Provides the AddressBookDao from the database.
     */
    @Provides
    @Singleton
    fun provideAddressBookDao(database: NexVaultDatabase): AddressBookDao {
        return database.addressBookDao()
    }
}

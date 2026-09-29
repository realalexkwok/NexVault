package com.nexvault.wallet.core.database.di

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.NexVaultDatabase
import com.nexvault.wallet.core.database.dao.AddressBookDao
import com.nexvault.wallet.core.database.dao.NftDao
import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * Roadmap 2.6 coverage: the Hilt provider functions delegate to the Room database instance.
 * Exercised on the JVM with a mocked database — no Robolectric needed.
 */
class DatabaseModuleTest {
    private val database = mockk<NexVaultDatabase>(relaxed = true)

    @Test
    fun provideTokenDao_delegatesToTheDatabase() {
        val expected = mockk<TokenDao>()
        every { database.tokenDao() } returns expected

        assertThat(DatabaseModule.provideTokenDao(database)).isSameInstanceAs(expected)
    }

    @Test
    fun provideTransactionDao_delegatesToTheDatabase() {
        val expected = mockk<TransactionDao>()
        every { database.transactionDao() } returns expected

        assertThat(DatabaseModule.provideTransactionDao(database)).isSameInstanceAs(expected)
    }

    @Test
    fun provideNftDao_delegatesToTheDatabase() {
        val expected = mockk<NftDao>()
        every { database.nftDao() } returns expected

        assertThat(DatabaseModule.provideNftDao(database)).isSameInstanceAs(expected)
    }

    @Test
    fun provideAddressBookDao_delegatesToTheDatabase() {
        val expected = mockk<AddressBookDao>()
        every { database.addressBookDao() } returns expected

        assertThat(DatabaseModule.provideAddressBookDao(database)).isSameInstanceAs(expected)
    }
}

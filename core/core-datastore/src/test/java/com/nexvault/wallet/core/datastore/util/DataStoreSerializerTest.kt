package com.nexvault.wallet.core.datastore.util

import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.datastore.model.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreSerializerTest {
    @Test
    fun serializeDeserialize_walletsRoundTrip() {
        val wallets = listOf(
            WalletMetadata(
                id = "wallet-1",
                name = "Main Wallet",
                createdAt = 1700000000000L,
                type = WalletType.HD,
                accountCount = 3,
                isActive = true,
            ),
            WalletMetadata(
                id = "wallet-2",
                name = "Secondary Wallet",
                createdAt = 1700000001000L,
                type = WalletType.IMPORTED,
                accountCount = 1,
                isActive = false,
            ),
        )

        val serialized = DataStoreSerializer.serializeWallets(wallets)
        val deserialized = DataStoreSerializer.deserializeWallets(serialized)

        assertEquals(wallets.size, deserialized.size)
        assertEquals(wallets[0].id, deserialized[0].id)
        assertEquals(wallets[0].name, deserialized[0].name)
        assertEquals(wallets[0].type, deserialized[0].type)
        assertEquals(wallets[1].id, deserialized[1].id)
        assertEquals(wallets[1].name, deserialized[1].name)
    }

    @Test
    fun serializeDeserialize_emptyList() {
        val wallets = emptyList<WalletMetadata>()

        val serialized = DataStoreSerializer.serializeWallets(wallets)
        val deserialized = DataStoreSerializer.deserializeWallets(serialized)

        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun serializeDeserialize_singleWalletRoundTrip() {
        val wallet = WalletMetadata(
            id = "single-wallet",
            name = "Single Wallet",
            createdAt = 1700000000000L,
            type = WalletType.HD,
            accountCount = 2,
            isActive = true,
        )

        val serialized = DataStoreSerializer.serializeWallets(listOf(wallet))
        val deserialized = DataStoreSerializer.deserializeWallets(serialized)

        assertEquals(1, deserialized.size)
        assertEquals(wallet.id, deserialized[0].id)
        assertEquals(wallet.name, deserialized[0].name)
        assertEquals(wallet.createdAt, deserialized[0].createdAt)
        assertEquals(wallet.type, deserialized[0].type)
        assertEquals(wallet.accountCount, deserialized[0].accountCount)
        assertEquals(wallet.isActive, deserialized[0].isActive)
    }

    @Test
    fun serializeDeserialize_multipleWalletsRoundTrip() {
        val wallets = (0 until 10).map { index ->
            WalletMetadata(
                id = "wallet-$index",
                name = "Wallet $index",
                createdAt = 1700000000000L + index,
                type = if (index % 2 == 0) WalletType.HD else WalletType.IMPORTED,
                accountCount = index + 1,
                isActive = index == 0,
            )
        }

        val serialized = DataStoreSerializer.serializeWallets(wallets)
        val deserialized = DataStoreSerializer.deserializeWallets(serialized)

        assertEquals(wallets.size, deserialized.size)
        wallets.forEachIndexed { index, wallet ->
            assertEquals(wallet.id, deserialized[index].id)
            assertEquals(wallet.name, deserialized[index].name)
            assertEquals(wallet.type, deserialized[index].type)
        }
    }

    @Test
    fun serializeDeserialize_accountsRoundTrip() {
        val accounts = listOf(
            AccountMetadata(
                walletId = "wallet-1",
                accountIndex = 0,
                address = "0x742d35Cc6634C0532925a3b844Bc9e7595f",
                name = "Account 1",
                derivationPath = "m/44'/60'/0'/0/0",
                isActive = true,
                addedAt = 1700000000000L,
            ),
            AccountMetadata(
                walletId = "wallet-1",
                accountIndex = 1,
                address = "0x123d35Cc6634C0532925a3b844Bc9e7596f",
                name = "Account 2",
                derivationPath = "m/44'/60'/0'/0/1",
                isActive = false,
                addedAt = 1700000001000L,
            ),
        )

        val serialized = DataStoreSerializer.serializeAccounts(accounts)
        val deserialized = DataStoreSerializer.deserializeAccounts(serialized)

        assertEquals(accounts.size, deserialized.size)
        assertEquals(accounts[0].address, deserialized[0].address)
        assertEquals(accounts[1].derivationPath, deserialized[1].derivationPath)
    }

    @Test
    fun deserialize_invalidJson_returnsEmptyList() {
        val invalidJson = "{ invalid json content }"
        val deserialized = DataStoreSerializer.deserializeWallets(invalidJson)

        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun deserialize_emptyString_returnsEmptyList() {
        val deserialized = DataStoreSerializer.deserializeWallets("")

        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun deserialize_blankString_returnsEmptyList() {
        val deserialized = DataStoreSerializer.deserializeWallets("   ")

        assertTrue(deserialized.isEmpty())
    }
}
